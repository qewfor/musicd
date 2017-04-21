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

import club.minnced.kjda.entities.sendEmbedAsync
import club.minnced.kjda.entities.sendTextAsync
import club.minnced.kjda.plusAssign
import club.minnced.kjda.then
import com.futuremangaming.futurebot.Assets
import com.futuremangaming.futurebot.Permissions
import com.futuremangaming.futurebot.command.timestamp
import com.futuremangaming.futurebot.mask0
import com.futuremangaming.futurebot.mask1
import com.futuremangaming.futurebot.music.PlayerRemote
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import java.util.concurrent.atomic.AtomicLong

class Display(val channel: TextChannel, val remote: PlayerRemote) {

    val listener = DisplayAdapter(this)
    var message = AtomicLong(0)

    init {
        channel.jda.addEventListener(listener)
    }

    fun destroy() {
        channel.jda.removeEventListener(listener)
    }

    fun shuffle(mem: Member) {
        if (Permissions.isModerator(mem))
            remote.shuffle()
    }

    fun skip(mem: Member) {
        if (Permissions.isModerator(mem))
            remote.skipTrack()
    }

    fun show() {
        val bar = render()
        if (bar !== null) {
            channel.sendEmbedAsync {
                color { Assets.MUSIC_EMBED_COLOR }
                this += bar
                val info = trackInfo()
                if (info !== null) field {
                    name = "Currently Playing"
                    value = String.format("**[%.45s](%s)**", info.title?.mask0() ?: "T/A", info.uri?.mask1())
                }
            } then {
                message.set(it?.idLong ?: message.get())
                if (it !== null) {
                    it.addReaction(DisplaySymbol.SHUFFLE) then {
                        it.addReaction(DisplaySymbol.SKIP).queue()
                    }
                }
            }
        }
        else {
            channel.sendTextAsync { "Nothing to display!" }
        }
    }

    fun trackInfo(): AudioTrackInfo? = remote.player.playingTrack?.info

    internal fun render(): String? {
        val player = remote.player
        val track = player.playingTrack
        val info = track?.info ?: return null

        return buildString {
            this += "[`${timestamp(track.position)}`/`${timestamp(info.length)}`]"

            val vol = player.volume

            if (vol < 75)
                this += DisplaySymbol.VOLUME_LOW
            else if (vol < 150)
                this += DisplaySymbol.VOLUME_MED
            else
                this += DisplaySymbol.VOLUME_MAX

            val p1 = (track.position / info.length.toDouble()) // convert to double to get actual value
            val p2 = p1 * 10
            val position = Math.round(p2).toInt()

            repeat(10) {
                if (it == position)
                    this += DisplaySymbol.PLAY_POS
                else
                    this += DisplaySymbol.PLAY_BAR
            }

            this += DisplaySymbol.NOTE
        }
    }

}
