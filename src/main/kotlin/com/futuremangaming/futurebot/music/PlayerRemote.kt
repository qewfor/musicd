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

package com.futuremangaming.futurebot.music

import com.futuremangaming.futurebot.music.display.Display
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.Queue

class PlayerRemote internal constructor(
    val player: AudioPlayer,
    val scheduler: TrackScheduler,
    val module: MusicModule,
    val guild: Long) {

    companion object {
        val LOG = LoggerFactory.getLogger(PlayerRemote::class.java)!!
    }

    init {
        LOG.debug("Created PlayerRemote in Guild with ID: {}", guild)
    }

    var voice: VoiceChannel? = null
        set(value) { scheduler.voice = value }
    var isPaused: Boolean
        get() = player.isPaused
        set(v) { player.isPaused = v }

    val displays: MutableMap<Long, Display> by lazy { mutableMapOf<Long, Display>() }
    val queue: Queue<AudioTrack> get() = scheduler.queue
    val remainingTime: Long get() {
        return queue
                .map { it.duration }
                .sum()
    }

    fun handleRequest(request: TrackRequest, allowLive: Boolean = false) {
        LOG.trace("Handling Request for ID: %s |> Requested by: %#s".format(request.id, request.member.user))
        val handler = TrackLoadHandler(request)
        handler.allowLive = allowLive

        MusicModule.PLAYER_MANAGER.loadItemOrdered(this, request.id, handler)
    }

    fun skipTrack(): Boolean {
        LOG.trace("Skipping Track")
        return scheduler.nextTrack(true)
    }

    fun shuffle() = synchronized(queue) {
        val tracks = queue.toMutableList()
        this.queue.clear()

        Collections.shuffle(tracks)

        queue += tracks
    }

    inline fun ifDisplayPresent(channel: TextChannel, block: Display.() -> Unit) {
        val display = displays[channel.idLong]
        if (display !== null)
            display.block()
    }

    fun display(channel: TextChannel) = displays.getOrPut(channel.idLong) {
        LOG.debug("Created new Display (!playing) in #{}.", channel.name)
        return@getOrPut Display(channel, this)
    }

    fun removeByName(name: String): Boolean = scheduler.queue.removeAll {
        StringUtils.containsIgnoreCase(it.info.title, name)
    }

    fun destroy() {
        LOG.debug("Deconstructing Remote for Guild with ID: {}", guild)
        module.remotes.remove(guild)
        scheduler.destroy()
        displays.forEach { it.value.destroy() }
    }
}
