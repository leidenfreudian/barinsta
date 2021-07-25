package awais.instagrabber.webservices

import android.util.Log
import awais.instagrabber.repositories.FeedService
import awais.instagrabber.repositories.responses.Media
import awais.instagrabber.repositories.responses.PostsFetchResponse
import awais.instagrabber.repositories.responses.discover.TopicalExploreFeedResponse
import awais.instagrabber.repositories.responses.feed.FeedFetchResponse
import awais.instagrabber.utils.TextUtils.isEmpty
import com.google.common.collect.ImmutableMap
import java.util.*

open class FeedRepository(private val repository: FeedService) {
    suspend fun fetchFeed(
        csrfToken: String,
        deviceUuid: String,
        cursor: String
    ): PostsFetchResponse {
        val form: MutableMap<String, String> = HashMap()
        form["_uuid"] = deviceUuid
        form["_csrftoken"] = csrfToken
        form["phone_id"] = UUID.randomUUID().toString()
        form["device_id"] = UUID.randomUUID().toString()
        form["client_session_id"] = UUID.randomUUID().toString()
        form["is_prefetch"] = "0"
        if (!isEmpty(cursor)) {
            form["max_id"] = cursor
            form["reason"] = "pagination"
        } else {
            form["is_pull_to_refresh"] = "1"
            form["reason"] = "pull_to_refresh"
        }
        return parseResponse(repository.fetchFeed(form.toMap()))
    }

    suspend fun topicalExplore(maxId: String): TopicalExploreFeedResponse? {
        val builder = ImmutableMap.builder<String, String>().put("module", "explore_popular")
        if (!isEmpty(maxId)) {
            builder.put("max_id", maxId)
        }
        return repository.topicalExplore(builder.build())
    }

    private fun parseResponse(feedFetchResponse: FeedFetchResponse): PostsFetchResponse {
        val moreAvailable = feedFetchResponse.isMoreAvailable
        var nextMaxId = feedFetchResponse.nextMaxId
        val needNewMaxId = nextMaxId == "feed_recs_head_load"
        val allPosts: MutableList<Media> = ArrayList()
        val items = feedFetchResponse.items
        for (media in items) {
            if (needNewMaxId && media!!.endOfFeedDemarcator != null) {
                val endOfFeedDemarcator = media.endOfFeedDemarcator
                val groupSet = endOfFeedDemarcator!!.groupSet ?: continue
                val groups = groupSet.groups ?: continue
                for (group in groups) {
                    val id = group.id
                    if (id == null || id != "past_posts") continue
                    nextMaxId = group.nextMaxId
                    val feedItems = group.feedItems
                    for (feedItem in feedItems) {
                        if (feedItem == null || feedItem.isInjected() || feedItem.type == null) continue
                        allPosts.add(feedItem)
                    }
                }
                continue
            }
            if (media == null || media.isInjected() || media.type == null) continue
            allPosts.add(media)
        }
        return PostsFetchResponse(allPosts, moreAvailable, nextMaxId)
    }

    companion object {
        @Volatile
        private var INSTANCE: FeedRepository? = null

        fun getInstance(): FeedRepository {
            return INSTANCE ?: synchronized(this) {
                val service = RetrofitFactory.retrofit.create(FeedService::class.java)
                FeedRepository(service).also { INSTANCE = it }
            }
        }
    }
}