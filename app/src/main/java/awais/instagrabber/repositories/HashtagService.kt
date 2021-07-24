package awais.instagrabber.repositories

import awais.instagrabber.repositories.responses.Hashtag
import awais.instagrabber.repositories.responses.TagFeedResponse
import retrofit2.Call
import retrofit2.http.*

interface HashtagService {
    @GET("/api/v1/tags/{tag}/info/")
    suspend fun fetch(@Path("tag") tag: String?): Hashtag?

    @FormUrlEncoded
    @POST("/api/v1/tags/{action}/{tag}/")
    suspend fun changeFollow(
        @FieldMap signedForm: Map<String?, String?>?,
        @Path("action") action: String?,
        @Path("tag") tag: String?
    ): String?

    @GET("/api/v1/feed/tag/{tag}/")
    suspend fun fetchPosts(
        @Path("tag") tag: String?,
        @QueryMap queryParams: Map<String?, String?>?
    ): TagFeedResponse?
}