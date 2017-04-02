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

import club.minnced.kjda.entities.sendTextAsync
import com.futuremangaming.futurebot.getLogger
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.CLEANUP
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.REPLACED
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.STOPPED
import gnu.trove.TDecorators
import gnu.trove.map.hash.TLongObjectHashMap
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.exceptions.PermissionException
import org.apache.commons.lang3.StringUtils
import java.util.Collections
import java.util.Queue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * @author Florian Spieß
 * @since  2017-01-02
 */
val LOG = getLogger("MusicModule")
private val PLAYER_MANAGER = DefaultAudioPlayerManager()

class MusicModule {

    companion object {
        init { AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER) }
    }

    val manager: MusicManager = MusicManager()

    fun remote(guild: Guild): PlayerRemote {
        val player = manager.getPlayer(guild)
        val queue = manager.getScheduler(guild)
        return PlayerRemote(player, queue)
    }

    fun remote(guild: Guild, voiceChannel: VoiceChannel): PlayerRemote {
        val remote = remote(guild)
        remote.voice = voiceChannel
        return remote
    }
}

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

        PLAYER_MANAGER.loadItemOrdered(this, request.id, handler)
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

class MusicManager {

    private val players: MutableMap<Long, AudioPlayer> = TDecorators.wrap(TLongObjectHashMap<AudioPlayer>())
    private val schedulers: MutableMap<AudioPlayer, TrackScheduler> = hashMapOf()

    fun resetPlayer(guild: Guild) {
        getPlayer(guild).destroy()
        schedulers.remove(players.remove(guild.idLong))
    }

    fun getPlayer(guild: Guild): AudioPlayer {
        return players.getOrPut(guild.idLong) {
            val player = PLAYER_MANAGER.createPlayer()
            val scheduler = schedulers.getOrPut(player, { TrackScheduler(player, guild, this@MusicManager) })

            guild.audioManager.sendingHandler = PlayerSendHandler(player)
            player.addListener(scheduler)
            player.volume = 75
            return@getOrPut player
        }
    }

    fun getScheduler(guild: Guild): TrackScheduler = schedulers[getPlayer(guild)]!!

}

class TrackScheduler(val player: AudioPlayer, val guild: Guild, val manager: MusicManager) : AudioEventAdapter() { // copied from demo

    internal var queue: BlockingQueue<AudioTrack> = LinkedBlockingQueue()
    internal var voice: VoiceChannel? = null

    infix fun enqueue(track: AudioTrack): Boolean {
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
            val track = queue.poll()
            if (player.startTrack(track, !skip))
                return true
            getLogger("Music") warn "Track ${track.info.title} could not be played!"
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

        if (endReason !== REPLACED)
            nextTrack()
    }

    override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack, thresholdMs: Long) {
        getLogger("Music") error "Track got stuck [${track.info.title}]. Starting next..."
        nextTrack(true)
    }

    override fun onTrackException(player: AudioPlayer?, track: AudioTrack?, exception: FriendlyException) {
        val log = getLogger("Music")
        log error "Encountered FriendlyException [${exception.severity}]"

        nextTrack(true)

        if (exception.severity === FAULT) { exception.printStackTrace() }
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
        message.delete().queue()
    }
    catch (ex: PermissionException) {}
}

fun send(channel: MessageChannel, msg: String) {
    try {
        channel.sendTextAsync { msg } catch { }
    }
    catch (ex: PermissionException) { }
    catch (ex: Exception) {
        LOG.log(ex)
    }
}
