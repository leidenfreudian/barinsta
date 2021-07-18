package awais.instagrabber.repositories

import awais.instagrabber.repositories.responses.search.SearchResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface SearchService {
    @GET
    suspend fun search(
        @Url url: String?,
        @QueryMap(encoded = true) queryParams: Map<String?, String?>?
    ): SearchResponse
}