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

import club.minnced.kjda.entities.sendTextAsync
import com.futuremangaming.futurebot.FutureBot
import com.futuremangaming.futurebot.getLogger
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import org.apache.commons.lang3.exception.ExceptionUtils

abstract class AbstractCommand(override val name: String, val response: String? = null) : Command {

    companion object {
        val LOG = getLogger(CommandManagement::class.java)
    }

    val ignoredChannels: Set<TextChannel> = mutableSetOf()

    open fun checkPermission(member: Member): Boolean = true

    open fun checkPermission(channel: TextChannel): Boolean = channel.canTalk()

    open fun checkIgnored(channel: TextChannel): Boolean = ignoredChannels.contains(channel)

    fun respond(channel: TextChannel, response: String) {
        try {
            channel.sendTextAsync { response } catch { }
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
