package awais.instagrabber.webservices

import android.util.Log
import awais.instagrabber.repositories.HashtagService
import awais.instagrabber.repositories.responses.Hashtag
import awais.instagrabber.repositories.responses.PostsFetchResponse
import awais.instagrabber.repositories.responses.TagFeedResponse
import awais.instagrabber.utils.TextUtils.isEmpty
import awais.instagrabber.utils.Utils
import awais.instagrabber.webservices.RetrofitFactory.retrofit
import com.google.common.collect.ImmutableMap
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import java.util.*

open class HashtagRepository(private val repository: HashtagService) {
    suspend fun fetch(tag: String): Hashtag? {
        return repository.fetch(tag)
    }

    suspend fun changeFollow(
        action: String,
        tag: String,
        csrfToken: String,
        userId: Long,
        deviceUuid: String
    ): Boolean {
        val form: MutableMap<String, Any> = HashMap(3)
        form["_csrftoken"] = csrfToken
        form["_uid"] = userId
        form["_uuid"] = deviceUuid
        val signedForm = Utils.sign(form)
        val body = repository.changeFollow(signedForm, action, tag) ?: return false
        val jsonObject = JSONObject(body)
        return jsonObject.optString("status") == "ok"
    }

    suspend fun fetchPosts(tag: String, maxId: String?): PostsFetchResponse? {
        val builder = ImmutableMap.builder<String?, String?>()
        if (!isEmpty(maxId)) {
            builder.put("max_id", maxId)
        }
        val body = repository.fetchPosts(tag, builder.build()) ?: return null
        return PostsFetchResponse(
            body.items,
            body.moreAvailable,
            body.nextMaxId
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: HashtagRepository? = null

        fun getInstance(): HashtagRepository {
            return INSTANCE ?: synchronized(this) {
                val service = RetrofitFactory.retrofit.create(HashtagService::class.java)
                HashtagRepository(service).also { INSTANCE = it }
            }
        }
    }
}