package awais.instagrabber.webservices

import awais.instagrabber.repositories.SearchService
import awais.instagrabber.repositories.responses.search.SearchResponse
import awais.instagrabber.webservices.RetrofitFactory.retrofitWeb
import com.google.common.collect.ImmutableMap
import retrofit2.Call

open class SearchRepository(private val service: SearchService) {
    suspend fun search(
        isLoggedIn: Boolean,
        query: String,
        context: String
    ): SearchResponse {
        val builder = ImmutableMap.builder<String, String>()
        builder.put("query", query)
        // context is one of: "blended", "user", "place", "hashtag"
        // note that "place" and "hashtag" can contain ONE user result, who knows why
        builder.put("context", context)
        builder.put("count", "50")
        return service.search(
            if (isLoggedIn) "https://i.instagram.com/api/v1/fbsearch/topsearch_flat/" else "https://www.instagram.com/web/search/topsearch/",
            builder.build()
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: SearchRepository? = null

        fun getInstance(): SearchRepository {
            return INSTANCE ?: synchronized(this) {
                val service: SearchService = RetrofitFactory.retrofit.create(SearchService::class.java)
                SearchRepository(service).also { INSTANCE = it }
            }
        }
    }
}