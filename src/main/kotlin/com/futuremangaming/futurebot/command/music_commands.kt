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
import com.futuremangaming.futurebot.*
import com.futuremangaming.futurebot.internal.AbstractCommand
import com.futuremangaming.futurebot.internal.CommandGroup
import com.futuremangaming.futurebot.music.TrackRequest
import com.futuremangaming.futurebot.music.delete
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.jvm.JvmField as static

fun getMusic() = setOf(Play, Skip, Queue, Shuffle, NowPlaying)

object Play : MusicCommand("play") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        val channel = event.channel
        val member = event.member
        val guild = event.guild
        if (args.isBlank())
            return respond(channel, "Provide a link or id to a track resource!")

        val voice = guild.getVoiceChannelById(VOICE)
                ?: event.member.connectedChannel
                ?: return respond(channel, "There is no voice channel specified. Contact the host!")
        val remote = bot.musicModule.remote(guild, voice)
        val isMod = Permissions.isModerator(member)

        val identifier = if (args.startsWith("http") || args.startsWith("ytsearch:")) args else "ytsearch:$args"
        remote.handleRequest(TrackRequest(remote, identifier, member, channel, event.message), isMod)
    }
}

object Skip : MusicCommand("skip") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        val member = event.member

        if (!Permissions.isModerator(member))
            return respond(event.channel, "Only moderators are allowed to skip!")

        val voice = member.voiceState.channel
                ?: return respond(event.channel, "You can only skip something when you are in a voice channel")
        val remote = bot.musicModule.remote(event.guild, voice)

        if (!remote.skipTrack())
            return respond(event.channel, "${member.asMention}, queue has finished!")

        respond(event.channel, "${member.asMention} skipped current track!")

        event.message.delete("Music Cleanup")
    }
}

object Shuffle : MusicCommand("shuffle") {

    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        val member = event.member

        if (!Permissions.isModerator(member))
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

        if ((queue.isEmpty() && track === null)
                && remote.voice == null)
            return respond(event.channel, "There is currently no queue to display!")

        event.channel.sendEmbedAsync {
            color { Assets.MUSIC_EMBED_COLOR }

            var info = track.info

            if (info.isStream) {
                val title = info.title
                this += "🎥 **Live** [%s](%s)".format(
                        (if (title.length >= 40) "${title.substring(0..37)}..." else title).mask0(), info.uri.mask1())
                return@sendEmbedAsync
            }

            this += "Playing: [`${timestamp(track.position)}`/`${timestamp(track.duration)}`] " +
                    "**[${info.title.mask0()}](${info.uri.mask1()})**"

            if (queue.isNotEmpty()) {

                footer {
                    value = "[${queue.size} Tracks] ${timeFormat(remote.remainingTime).replace("**", "")}"
                    icon = Assets.MUSIC_PLAYLIST_FOOTER
                }

                this += "\n\n"
                val lines = mutableListOf<String>()

                for (i in 0..4) {
                    if (queue.size <= i) break
                    val song = queue[i]
                    info = song.info
                    val title = info.title
                    lines += "`%d.` **[%s](%s)** [`%s`]".format(i + 1,
                            (if (title.length >= 40) "${title.substring(0..37)}..." else title).mask0(), info.uri.mask1(),
                            if (info.isStream) "live" else timestamp(info.length))
                }

                this += lines.joinToString(separator = "\n")

                if (queue.size > 5)
                    this += "\n..."
            }
        }
    }

    override fun checkPermission(member: Member) = true
}

object NowPlaying : MusicCommand("playing") {

    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        val remote = bot.musicModule.remote(event.guild)
        remote.display(event.channel).show()
    }

    override fun checkPermission(member: Member) = true
}

fun timestamp(time: Long): String {
    val u = MILLISECONDS
    val hours = u.toHours(time) % 24
    val minutes = u.toMinutes(time) % 60
    val seconds = u.toSeconds(time) % 60

    if (hours > 0)
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    else
        return "%02d:%02d".format(minutes, seconds)
}

open class MusicCommand(override val name: String) : AbstractCommand(name) {
    internal companion object {
        val CHANNEL: String get() = System.getProperty("channel.music") ?: "-1"
        val VOICE: String get() = System.getProperty("channel.music.voice") ?: "-1"
        val RESTRICTED: Boolean get() = System.getProperty("app.music.restrict")?.toBoolean() ?: true
    }

    override val group: CommandGroup = CommandGroup("Music", "music")

    override fun checkPermission(member: Member): Boolean {
        return (!RESTRICTED || (member.connectedChannel?.id == VOICE && Permissions.isSubscriber(member)))
    }

    override fun checkIgnored(channel: TextChannel): Boolean {
        return RESTRICTED && channel.id != CHANNEL
    }

}
