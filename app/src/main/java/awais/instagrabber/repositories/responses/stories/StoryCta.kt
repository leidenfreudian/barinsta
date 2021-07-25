package awais.instagrabber.repositories.responses.stories

import java.io.Serializable
import com.google.gson.annotations.SerializedName
import awais.instagrabber.repositories.responses.Hashtag
import awais.instagrabber.repositories.responses.Location
import awais.instagrabber.repositories.responses.User

data class StoryCta(
    @SerializedName("webUri")
    val webUri: String?
) : Serializable