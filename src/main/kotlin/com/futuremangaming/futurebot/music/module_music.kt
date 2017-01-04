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
@file:JvmName("MusicModuleKt")
package com.futuremangaming.futurebot.music

import com.futuremangaming.futurebot.getLogger
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.CLEANUP
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.STOPPED
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.exceptions.PermissionException
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.stream.Stream

/**
 * @author Florian Spieß
 * @since  2017-01-02
 */
val LOG = getLogger("MusicModule")
private val PLAYER_MANAGER = DefaultAudioPlayerManager()

class MusicModule {

    companion object {
        init {
            PLAYER_MANAGER.registerSourceManager(YoutubeAudioSourceManager())
            PLAYER_MANAGER.registerSourceManager(SoundCloudAudioSourceManager())
            PLAYER_MANAGER.registerSourceManager(TwitchStreamAudioSourceManager())
            PLAYER_MANAGER.registerSourceManager(BandcampAudioSourceManager())
            PLAYER_MANAGER.registerSourceManager(HttpAudioSourceManager())
        }
    }

    val manager: MusicManager = MusicManager()

    fun remote(guild: Guild, voiceChannel: VoiceChannel): PlayerRemote {
        val player = manager.getPlayer(guild)
        val queue = manager.getScheduler(guild)
        val remote = PlayerRemote(player, queue)
        remote.voice = voiceChannel
        return remote
    }
}

class PlayerRemote(val player: AudioPlayer, val scheduler: TrackScheduler) {

    var voice: VoiceChannel? = null
        set(value) { scheduler.voice = value }
    var isPaused: Boolean
        get() = player.isPaused
        set(v) { player.isPaused = v }

    fun handleRequest(request: TrackRequest, allowLive: Boolean = false) {
        val handler = TrackLoadHandler(request)
        handler.allowLive = allowLive

        PLAYER_MANAGER.loadItemOrdered(this, request.id, handler)
    }

    fun getQueue() = scheduler.queue

    fun getRemainingTime(): Long {
        return Stream.of(*getQueue().toTypedArray()).parallel()
                     .mapToLong { it.info.length }
                     .sum()
    }

    fun skipTrack() = scheduler.nextTrack(true)

    fun removeByName(name: String): Boolean {
        val list: MutableList<AudioTrack> = mutableListOf()
        scheduler.queue.forEach {
            if (StringUtils.containsIgnoreCase(it.info.title, name))
                list += it
        }

        return list.isNotEmpty()
    }

    fun destroy() {
        scheduler.destroy()
    }
}

class MusicManager {

    private val players: MutableMap<String, AudioPlayer> = mutableMapOf()
    private val schedulers: MutableMap<AudioPlayer, TrackScheduler> = mutableMapOf()

    fun resetPlayer(guild: Guild) {
        getPlayer(guild).destroy()
        schedulers.remove(players.remove(guild.id))
    }

    fun getPlayer(guild: Guild): AudioPlayer {
        return players.getOrPut(guild.id) {
            val player = PLAYER_MANAGER.createPlayer()
            val scheduler = schedulers.getOrPut(player, { TrackScheduler(player, guild, this@MusicManager) })

            guild.audioManager.sendingHandler = PlayerSendHandler(player)
            player.addListener(scheduler)
            player.volume = 35
            player
        }
    }

    fun getScheduler(guild: Guild): TrackScheduler {
        return schedulers[getPlayer(guild)]!!
    }
}

class TrackScheduler(val player: AudioPlayer, val guild: Guild, val manager: MusicManager) : AudioEventAdapter() { // copied from demo

    val queue: BlockingQueue<AudioTrack> = LinkedBlockingQueue()
    internal var voice: VoiceChannel? = null

    fun enqueue(track: AudioTrack): Boolean {
        if (track.info.isStream) {
            return player.startTrack(track, false)
        }
        else if (!player.startTrack(track, true)) {
            queue.offer(track)
            return false
        }

        return true
    }

    fun nextTrack(skip: Boolean = false): Boolean {
        while (queue.isNotEmpty()) {
            if (player.startTrack(queue.poll(), skip))
                return true
        }

        destroy()
        return false
    }

    fun destroy() {
        manager.resetPlayer(guild)
        guild.audioManager.closeAudioConnection()
    }

    override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
        if (voice !== null && !guild.audioManager.isConnected)
            guild.audioManager.openAudioConnection(voice)
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        if (endReason === STOPPED || endReason === CLEANUP)
            return destroy()

        nextTrack()
    }
}

data class TrackRequest(
        val manager: PlayerRemote,
        val id: String,
        val member: Member,
        val channel: TextChannel,
        val message: Message
)

fun delete(message: Message) {
    try {
        message.deleteMessage().queue()
    }
    catch (ex: PermissionException) {}
}

fun send(channel: MessageChannel, msg: String) {
    try {
        channel.sendMessage(msg).queue()
    }
    catch (ex: PermissionException) { }
    catch (ex: Exception) {
        LOG.log(ex)
    }
}
