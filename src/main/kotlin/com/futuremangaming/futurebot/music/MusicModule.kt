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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import gnu.trove.map.hash.TLongObjectHashMap
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel

class MusicModule {

    companion object {
        internal val PLAYER_MANAGER: AudioPlayerManager

        init {
            PLAYER_MANAGER = DefaultAudioPlayerManager()
            AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER)
        }

    }

    val manager: MusicManager = MusicManager()
    val remotes = TLongObjectHashMap<PlayerRemote>()

    fun remote(guild: Guild): PlayerRemote = if (remotes.containsKey(guild.idLong)) {
        remotes.get(guild.idLong)
    }
    else {
        val player = manager.getPlayer(guild)
        val queue = manager.getScheduler(guild)
        remotes.put(guild.idLong, PlayerRemote(player, queue, this, guild.idLong))
        remote(guild) // safe recursion
    }

    fun remote(guild: Guild, voiceChannel: VoiceChannel): PlayerRemote {
        val remote = remote(guild)
        remote.voice = voiceChannel
        return remote
    }
}
