package awais.instagrabber.repositories

import awais.instagrabber.repositories.responses.UserFeedResponse
import awais.instagrabber.repositories.responses.WrappedFeedResponse
import awais.instagrabber.repositories.responses.saved.CollectionsListResponse
import retrofit2.Call
import retrofit2.http.*

interface ProfileService {
    @GET("/api/v1/feed/user/{uid}/")
    suspend fun fetch(
        @Path("uid") uid: Long,
        @QueryMap queryParams: Map<String?, String?>?
    ): UserFeedResponse?

    @GET("/api/v1/feed/saved/")
    suspend fun fetchSaved(@QueryMap queryParams: Map<String?, String?>?): WrappedFeedResponse?

    @GET("/api/v1/feed/collection/{collectionId}/")
    suspend fun fetchSavedCollection(
        @Path("collectionId") collectionId: String?,
        @QueryMap queryParams: Map<String?, String?>?
    ): WrappedFeedResponse?

    @GET("/api/v1/feed/liked/")
    suspend fun fetchLiked(@QueryMap queryParams: Map<String?, String?>?): UserFeedResponse?

    @GET("/api/v1/usertags/{profileId}/feed/")
    suspend fun fetchTagged(
        @Path("profileId") profileId: Long,
        @QueryMap queryParams: Map<String?, String?>?
    ): UserFeedResponse?

    @GET("/api/v1/collections/list/")
    suspend fun fetchCollections(@QueryMap queryParams: Map<String?, String?>?): CollectionsListResponse?

    @FormUrlEncoded
    @POST("/api/v1/collections/create/")
    suspend fun createCollection(@FieldMap signedForm: Map<String?, String?>?): String?
}