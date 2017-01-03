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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import org.apache.commons.lang3.exception.ExceptionUtils

/**
 * @author Florian Spieß
 * @since  2017-01-02
 */
class TrackLoadHandler(val trackRequest: TrackRequest) : AudioLoadResultHandler {

    override fun noMatches() {
        val (remote, id, member, channel) = trackRequest
        send(channel, "${member.asMention}, no tracks found for input `$id`!")
    }

    override fun loadFailed(exception: FriendlyException?) {
        val (remote, id, member, channel) = trackRequest
        send(channel, "${member.asMention}, failed to load track for id `$id`!")
        LOG.debug(ExceptionUtils.getStackTrace(exception?.cause))
    }

    override fun trackLoaded(track: AudioTrack?) {
        if (track === null) return
        val (remote, id, member, channel, message) = trackRequest
        delete(message)
        if (track.info.isStream)
            return send(channel, "Unable to play track for id `$id`: Live streams are not supported")

        val started: Boolean = remote.scheduler.enqueue(track)
        send(channel, "${if (started) "Started playing " else "Loaded "}track `${track.info.title}` [Requested by ${member.asMention}]")
    }

    override fun playlistLoaded(playlist: AudioPlaylist?) {
        if (playlist === null) return
        val (remote, id, member, channel, message) = trackRequest
        if (playlist.isSearchResult) {
            val track = playlist.selectedTrack ?: playlist.tracks.firstOrNull()
            if (track !== null)
                return trackLoaded(track)
            delete(message)
            return send(channel, "Unable to find anything for `${id.replaceFirst("ytsearch:", "")}`!")
        }

        delete(message)
        channel.sendTyping().queue()

        for (track in playlist.tracks)
            remote.scheduler.enqueue(track)

        send(channel, "Loaded playlist with **${playlist.tracks?.size}** tracks! [Requested by ${member.asMention}]")
    }

}
