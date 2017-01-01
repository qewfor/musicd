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

import com.futuremangaming.futurebot.FutureBot
import com.futuremangaming.futurebot.command.getAdmin
import com.futuremangaming.futurebot.command.getStats
import com.futuremangaming.futurebot.getLogger
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.hooks.EventListener
import org.apache.commons.lang3.exception.ExceptionUtils

/**
 * @author Florian Spieß
 * @since  2016-12-30
 */

interface Command {

    val name: String

    fun onCommand(args: String, event: GuildMessageReceivedEvent, bot: FutureBot)

}

abstract class AbstractCommand(override val name: String, val response: String? = null) : Command {

    companion object {
        val LOG = getLogger("CommandSystem")
    }

    val ignoredChannels: Set<TextChannel> = mutableSetOf()

    open fun checkPermission(member: Member): Boolean = true

    open fun checkPermission(channel: TextChannel): Boolean = channel.canTalk()

    open fun checkIgnored(channel: TextChannel): Boolean = ignoredChannels.contains(channel)

    fun respond(channel: TextChannel, response: String) {
        try {
            channel.sendMessage(response).queue()
        }
        catch (ex: PermissionException) {
            LOG.debug(ExceptionUtils.getStackTrace(ex))
        }
    }

    /////////////////////

    override final fun onCommand(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        if (checkPermission(event.member).not() || checkPermission(event.channel).not() || checkIgnored(event.channel))
            return
        // override here
        onVerified(args, event, bot)
    }

    open fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        if (response !== null)
            respond(event.channel, response)
    }

}

class CommandManagement(val bot: FutureBot, val prefix: String = "!") : EventListener {

    private val commands: Set<Command> =
                          getStats() + // Stats: Ping, Uptime
                          getAdmin()   // Admin: Eval, Shutdown

    fun onMessage(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return

        val rawC = event.message.rawContent
        val user = event.author
        val channel = event.channel

        if (rawC.startsWith(prefix).not()) return

        val args = rawC.split(Regex("\\s+"), 2)
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
