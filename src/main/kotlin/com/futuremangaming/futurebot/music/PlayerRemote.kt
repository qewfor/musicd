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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.core.entities.VoiceChannel
import org.apache.commons.lang3.StringUtils
import java.util.Collections
import java.util.Queue
import java.util.concurrent.LinkedBlockingQueue

class PlayerRemote internal constructor(val player: AudioPlayer, val scheduler: TrackScheduler) {

    var voice: VoiceChannel? = null
        set(value) { scheduler.voice = value }
    var isPaused: Boolean
        get() = player.isPaused
        set(v) { player.isPaused = v }

    val queue: Queue<AudioTrack> get() = scheduler.queue

    val remainingTime: Long get() {
        return queue
                .asSequence()
                .map { it.duration }
                .sum()
    }

    fun handleRequest(request: TrackRequest, allowLive: Boolean = false) {
        val handler = TrackLoadHandler(request)
        handler.allowLive = allowLive

        MusicModule.PLAYER_MANAGER.loadItemOrdered(this, request.id, handler)
    }

    fun skipTrack() = scheduler.nextTrack(true)

    fun shuffle() {

        val queue = queue.toList()

        Collections.shuffle(queue)

        scheduler.queue = LinkedBlockingQueue(queue)
    }

    fun removeByName(name: String): Boolean = scheduler.queue.removeAll {
        StringUtils.containsIgnoreCase(it.info.title, name)
    }

    fun destroy() {
        scheduler.destroy()
    }
}
