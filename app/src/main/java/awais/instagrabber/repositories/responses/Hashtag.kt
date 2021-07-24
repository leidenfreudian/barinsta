package awais.instagrabber.repositories.responses

import java.io.Serializable

data class Hashtag(
    val id: String,
    val name: String,
    val mediaCount: Long,
    val following: Int?, // 0 false 1 true; not on search results
    val searchResultSubtitle: String? // shows how many posts there are on search results
) : Serializable {
    val follow: Boolean
        get() = following == 1
}