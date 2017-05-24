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
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
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
        val volume = player.volume
        when (reaction) {
            DisplaySymbol.PLAY_PAUSE  -> display.togglePause()
            DisplaySymbol.SHUFFLE     -> display.shuffle(mem)
            DisplaySymbol.SKIP        -> display.skip(mem)
            DisplaySymbol.MUTED       -> player.volume = 0
            DisplaySymbol.VOLUME_DOWN -> decVolume(player, volume)
            DisplaySymbol.VOLUME_UP   -> incVolume(player, volume)
            DisplaySymbol.REFRESH     -> nop()
            else                      -> return
        }
        // On every successful interaction update the display
        display.channel.editMessageById(messageId, display.createMessage()).queue()
    }

    fun incVolume(player: AudioPlayer, vol: Int)
    {
        if (vol <= 140)
            player.volume += 10
    }

    fun decVolume(player: AudioPlayer, vol: Int)
    {
        if (vol >= 10)
            player.volume -= 10
    }

    fun nop() {}

}
