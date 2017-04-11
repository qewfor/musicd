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

import club.minnced.kjda.entities.sendEmbedAsync
import club.minnced.kjda.then
import com.futuremangaming.futurebot.Assets
import com.futuremangaming.futurebot.mask0
import com.futuremangaming.futurebot.mask1
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

class TrackLoadHandler(val trackRequest: TrackRequest) : AudioLoadResultHandler {

    var allowLive = false
    override fun noMatches() {
        val (_, id, member, channel) = trackRequest
        send(channel, "${member.asMention}, no tracks found for input `$id`!")
    }

    override fun loadFailed(exception: FriendlyException) {
        val (_, id, member, channel) = trackRequest
        send(channel, "${member.asMention}, failed to load track for id `$id`!\n```\n${exception.message}\n```")
        LOG.debug("Loading Track Failed", exception)
    }

    override fun trackLoaded(track: AudioTrack?) {
        if (track === null) return
        val (remote, id, member, channel, message) = trackRequest
        delete(message)
        val info = track.info
        if (info.isStream) {
            if (!allowLive) {
                return send(channel, "${member.asMention}, live streams are not allowed for track requests.")
            }
            else {
                if (remote.scheduler.enqueue(track))
                    send(channel, "Started live sessions for `${info.author}`! [Started by ${member.asMention}]")
                else
                    send(channel, "Failed to start live session for `$id`! Sorry to ${member.asMention} :(")

                return
            }
        }

        val started: Boolean = remote.scheduler.enqueue(track)
        channel.sendEmbedAsync {
            color { Assets.MUSIC_EMBED_COLOR }
            this += "${if (started) "Started playing " else "Loaded "}track **" +
                "[${info.title.mask0()}](${info.uri.mask1()})** [Requested by ${member.asMention}]"
        }
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
        channel.sendTyping() then {

            for (track in playlist.tracks)
                remote.scheduler.enqueue(track)

            send(channel, "Loaded playlist with **${playlist.tracks?.size}** tracks! [Requested by ${member.asMention}]")

        }
    }

}
