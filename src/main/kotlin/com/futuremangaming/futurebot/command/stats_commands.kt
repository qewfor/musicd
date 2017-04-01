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
@file:JvmName("StatCommands")
package com.futuremangaming.futurebot.command

import com.futuremangaming.futurebot.FutureBot
import com.futuremangaming.futurebot.internal.AbstractCommand
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

/**
 * @author Florian Spieß
 * @since  2016-12-31
 */
object Ping : AbstractCommand("ping", "pong")
object Uptime : SupplierCommand("uptime", { timeFormat() })

fun getStats() = setOf(Ping, Uptime)

open class SupplierCommand(override val name: String, val supply: () -> String) : AbstractCommand(name, null) {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        respond(event.channel, supply.invoke())
    }
}

fun timeFormat(time: Long = ManagementFactory.getRuntimeMXBean().uptime): String {
    val unit = TimeUnit.MILLISECONDS
    val days = unit.toDays(time)
    val hours = unit.toHours(time) % 24
    val minutes = (unit.toMinutes(time) % 60) % 24
    val seconds = ((unit.toSeconds(time) % 60) % 60) % 24

    val adjustedD: String? = if (days < 1) null else "**$days** day(s)"
    val adjustedH: String? = if (hours < 1) null else "**$hours** hour(s)"
    val adjustedM: String? = if (minutes < 1) null else "**$minutes** minute(s)"
    val adjustedS: String? = if (seconds < 1) null else "**$seconds** second(s)"

    return arrayOf(adjustedD, adjustedH, adjustedM, adjustedS)
            .filterNotNull()
            .joinToString(", ")
}
