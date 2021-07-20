package awais.instagrabber.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import awais.instagrabber.db.datasources.RecentSearchDataSource
import awais.instagrabber.db.entities.Favorite
import awais.instagrabber.db.entities.RecentSearch
import awais.instagrabber.db.entities.RecentSearch.Companion.fromSearchItem
import awais.instagrabber.db.repositories.FavoriteRepository
import awais.instagrabber.db.repositories.RecentSearchRepository
import awais.instagrabber.models.Resource
import awais.instagrabber.models.Resource.Companion.error
import awais.instagrabber.models.Resource.Companion.loading
import awais.instagrabber.models.Resource.Companion.success
import awais.instagrabber.models.enums.FavoriteType
import awais.instagrabber.repositories.responses.search.SearchItem
import awais.instagrabber.repositories.responses.search.SearchResponse
import awais.instagrabber.utils.*
import awais.instagrabber.utils.AppExecutors.mainThread
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.webservices.SearchRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.function.BiConsumer
import java.util.stream.Collectors

class SearchFragmentViewModel(application: Application) : AppStateViewModel(application) {
    private val query = MutableLiveData<String>()
    private val topResults = MutableLiveData<Resource<List<SearchItem>?>>()
    private val userResults = MutableLiveData<Resource<List<SearchItem>?>>()
    private val hashtagResults = MutableLiveData<Resource<List<SearchItem>?>>()
    private val locationResults = MutableLiveData<Resource<List<SearchItem>?>>()
    private val searchRepository: SearchRepository by lazy { SearchRepository.getInstance() }
    private val searchCallback: Debouncer.Callback<String> = object : Debouncer.Callback<String> {
        override fun call(key: String) {
            if (tempQuery == null) return
            query.postValue(tempQuery!!)
        }

        override fun onError(t: Throwable) {
            Log.e(TAG, "onError: ", t)
        }
    }
    private val searchDebouncer = Debouncer(searchCallback, 500)
    private val cookie = Utils.settingsHelper.getString(Constants.COOKIE)
    private val isLoggedIn = !isEmpty(cookie) && getUserIdFromCookie(cookie) != 0L
    private val distinctQuery = Transformations.distinctUntilChanged(query)
    private val recentSearchRepository: RecentSearchRepository by lazy {
        RecentSearchRepository.getInstance(RecentSearchDataSource.getInstance(application))
    }
    private val favoriteRepository: FavoriteRepository by lazy { FavoriteRepository.getInstance(application) }
    private var tempQuery: String? = null
    fun getQuery(): LiveData<String> {
        return distinctQuery
    }

    fun getTopResults(): LiveData<Resource<List<SearchItem>?>> {
        return topResults
    }

    fun getUserResults(): LiveData<Resource<List<SearchItem>?>> {
        return userResults
    }

    fun getHashtagResults(): LiveData<Resource<List<SearchItem>?>> {
        return hashtagResults
    }

    fun getLocationResults(): LiveData<Resource<List<SearchItem>?>> {
        return locationResults
    }

    fun submitQuery(query: String?) {
        var localQuery = query
        if (query == null) {
            localQuery = ""
        }
        if (tempQuery != null && localQuery!!.lowercase(Locale.getDefault()) == tempQuery!!.lowercase(Locale.getDefault())) return
        tempQuery = query
        if (isEmpty(query)) {
            // If empty immediately post it
            searchDebouncer.cancel(QUERY)
            this.query.postValue("")
            return
        }
        searchDebouncer.call(QUERY)
    }

