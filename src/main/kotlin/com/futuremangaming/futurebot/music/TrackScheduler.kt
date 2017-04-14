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

import com.futuremangaming.futurebot.getLogger
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.CLEANUP
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.REPLACED
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.STOPPED
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class TrackScheduler(val player: AudioPlayer, val guild: Guild, val manager: MusicManager) : AudioEventAdapter() {

    internal val queue: BlockingQueue<AudioTrack> = LinkedBlockingQueue()
    internal var voice: VoiceChannel? = null

    infix fun enqueue(track: AudioTrack): Boolean = synchronized(queue) {
        if (track.info.isStream) {
            return player.startTrack(track, false)
        }
        else if (!player.startTrack(track, true)) {
            queue.offer(track)
            return false
        }

        return true
    }

    fun nextTrack(skip: Boolean = false): Boolean = synchronized(queue) {
        while (queue.isNotEmpty()) {
            val track = queue.poll()
            if (player.startTrack(track, !skip))
                return true
            getLogger(TrackScheduler::class.java).warn("Track ${track.info.title} could not be played!")
        }

        destroy()
        return false
    }

    fun destroy() {
        queue.clear()
        manager.resetPlayer(guild)
        guild.audioManager.closeAudioConnection()
    }

    override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
        if (voice !== null && !guild.audioManager.isConnected)
            guild.audioManager.openAudioConnection(voice)
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason) {
        if (endReason === STOPPED || endReason === CLEANUP)
            return destroy()

        if (endReason !== REPLACED)
            nextTrack()
    }

    override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack, thresholdMs: Long) {
        getLogger(TrackScheduler::class.java).error("Track got stuck [${track.info.title}]. Starting next...")
        nextTrack(true)
    }

    override fun onTrackException(player: AudioPlayer?, track: AudioTrack?, exception: FriendlyException) {
        val log = getLogger(TrackScheduler::class.java)
        log.error("Encountered FriendlyException [${exception.severity}]")

        nextTrack(true)

        //if (exception.severity === FAULT) { exception.printStackTrace() }
    }
}
