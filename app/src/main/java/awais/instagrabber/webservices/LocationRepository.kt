package awais.instagrabber.webservices

import awais.instagrabber.repositories.HashtagService
import awais.instagrabber.repositories.responses.Location
import awais.instagrabber.repositories.responses.LocationFeedResponse
import awais.instagrabber.repositories.responses.Place
import awais.instagrabber.repositories.responses.PostsFetchResponse
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.repositories.LocationService
import awais.instagrabber.webservices.RetrofitFactory.retrofit
import com.google.common.collect.ImmutableMap
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

open class LocationRepository(private val repository: LocationService) {
    suspend fun fetchPosts(locationId: Long, maxId: String): PostsFetchResponse? {
        val builder = ImmutableMap.builder<String, String>()
        if (!isEmpty(maxId)) {
            builder.put("max_id", maxId)
        }
        val body = repository.fetchPosts(locationId, builder.build()) ?: return null
        return PostsFetchResponse(
            body.items,
            body.moreAvailable,
            body.nextMaxId
        )
    }

    suspend fun fetch(locationId: Long): Location? {
        val place = repository.fetch(locationId) ?: return null
        return place.location
    }

    companion object {
        @Volatile
        private var INSTANCE: LocationRepository? = null

        fun getInstance(): LocationRepository {
            return INSTANCE ?: synchronized(this) {
                val service = RetrofitFactory.retrofit.create(LocationService::class.java)
                LocationRepository(service).also { INSTANCE = it }
            }
        }
    }
}