    fun search(
        query: String,
        type: FavoriteType
    ) {
        val liveData = getLiveDataByType(type) ?: return
        if (isEmpty(query)) {
            showRecentSearchesAndFavorites(type, liveData)
            return
        }
        if (query == "@" || query == "#") return
        val c: String
        c = when (type) {
            FavoriteType.TOP -> "blended"
            FavoriteType.USER -> "user"
            FavoriteType.HASHTAG -> "hashtag"
            FavoriteType.LOCATION -> "place"
            else -> return
        }
        liveData.postValue(loading<List<SearchItem>?>(null))
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = searchRepository.search(isLoggedIn, query, c)
                parseResponse(response, type)
            }
            catch (e: Exception) {
                sendErrorResponse(type)
            }
        }
    }

    private fun showRecentSearchesAndFavorites(
        type: FavoriteType,
        liveData: MutableLiveData<Resource<List<SearchItem>?>>
    ) {
        val recentResultsFuture = SettableFuture.create<List<RecentSearch>>()
        val favoritesFuture = SettableFuture.create<List<Favorite>>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recentSearches = recentSearchRepository.getAllRecentSearches()
                recentResultsFuture.set(
                    if (type == FavoriteType.TOP) recentSearches
                    else recentSearches.stream()
                        .filter { (_, _, _, _, _, type1) -> type1 === type }
                        .collect(Collectors.toList())
                )
            }
            catch (e: Exception) {
                recentResultsFuture.set(emptyList())
            }
            try {
                val favorites = favoriteRepository.getAllFavorites()
                favoritesFuture.set(
                    if (type == FavoriteType.TOP) favorites
                    else favorites
                        .stream()
                        .filter { (_, _, type1) -> type1 === type }
                        .collect(Collectors.toList())
                )
            }
            catch (e: Exception) {
                favoritesFuture.set(emptyList())
            }
        }
        val listenableFuture = Futures.allAsList<List<*>>(recentResultsFuture, favoritesFuture)
        Futures.addCallback(listenableFuture, object : FutureCallback<List<List<*>?>?> {
            override fun onSuccess(result: List<List<*>?>?) {
                if (!isEmpty(tempQuery)) return  // Make sure user has not entered anything before updating results
                if (result == null) {
                    liveData.postValue(success(emptyList()))
                    return
                }
                try {
                    liveData.postValue(
                        success(
                            ImmutableList.builder<SearchItem>()
                                .addAll(SearchItem.fromRecentSearch(result[0] as List<RecentSearch?>?))
                                .addAll(SearchItem.fromFavorite(result[1] as List<Favorite?>?))
                                .build()
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "onSuccess: ", e)
                    liveData.postValue(success(emptyList()))
                }
            }

            override fun onFailure(t: Throwable) {
                if (!isEmpty(tempQuery)) return
                liveData.postValue(success(emptyList()))
                Log.e(TAG, "onFailure: ", t)
            }
        }, mainThread)
    }

    private fun sendErrorResponse(type: FavoriteType) {
        val liveData = getLiveDataByType(type) ?: return
        liveData.postValue(error(null, emptyList()))
    }

    private fun getLiveDataByType(type: FavoriteType): MutableLiveData<Resource<List<SearchItem>?>>? {
        val liveData: MutableLiveData<Resource<List<SearchItem>?>>
        liveData = when (type) {
            FavoriteType.TOP -> topResults
            FavoriteType.USER -> userResults
            FavoriteType.HASHTAG -> hashtagResults
            FavoriteType.LOCATION -> locationResults
            else -> return null
        }
        return liveData
    }

    private fun parseResponse(
        body: SearchResponse,
        type: FavoriteType
    ) {
        val liveData = getLiveDataByType(type) ?: return
        if (isLoggedIn) {
            if (body.list == null) {
                liveData.postValue(success(emptyList()))
                return
            }
            if (type === FavoriteType.HASHTAG || type === FavoriteType.LOCATION) {
                liveData.postValue(success(body.list
                    .stream()
                    .filter { i: SearchItem -> i.user == null }
                    .collect(Collectors.toList())))
                return
            }
            liveData.postValue(success(body.list))
            return
        }

        // anonymous
        val list: List<SearchItem>?
        list = when (type) {
            FavoriteType.TOP -> ImmutableList
                .builder<SearchItem>()
                .addAll(body.users ?: emptyList())
                .addAll(body.hashtags ?: emptyList())
                .addAll(body.places ?: emptyList())
                .build()
            FavoriteType.USER -> body.users
            FavoriteType.HASHTAG -> body.hashtags
            FavoriteType.LOCATION -> body.places
            else -> return
        }
        liveData.postValue(success(list))
    }

    fun saveToRecentSearches(searchItem: SearchItem?) {
        if (searchItem == null) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recentSearch = fromSearchItem(searchItem)
                recentSearchRepository.insertOrUpdateRecentSearch(recentSearch!!)
            } catch (e: Exception) {
                Log.e(TAG, "saveToRecentSearches: ", e)
            }
        }
    }

    fun deleteRecentSearch(searchItem: SearchItem?): LiveData<Resource<Any?>>? {
        if (searchItem == null || !searchItem.isRecent) return null
        val (_, igId, _, _, _, type) = fromSearchItem(searchItem) ?: return null
        val data = MutableLiveData<Resource<Any?>>()
        data.postValue(loading(null))
        viewModelScope.launch(Dispatchers.IO) {
            try {
                recentSearchRepository.deleteRecentSearchByIgIdAndType(igId, type)
                data.postValue(success(Any()))
            }
            catch (e: Exception) {
                data.postValue(error(e.message, null))
            }
        }
        return data
    }

    companion object {
        private val TAG = SearchFragmentViewModel::class.java.simpleName
        private const val QUERY = "query"
    }
}