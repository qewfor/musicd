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
package com.futuremangaming.futurebot.music

import com.futuremangaming.futurebot.FutureBot
import com.futuremangaming.futurebot.internal.AbstractCommand
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException

import kotlin.jvm.JvmField as static

fun getMusic() = setOf(Play(), Skip())

/**
 * @author Florian Spieß
 * @since  2017-01-02
 */
class Play : MusicCommand("play") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        if (args.isBlank())
            return respond(event.channel, "Provide a link or id to a track resource!")

        val member = event.member
        val voice = event.guild.getVoiceChannelById(VOICE) ?: return respond(event.channel, "There is no voice channel specified. Contact the host!")
        val remote = bot.musicModule.remote(event.guild, voice)
        val isMod = member.isOwner || member.roles.any { it.id == MOD }

        val identifier = if (args.startsWith("http")) args else "ytsearch:$args"
        remote.handleRequest(TrackRequest(remote, identifier, member, event.channel, event.message), isMod)
    }
}

class Skip : MusicCommand("skip") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        val member = event.member
        val isMod = member.isOwner || member.roles.any { it.id == MOD }

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

open class MusicCommand(override val name: String) : AbstractCommand(name) {
    companion object {

        @static
        val MOD = System.getProperty("role.mod") ?: "-1"

        @static
        val CHANNEL = System.getProperty("channel.music") ?: "-1"

        @static
        val VOICE = System.getProperty("channel.music.voice") ?: "-1"

        @static
        val RESTRICTED = System.getProperty("app.music.restrict")?.toBoolean() ?: true
    }

    override fun checkPermission(member: Member): Boolean {
        return super.checkPermission(member) && member.voiceState?.channel?.id == VOICE
    }

    override fun checkIgnored(channel: TextChannel): Boolean {
        return RESTRICTED && channel.id != CHANNEL
    }

}
