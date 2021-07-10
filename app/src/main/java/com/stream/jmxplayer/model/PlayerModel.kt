package com.stream.jmxplayer.model

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

class PlayerModel : Serializable {
    companion object {
        const val linkIntent = "LINK"
        const val userAgentIntent = "USER_AGENT"
        const val drmStringIntent = "DRM"
        const val cookieIntent = "COOKIE"
        const val languageIntent = "LANGUAGE"
        const val headerIntent = "HEADERS"
        const val titleIntent = "TITLE"
        const val descriptionIntent = "DESCRIPTION"
        const val mainLinkIntent = "MAIN_LINK"
        const val typeIntent = "STREAM_TYPE"
        const val playerLatinoDomain = "app.playerlatino.live"
        const val DIRECT_PUT = "MODEL"

    }

    var link: String = ""
    var mainLink: String = ""
    var userAgent: String = GlobalFunctions.USER_AGENT
    var drmSting: String = ""
    var cookies: String = ""
    var title: String = "JMX Player"
    var description: String = "JMX Player"
    var mLanguage: String = "default"
    var streamType : Int = 0
    var headers: HashMap<String, String> = HashMap()


    override fun toString(): String {
        return (linkIntent + " : " + link + "\n"
                + mainLinkIntent + " : " + mainLink + "\n"
                + typeIntent + " : " + streamType + "\n"
                + userAgentIntent + " : " + userAgent + "\n"
                + drmStringIntent + " : " + drmSting + "\n"
                + cookieIntent + " : " + cookies + "\n"
                + titleIntent + " : " + title + "\n"
                + descriptionIntent + " : " + description + "\n"
                + languageIntent + " : " + mLanguage + "\n"
                )
    }

    constructor()

    constructor(title: String, link: String, userAgent: String) {
        this.title = title
        this.link = link
        this.userAgent = userAgent
        mainLink = link
        drmSting = ""
        cookies = ""
        streamType = 0
        description = "I love JMX Player"
        headers["User-Agent"] = userAgent
        headers["referer"] = mainLink
    }
}