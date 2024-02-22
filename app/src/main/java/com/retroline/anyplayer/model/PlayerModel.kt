package com.retroline.anyplayer.model

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.retroline.anyplayer.model.db.MapConvert
import com.retroline.anyplayer.utils.GlobalFunctions
import java.io.Serializable

/*
params
======================
1. Link:String (LINK)
2. User_agent:String (USER_AGENT)
3. DRM:String (DRM)
4. cookies:String (COOKIES)
5. mainLink:String (MAIN_LINK)

======================
1. title:String (TITLE)
2. description:String (DESCRIPTION)
3. mLanguage:String (LANGUAGE)
======================
1. header_source_req:String[] (HEADERS)
======================
 */
@Keep
@Entity(tableName = "playerModel")
data class PlayerModel(
    @PrimaryKey var id: Long = 0L,
    @ColumnInfo(name = "link") var link: String = "",
    @ColumnInfo(name = "image") var image: String = "",
    @ColumnInfo(name = "mainLink") var mainLink: String = "",
    @ColumnInfo(name = "USER_AGENT") var userAgent: String = GlobalFunctions.USER_AGENT,
    @ColumnInfo(name = "drm") var drmSting: String = "",
    @ColumnInfo(name = "cookies") var cookies: String = "",
    @ColumnInfo(name = "duration") val duration: Int = 0,
    @ColumnInfo(name = "title") var title: String = "AnyPlayer",
    @ColumnInfo(name = "description") var description: String = "AnyPlayer",
    @ColumnInfo(name = "language") var mLanguage: String = "default",
    @ColumnInfo(name = "cardImageUrl") var cardImageUrl: String = "",
    @ColumnInfo(name = "streamType") var streamType: Int = 0,
    @TypeConverters(MapConvert::class)
    @ColumnInfo(name = "headers")
    var headers: Map<String, String> = HashMap(),
) : Serializable {
    fun addHeader(key: String, value: String) {
        (headers as? HashMap<String, String>)?.put(key, value)
    }

//    override fun toString(): String {
//        return "Movie{" +
//                "id=" + id +
//                ", $titleIntent='" + title + '\'' +
//                ", $durationIntent='" + duration + '\'' +
////                ", $userAgentIntent='" + userAgent + '\'' +
////                ", $drmStringIntent='" + drmSting + '\'' +
////                ", $cookieIntent='" + cookies + '\'' +
////                ", $languageIntent='" + mLanguage + '\'' +
////                ", $descriptionIntent='" + description + '\'' +
////                ", $mainLinkIntent='" + mainLinkIntent + '\'' +
//                ", $linkIntent='" + link + '\'' +
//                ", $imageIntent='" + image + '\'' +
////                ", $cardImageIntent='" + cardImageUrl + '\'' +
//                ", $typeIntent='" + streamType + '\'' +
////                ", $headerIntent='" + headerString() + '\'' +
//                '}'
//
//    }

    companion object {
        const val linkIntent = "LINK"
        const val imageIntent = "IMAGE"
        const val cardImageIntent = "CARD_IMAGE"
        const val durationIntent = "DURATION"
        const val userAgentIntent = "USER_AGENT"
        const val drmStringIntent = "DRM"
        const val cookieIntent = "COOKIE"
        const val languageIntent = "LANGUAGE"
        const val typeIntent = "STREAM_TYPE"
        const val headerIntent = "HEADERS"
        const val titleIntent = "TITLE"
        const val descriptionIntent = "DESCRIPTION"
        const val mainLinkIntent = "MAIN_LINK"
        const val playerLatinoDomain = "app.playerlatino.live"
        const val DIRECT_PUT = "MODEL"
        const val SELECTED_MODEL = "SELECTED_MODEL"
        fun getId(link: String, title: String, type: Int): Long {
            return (link.hashCode().toLong() + title.hashCode().toLong() + type.toLong())
        }

        fun isLocal(type: Int): Boolean {
            return type == STREAM_OFFLINE_VIDEO || type == STREAM_OFFLINE_AUDIO || type == STREAM_OFFLINE_IMAGE
        }

        const val STREAM_ONLINE_GENERAL = 0
        const val STREAM_ONLINE_LIVE = 1
        const val STREAM_OFFLINE_VIDEO = 2
        const val STREAM_M3U = 3

        const val STREAM_OFFLINE_AUDIO = 4
        const val STREAM_OFFLINE_IMAGE = 5

        const val WEB_VIDEO = 6
    }
}