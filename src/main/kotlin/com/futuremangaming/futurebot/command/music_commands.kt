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
@file:JvmName("PlayCommand")
package com.futuremangaming.futurebot.command

import club.minnced.kjda.entities.connectedChannel
import club.minnced.kjda.entities.sendEmbedAsync
import com.futuremangaming.futurebot.FutureBot
import com.futuremangaming.futurebot.internal.AbstractCommand
import com.futuremangaming.futurebot.music.TrackRequest
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.jvm.JvmField as static

fun getMusic() = setOf(Play, Skip, Queue, Shuffle)

/**
 * @author Florian Spieß
 * @since  2017-01-02
 */
object Play : MusicCommand("play") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        if (args.isBlank())
            return respond(event.channel, "Provide a link or id to a track resource!")

        val member = event.member
        val voice = event.guild.getVoiceChannelById(VOICE())
                ?: event.member.connectedChannel
                ?: return respond(event.channel, "There is no voice channel specified. Contact the host!")
        val remote = bot.musicModule.remote(event.guild, voice)
        val isMod = member.isOwner || member.roles.any { it.id == MOD() }

        val identifier = if (args.startsWith("http")) args else "ytsearch:$args"
        remote.handleRequest(TrackRequest(remote, identifier, member, event.channel, event.message), isMod)
    }
}

object Skip : MusicCommand("skip") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        val member = event.member
        val isMod = member.isOwner || member.roles.any { it.id == MOD() }

        if (!isMod)
            return respond(event.channel, "Only moderators are allowed to skip!")

        val voice = member.voiceState.channel
                ?: return respond(event.channel, "You can only request something when you are in a voice channel")
        val remote = bot.musicModule.remote(event.guild, voice)

        if (!remote.skipTrack())
            return respond(event.channel, "${member.asMention}, queue has finished!")

        respond(event.channel, "${member.asMention} skipped current track!")

        try { event.message.delete().queue() }
        catch (ex: PermissionException) { }
    }
}

object Shuffle : MusicCommand("shuffle") {

    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        val member = event.member
        val isMod = member.isOwner || member.roles.any { it.id == MOD() }

        if (!isMod)
            return respond(event.channel, "Only moderators are allowed to shuffle!")

        val remote = bot.musicModule.remote(event.guild)
        remote.shuffle()

        respond(event.channel, "The queue has been shuffled!")
    }
}

object Queue : MusicCommand("queue") {

    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        val remote = bot.musicModule.remote(event.guild)
        val queue = remote.queue.toList()
        val track = remote.player.playingTrack

        if ((queue.isEmpty() || track === null)
                && remote.voice == null)
            return respond(event.channel, "There is currently no queue to display!")

        event.channel.sendEmbedAsync {
            color { 0x50aace }

            this += "Currently Playing: [`${timestamp(track.position)}`/`${timestamp(track.duration)}`] " +
                    "**${track.info.title}**"

            if (queue.isNotEmpty()) {

                footer {
                    value = "[${queue.size} Tracks] ${timeFormat(remote.remainingTime).replace("**", "")}"
                    icon = "https://i.imgur.com/6iSNidq.png"
                }

                this += "\n\n"
                val lines = mutableListOf<String>()

                for (i in 0..4) {
                    if (queue.size <= i) break
                    val song = queue[i]
                    val info = song.info
                    lines += "`${i + 1}.` **${info.title}** " +
                            "[`${if (info.isStream) "live" else timestamp(info.length)}`]"
                }

                this += lines.joinToString(separator = "\n")

                if (queue.size > 5)
                    this += "\n..."
            }
        }
    }

    override fun checkPermission(member: Member): Boolean { return true }
}

fun timestamp(time: Long): String {
    val u = MILLISECONDS
    val hours = u.toHours(time) % 24
    val minutes = u.toMinutes(time) % 60
    val seconds = u.toSeconds(time) % 60

    if (u.toHours(time) > 0)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    else
        return String.format("%02d:%02d", minutes, seconds)
}

open class MusicCommand(override val name: String) : AbstractCommand(name) {
    companion object {
        val MOD        = { System.getProperty("role.mod") ?: "-1" }
        val CHANNEL    = { System.getProperty("channel.music") ?: "-1" }
        val VOICE      = { System.getProperty("channel.music.voice") ?: "-1" }
        val RESTRICTED = { System.getProperty("app.music.restrict")?.toBoolean() ?: true }
    }

    override fun checkPermission(member: Member): Boolean {
        return super.checkPermission(member) && (!RESTRICTED() || member.voiceState?.channel?.id == VOICE())
    }

    override fun checkIgnored(channel: TextChannel): Boolean {
        return RESTRICTED() && channel.id != CHANNEL()
    }

}
