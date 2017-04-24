/*
 *     Copyright 2014-2017 FuturemanGaming
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.futuremangaming.futurebot

object Assets {

    val MUSIC_PLAYLIST_FOOTER: String? get() = System.getProperty("music.playlist.footer")
    val MUSIC_EMBED_COLOR: Int get() = Integer.decode(System.getProperty("music.embed.color", "0x50aace"))

    val TWITCH_URL: String? get() = System.getProperty("twitch.url")
    val TWITCH_FOOTER_ICON: String? get() = System.getProperty("twitch.footer.icon")

    val SOCIAL_MERCH: String? get() = System.getProperty("social.merch")
    val SOCIAL_TWITTER: String? get() = System.getProperty("social.twitter")
    val SOCIAL_YOUTUBE: String? get() = System.getProperty("social.youtube")
    val SOCIAL_TWITCH: String? get() = System.getProperty("social.twitch")

    val PAGES_NEXT: String? get()  = System.getProperty("pages.next")
    val PAGES_PREV: String? get()  = System.getProperty("pages.prev")
    val PAGES_LAST: String? get()  = System.getProperty("pages.last")
    val PAGES_FIRST: String? get() = System.getProperty("pages.first")

    val all = mapOf(
        "music.playlist.footer" to MUSIC_PLAYLIST_FOOTER,
        "music.embed.color"     to MUSIC_EMBED_COLOR,
        "twitch.url"            to TWITCH_URL,
        "twitch.footer.icon"    to TWITCH_FOOTER_ICON,
        "social.merch"   to SOCIAL_MERCH,
        "social.twitter" to SOCIAL_TWITTER,
        "social.youtube" to SOCIAL_YOUTUBE,
        "social.twitch"  to SOCIAL_TWITCH,
        "pages.next"  to PAGES_NEXT,
        "pages.prev"  to PAGES_PREV,
        "pages.last"  to PAGES_LAST,
        "pages.first" to PAGES_FIRST
    )

    init {
        val properties = System.getProperties()
        val stream = Assets::class.java.classLoader.getResourceAsStream("assets.properties")
        properties.load(stream)
    }

}