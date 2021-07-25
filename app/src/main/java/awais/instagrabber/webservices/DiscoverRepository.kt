package awais.instagrabber.webservices

import awais.instagrabber.repositories.responses.discover.TopicalExploreFeedResponse
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.repositories.FeedService
import com.google.common.collect.ImmutableMap

open class DiscoverRepository(private val repository: FeedService) {


    companion object {
        @Volatile
        private var INSTANCE: DiscoverRepository? = null

        fun getInstance(): DiscoverRepository {
            return INSTANCE ?: synchronized(this) {
                val service = RetrofitFactory.retrofit.create(FeedService::class.java)
                DiscoverRepository(service).also { INSTANCE = it }
            }
        }
    }
}