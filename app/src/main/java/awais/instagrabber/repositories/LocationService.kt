package awais.instagrabber.repositories

import retrofit2.http.GET
import awais.instagrabber.repositories.responses.Place
import awais.instagrabber.repositories.responses.LocationFeedResponse
import retrofit2.Call
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface LocationService {
    @GET("/api/v1/locations/{location}/info/")
    suspend fun fetch(@Path("location") locationId: Long): Place?

    @GET("/api/v1/feed/location/{location}/")
    suspend fun fetchPosts(
        @Path("location") locationId: Long,
        @QueryMap queryParams: Map<String?, String?>?
    ): LocationFeedResponse?
}