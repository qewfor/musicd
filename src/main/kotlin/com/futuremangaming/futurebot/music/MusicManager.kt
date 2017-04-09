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
import gnu.trove.TDecorators
import gnu.trove.map.hash.TLongObjectHashMap
import net.dv8tion.jda.core.entities.Guild

class MusicManager {

    private val players: MutableMap<Long, AudioPlayer> = TDecorators.wrap(TLongObjectHashMap<AudioPlayer>())
    private val schedulers: MutableMap<AudioPlayer, TrackScheduler> = hashMapOf()

    fun resetPlayer(guild: Guild) {
        getPlayer(guild).destroy()
        schedulers.remove(players.remove(guild.idLong))
    }

    fun getPlayer(guild: Guild): AudioPlayer {
        return players.getOrPut(guild.idLong) {
            val player = MusicModule.PLAYER_MANAGER.createPlayer()
            val scheduler = schedulers.getOrPut(player, { TrackScheduler(player, guild, this@MusicManager) })

            guild.audioManager.sendingHandler = PlayerSendHandler(player)
            player.addListener(scheduler)
            player.volume = Integer.decode(System.getProperty("app.music.volume", "50"))
            return@getOrPut player
        }
    }

    fun getScheduler(guild: Guild): TrackScheduler = schedulers[getPlayer(guild)]!!

}
