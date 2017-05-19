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
@file:JvmName("Music")
package com.futuremangaming.futurebot.music

import club.minnced.kjda.catch
import club.minnced.kjda.entities.sendTextAsync
import com.futuremangaming.futurebot.getLogger
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.exceptions.PermissionException

val LOG = getLogger(MusicModule::class.java)

data class TrackRequest(
    val manager: PlayerRemote,
    val id: String,
    val member: Member,
    val channel: TextChannel,
    val message: Message
)

fun Message.delete(formatReason: String, vararg args: Any) = try {
    delete().reason(formatReason.format(*args)) catch { }
}
catch (ex: PermissionException) { }

fun send(channel: MessageChannel, msg: String) {
    try {
        channel.sendTextAsync { msg } catch { }
    }
    catch (ex: PermissionException) { }
    catch (ex: Exception) {
        LOG.error("Failed to send message", ex)
    }
}

fun AudioPlayerManager.loadItem(id: String, block: LoadHandlerBuilder.() -> Unit) {
    val builder = LoadHandlerBuilder()
    builder.block()
    loadItem(id, builder.build())
}

class LoadHandlerBuilder {

    var onFailure: (FriendlyException) -> Unit = { }
    var onPlaylist: (AudioPlaylist) -> Unit = { }
    var onTrack: (AudioTrack) -> Unit = { }
    var onNoMatch: () -> Unit = { }

    fun build() = object : AudioLoadResultHandler {
        override fun loadFailed(exception: FriendlyException) = onFailure(exception)
        override fun trackLoaded(track: AudioTrack) = onTrack(track)
        override fun noMatches() = onNoMatch()
        override fun playlistLoaded(playlist: AudioPlaylist) = onPlaylist(playlist)
    }

    infix fun onFailure(block: (FriendlyException) -> Unit): LoadHandlerBuilder {
        onFailure = block
        return this
    }

    infix fun onPlaylist(block: (AudioPlaylist) -> Unit): LoadHandlerBuilder {
        onPlaylist = block
        return this
    }

    infix fun onTrack(block: (AudioTrack) -> Unit): LoadHandlerBuilder {
        onTrack = block
        return this
    }

    infix fun onNoMatch(block: () -> Unit): LoadHandlerBuilder {
        onNoMatch = block
        return this
    }
}
