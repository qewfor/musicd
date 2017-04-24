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
import com.futuremangaming.futurebot.command.help.Helpers
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.EventListener

class CommandManagement(val bot: FutureBot, val jda: JDA) : EventListener {

    val prefix: String get() = System.getProperty("bot.prefix", "!")
    val helpers = Helpers(this)

    private val commands: Set<Command> = mutableSetOf(helpers.adapter) +
                          getMusic()  + // Music : Play, Skip, Queue, Shuffle
                          getStats()  + // Stats : Ping, Uptime
                          getAdmin()  + // Admin : Eval, Shutdown, Settings
                          getMods()   + // Mods  : Prune, Lock, Unlock, Giveaway, Draw
                          getSocial()   // Social: Merch, Twitter, Youtube, Twitch

    val groups: Map<CommandGroup, List<Command>> by lazy { groups() }

    fun group(shortName: String)
        = commands.asSequence()
                  .filter { shortName == it.group.shortName }
                  .toList()

    private fun groups() = commands.asSequence().groupBy { it.group }

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
