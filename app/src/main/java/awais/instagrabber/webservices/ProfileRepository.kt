package awais.instagrabber.webservices

import awais.instagrabber.repositories.ProfileService
import awais.instagrabber.repositories.responses.Media
import awais.instagrabber.repositories.responses.PostsFetchResponse
import awais.instagrabber.repositories.responses.WrappedMedia
import awais.instagrabber.repositories.responses.saved.CollectionsListResponse
import awais.instagrabber.utils.Utils
import com.google.common.collect.ImmutableMap
import java.util.*
import java.util.stream.Collectors

open class ProfileRepository(private val repository: ProfileService) {
    suspend fun fetchPosts(
        userId: Long,
        maxId: String?
    ): PostsFetchResponse? {
        val builder = ImmutableMap.builder<String?, String?>()
        if (!maxId.isNullOrEmpty()) {
            builder.put("max_id", maxId)
        }
        val body = repository.fetch(userId, builder.build()) ?: return null
        return PostsFetchResponse(
            body.items,
            body.moreAvailable,
            body.nextMaxId
        )
    }

    suspend fun fetchSaved(maxId: String?, collectionId: String): PostsFetchResponse? {
        val builder = ImmutableMap.builder<String?, String?>()
        if (!maxId.isNullOrEmpty()) {
            builder.put("max_id", maxId)
        }
        val userFeedResponse = if (collectionId.isNullOrEmpty() || collectionId == "ALL_MEDIA_AUTO_COLLECTION")
                (repository.fetchSaved(builder.build()) ?: return null)
            else repository.fetchSavedCollection(collectionId, builder.build()) ?: return null
        val items = userFeedResponse.items
        val posts: List<Media> = if (items == null) {
            emptyList()
        } else {
            items.stream()
                .map(WrappedMedia::media)
                .filter { obj: Media? -> Objects.nonNull(obj) }
                .collect(Collectors.toList())
        }
        return PostsFetchResponse(
            posts,
            userFeedResponse.isMoreAvailable,
            userFeedResponse.nextMaxId
        )
    }

    suspend fun fetchCollections(maxId: String?): CollectionsListResponse? {
        val builder = ImmutableMap.builder<String?, String?>()
        if (!maxId.isNullOrEmpty()) {
            builder.put("max_id", maxId)
        }
        builder.put(
            "collection_types",
            "[\"ALL_MEDIA_AUTO_COLLECTION\",\"MEDIA\",\"PRODUCT_AUTO_COLLECTION\"]"
        )
        return repository.fetchCollections(builder.build())
    }

    suspend fun createCollection(
        name: String,
        deviceUuid: String,
        userId: Long,
        csrfToken: String
    ): String? {
        val form: MutableMap<String, Any> = HashMap(6)
        form["_csrftoken"] = csrfToken
        form["_uuid"] = deviceUuid
        form["_uid"] = userId
        form["collection_visibility"] =
            "0" // 1 for public, planned for future but currently inexistant
        form["module_name"] = "collection_create"
        form["name"] = name
        val signedForm = Utils.sign(form)
        return repository.createCollection(signedForm)
    }

    suspend fun fetchLiked(maxId: String?): PostsFetchResponse? {
        val builder = ImmutableMap.builder<String?, String?>()
        if (!maxId.isNullOrEmpty()) {
            builder.put("max_id", maxId)
        }
        val userFeedResponse = repository.fetchLiked(builder.build()) ?: return null
        return PostsFetchResponse(
            userFeedResponse.items,
            userFeedResponse.moreAvailable,
            userFeedResponse.nextMaxId
        )
    }

    suspend fun fetchTagged(profileId: Long, maxId: String?): PostsFetchResponse? {
        val builder = ImmutableMap.builder<String?, String?>()
        if (!maxId.isNullOrEmpty()) {
            builder.put("max_id", maxId)
        }
        val userFeedResponse = repository.fetchTagged(profileId, builder.build()) ?: return null
        return PostsFetchResponse(
            userFeedResponse.items,
            userFeedResponse.moreAvailable,
            userFeedResponse.nextMaxId
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: ProfileRepository? = null

        fun getInstance(): ProfileRepository {
            return INSTANCE ?: synchronized(this) {
                val service = RetrofitFactory.retrofit.create(ProfileService::class.java)
                ProfileRepository(service).also { INSTANCE = it }
            }
        }
    }
}