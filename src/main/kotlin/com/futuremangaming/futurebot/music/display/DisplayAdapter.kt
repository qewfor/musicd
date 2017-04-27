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

package com.futuremangaming.futurebot.music.display

import com.futuremangaming.futurebot.Permissions
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class DisplayAdapter(val display: Display) : ListenerAdapter() {

    override fun onGenericGuildMessageReaction(event: GenericGuildMessageReactionEvent) {
        onReaction(event.messageIdLong, event.reactionEmote.name, event.member)
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        if (event.member.user == event.jda.selfUser)
            display.remote.destroy()
    }

    fun onReaction(messageId: Long, reaction: String, mem: Member) {
        if (messageId != display.message.get() || mem.user == mem.jda.selfUser) return

        if (!Permissions.isModerator(mem)) return

        val player = display.remote.player
        when (reaction) {
            DisplaySymbol.SHUFFLE -> display.shuffle(mem)
            DisplaySymbol.SKIP    -> display.skip(mem)
            DisplaySymbol.MUTED      -> player.volume = 0
            DisplaySymbol.VOLUME_LOW -> player.volume = 50
            DisplaySymbol.VOLUME_MED -> player.volume = 100
            DisplaySymbol.VOLUME_MAX -> player.volume = 150
            else -> return
        }
        // On every successful interaction update the display
        display.channel.editMessageById(messageId, display.createMessage()).queue()
    }

}
