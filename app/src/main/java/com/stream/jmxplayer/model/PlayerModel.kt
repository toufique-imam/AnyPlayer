package com.stream.jmxplayer.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.stream.jmxplayer.model.db.MapConvert
import com.stream.jmxplayer.utils.GlobalFunctions
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
    @ColumnInfo(name = "title") var title: String = "JMX Player",
    @ColumnInfo(name = "description") var description: String = "JMX Player",
    @ColumnInfo(name = "language") var mLanguage: String = "default",
    @ColumnInfo(name = "cardImageUrl") var cardImageUrl: String = "",
    @ColumnInfo(name = "streamType") var streamType: Int = 0,

    @TypeConverters(MapConvert::class)
    @ColumnInfo(name = "headers")
    var headers: Map<String, String> = HashMap(),
) : Serializable {

//    private fun headerString(): String {
//        val stringBuilder = StringBuilder()
//        for (i in headers.keys) {
//            stringBuilder.append("$i:${headers[i]} \n")
//        }
//        return stringBuilder.toString()
//    }
//
//    private fun stringToHeader(headerString: String) {
//        val headerArray = headerString.split(":")
//        var idx = 1
//        while (idx < headerArray.size) {
//            headers[headerArray[idx - 1]] = headerArray[idx]
//            idx += 2
//        }
//    }

    override fun toString(): String {
        return "Movie{" +
                "id=" + id +
                ", $titleIntent='" + title + '\'' +
                ", $userAgentIntent='" + userAgent + '\'' +
                ", $drmStringIntent='" + drmSting + '\'' +
                ", $cookieIntent='" + cookies + '\'' +
                ", $languageIntent='" + mLanguage + '\'' +
                ", $descriptionIntent='" + description + '\'' +
                ", $mainLinkIntent='" + mainLinkIntent + '\'' +
                ", $linkIntent='" + link + '\'' +
                ", $imageIntent='" + image + '\'' +
                ", $cardImageIntent='" + cardImageUrl + '\'' +
                ", $typeIntent='" + streamType + '\'' +
//                ", $headerIntent='" + headerString() + '\'' +
                '}'

    }

    companion object {
        const val linkIntent = "LINK"
        const val imageIntent = "IMAGE"
        const val cardImageIntent = "CARD_IMAGE"
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
        fun getId(link: String, title: String): Long {
            return (link.hashCode() + title.hashCode()).toLong()
        }
    }
}