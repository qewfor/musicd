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

import club.minnced.kjda.embed
import club.minnced.kjda.message
import club.minnced.kjda.plusAssign
import club.minnced.kjda.then
import com.futuremangaming.futurebot.Assets
import com.futuremangaming.futurebot.Permissions
import com.futuremangaming.futurebot.command.timestamp
import com.futuremangaming.futurebot.music.PlayerRemote
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import java.util.concurrent.atomic.AtomicLong

class Display(val channel: TextChannel, val remote: PlayerRemote) {

    val listener = DisplayAdapter(this)
    val message = AtomicLong(0)

    init {
        channel.jda.addEventListener(listener)
    }

    fun destroy() {
        channel.jda.removeEventListener(listener)
        channel.deleteMessageById(message.get()).queue(null) {}
    }

    fun shuffle(mem: Member) {
        if (Permissions.isModerator(mem))
            remote.shuffle()
    }

    fun skip(mem: Member) {
        if (Permissions.isModerator(mem))
            remote.skipTrack()
    }

    fun togglePause() {
        remote.isPaused = !remote.isPaused
    }

    fun show() {
        val message = createMessage()
        channel.sendMessage(message).then(this::addReactions)
    }

    fun trackInfo(): AudioTrackInfo? = remote.player.playingTrack?.info

    fun createMessage(): Message {
        val bar = render()
        if (bar !== null) return message {
            embed {
                color { Assets.MUSIC_EMBED_COLOR }
                val info = trackInfo()
                if (info !== null) {
                    title = "%.40s".format(info.title)
                    url = info.uri
                    field {
                        value = bar + "\n**Volume**: `%d`/`150`".format(remote.player.volume)
                    }
                }
                else {
                    this += "Nothing to display!"
                }
            }
        }
        else return message {
            embed {
                color { Assets.MUSIC_EMBED_COLOR }
                this += "Nothing to display!"
            }
        }
    }

    internal fun addReactions(message: Message?) {
        if (message == null) return
        this.message.set(message.idLong)
        message.addReaction(DisplaySymbol.REFRESH).queue()
        message.addReaction(DisplaySymbol.PLAY_PAUSE).queue()
        message.addReaction(DisplaySymbol.SHUFFLE).queue()
        message.addReaction(DisplaySymbol.SKIP).queue()
        message.addReaction(DisplaySymbol.MUTED).queue()
        message.addReaction(DisplaySymbol.VOLUME_DOWN).queue()
        message.addReaction(DisplaySymbol.VOLUME_UP).queue()
    }

    internal fun render(): String? {
        val player = remote.player
        val track = player.playingTrack
        val info = track?.info ?: return null

        return buildString {
            this += "[`${timestamp(track.position)}`/`${timestamp(info.length)}`]"

            if (remote.isPaused)
                this += DisplaySymbol.PAUSE
            else
                this += DisplaySymbol.PLAY

            this += DisplaySymbol.NOTE

            val p1 = (track.position / info.length.toDouble()) // convert to double to get actual value
            val p2 = p1 * 10
            val position = Math.floor(p2).toInt()

            repeat(10) {
                if (it == position)
                    this += DisplaySymbol.PLAY_POS
                else
                    this += DisplaySymbol.PLAY_BAR
            }

            val vol = player.volume

            if (vol < 10)
                this += DisplaySymbol.MUTED
            else if (vol < 75)
                this += DisplaySymbol.VOLUME_LOW
            else if (vol < 150)
                this += DisplaySymbol.VOLUME_DOWN //aka mid
            else
                this += DisplaySymbol.VOLUME_UP //aka max
        }
    }

}
