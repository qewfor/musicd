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

package com.futuremangaming.futurebot.external

import com.futuremangaming.futurebot.AnsiCode.Companion.CYAN
import com.futuremangaming.futurebot.AnsiCode.Companion.GREEN
import com.futuremangaming.futurebot.AnsiCode.Companion.RESET
import com.futuremangaming.futurebot.external.LiveListener.Companion.LOG
import com.futuremangaming.futurebot.getConfig
import com.futuremangaming.futurebot.getLogger
import com.mashape.unirest.http.Unirest
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Game.GameType.TWITCH
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.user.UserGameUpdateEvent
import net.dv8tion.jda.core.hooks.EventListener
import java.awt.Color
import java.time.OffsetDateTime

/**
 * @author Florian Spieß
 * @since  2016-12-31
 */

val twitchColor: Color = Color.decode("#6441A4")

class LiveListener : EventListener {

    companion object {
        var channel: String? = "237351016511569921" // TODO Change
        var guild: String = "237300175893299201"    // TODO Change
        val LOG = getLogger("Twitch")
    }

    var streaming = false

    override fun onEvent(event: Event?) {
        if (event is UserGameUpdateEvent) {
            val user = event.user
            val member = event.guild.getMember(user)
            if (user.id != "244070237849387008") // TODO Change
                return
            synchronized(streaming) {
                if (member.game?.type === TWITCH && streaming.not()) {
                    streaming = true
                    event.jda.presence.game = member.game
                    announce(event.jda.getTextChannelById(channel) ?: event.jda.getGuildById(guild)?.publicChannel ?: event.guild.publicChannel)
                }
                else if (member.game?.type !== TWITCH && streaming) {
                    streaming = false
                    event.jda.presence.game = null
                    LOG.info("${CYAN}Reset status$RESET")
                }
            }
        }
    }
}

fun LiveListener.announce(channel: TextChannel) {
    try {
        channel.sendMessage(embed(stream())).queue({ LOG.info("${GREEN}Announced live event$RESET") })
    }
    catch (ex: Exception) {
        LiveListener.LOG.log(ex)
    }
}

fun stream(): Map<String, Any?>? {
    val client: String = (getConfig("login")["twitch_key"] as? String) ?: return null
    val response = Unirest.get("https://api.twitch.tv/kraken/streams/timthetatman") // TODO Change
                     .header("accept", "application/vnd.twitchtv.v3+json")
                     .header("client-id", client)
                     .asJson()
    if (response.status >= 300) {
        LiveListener.LOG.error("[TWITCH] Invalid response: " + response.statusText)
        return null
    }
    return response.body.`object`?.toMap()
}

fun embed(map: Map<String, Any?>?): MessageEmbed? {
    if (map === null) return null
    val stream = map["stream"] as? Map<String, Any> ?: throw IllegalStateException("Stream is not live")
    val previews = stream["preview"] as Map<String, Any>
    val channel: Map<String, Any> = stream["channel"] as? Map<String, Any> ?: throw IllegalArgumentException("Channel was null")
    LiveListener.LOG.internal(stream.toString())
    LiveListener.LOG.internal(channel.toString())

    val builder = EmbedBuilder()
    builder.setUrl("https://twitch.tv/FuturemanGaming")
    builder.setTitle("Futureman is live now!")
    builder.setDescription("<:fmgSUP:219939370575069194> " + channel["status"]?.toString())
    builder.setAuthor("FuturemanGaming", "https://twitch.tv/FuturemanGaming/profile", channel["logo"] as? String)
    builder.setColor(twitchColor)
    builder.setImage(previews["large"] as String)
    builder.setTimestamp(OffsetDateTime.parse(stream["created_at"] as? String))

    builder.addField("Directory", channel["game"] as? String, true)
    builder.setThumbnail("https://static-cdn.jtvnw.net/ttv-boxart/${channel["game"]}-138x190.jpg") // TODO checks
    return builder.build()
}
