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

package com.futuremangaming.futurebot.internal

import club.minnced.kjda.entities.div
import com.futuremangaming.futurebot.FutureBot
import com.futuremangaming.futurebot.command.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.EventListener

class CommandManagement(val bot: FutureBot) : EventListener {

    val prefix: String get() = System.getProperty("bot.prefix", "!")

    private val commands: Set<Command> =
                          getMusic()  + // Music : Play, Skip, Queue, Shuffle
                          getStats()  + // Stats : Ping, Uptime
                          getAdmin()  + // Admin : Eval, Shutdown, Settings
                          getMods()   + // Mods  : Prune, Lock, Unlock, Giveaway, Draw
                          getSocial()   // Social: Merch, Twitter, Youtube, Twitch

    fun onMessage(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return

        val rawC = event.message.rawContent
        //val user = event.author
        //val channel = event.channel

        if (rawC.startsWith(prefix).not()) return

        val args = event.message / 2
        val name = args[0].substring(prefix.length).toLowerCase()
        val cArgs = if (args.size > 1) args[1] else ""
        commands.filter { name == it.name }
                .forEach { it.onCommand(cArgs, event, bot) }
    }

    override fun onEvent(event: Event?) {
        if (event is GuildMessageReceivedEvent)
            onMessage(event)
    }

}
