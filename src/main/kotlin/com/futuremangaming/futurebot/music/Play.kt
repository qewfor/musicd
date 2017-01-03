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
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

fun getMusic() = setOf(Play())

/**
 * @author Florian Spieß
 * @since  2017-01-02
 */
class Play : AbstractCommand("track") {

    override fun checkPermission(member: Member): Boolean {
        return super.checkPermission(member) && member.voiceState?.inVoiceChannel() ?: false
    }

    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        if (args.isBlank()) return respond(event.channel, "Provide a link or id to a track resource!")
        val member = event.member
        val voice = member.voiceState.channel ?: return respond(event.channel, "You can only request something when you are in a voice channel")
        val remote = bot.musicModule.remote(event.guild, voice)

        remote.handleRequest(TrackRequest(remote, args, member, event.channel, event.message))
    }
}
