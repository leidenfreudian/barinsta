package awais.instagrabber.repositories

import awais.instagrabber.repositories.responses.discover.TopicalExploreFeedResponse
import awais.instagrabber.repositories.responses.feed.FeedFetchResponse
import retrofit2.Call
import retrofit2.http.*

interface FeedService {
    @GET("/api/v1/discover/topical_explore/")
    suspend fun topicalExplore(@QueryMap queryParams: Map<String?, String?>?): TopicalExploreFeedResponse?

    @FormUrlEncoded
    @POST("/api/v1/feed/timeline/")
    suspend fun fetchFeed(@FieldMap signedForm: Map<String?, String?>?): FeedFetchResponse
}