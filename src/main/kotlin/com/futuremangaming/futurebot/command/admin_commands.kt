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
@file:JvmName("AdminCommands")
package com.futuremangaming.futurebot.command

import club.minnced.kjda.div
import club.minnced.kjda.entities.sendEmbedAsync
import com.futuremangaming.futurebot.Assets
import com.futuremangaming.futurebot.FutureBot
import com.futuremangaming.futurebot.Permissions
import com.futuremangaming.futurebot.internal.AbstractCommand
import com.futuremangaming.futurebot.internal.Command
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.util.Properties
import java.util.TreeMap
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

fun getAdmin() = setOf<Command>(Eval, Shutdown, Settings, Assetings)

object Settings : AdminCommand("set") {

    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        if (StringUtils.containsAny(args.toLowerCase(), "--list", "-l")) {
            val props = Properties()
            val reader = File("default.properties").reader()
            props.load(reader)
            reader.close()

            val mutableMap = TreeMap<String, String>()
            for (current in props.keys) {
                mutableMap[current as String] = System.getProperty(current) ?: "T/D"
            }

            event.channel.sendEmbedAsync {
                var longestKey = 0
                var longestVal = 0
                mutableMap.forEach { t, u ->
                    if (longestKey < t.length) longestKey = t.length
                    if (longestVal < u.length) longestVal = u.length
                }
                val list = mutableMap.map { entry ->
                    val (key, value) = entry
                    String.format("%-${longestKey}s: %${longestVal}s", key, value)
                }

                this += "```ldif\n"
                this += list.joinToString("\n")
                this += "```"
            }
            return
        }

        if (!args.contains(" ")) {
            if (!args.isEmpty() && System.getProperty(args) !== null) {
                System.getProperties().remove(args)
                return respond(event.channel, "Removed Property `$args`!")
            }
            else {
                return respond(event.channel, "No such property `$args`!")
            }
        }

        val (key, value) = args / 2
        val old = System.setProperty(key, value)
        if (old === null)
            respond(event.channel, "Set property `$key` to `$value`")
        else
            respond(event.channel, "Changed property `$key` from `$old` to `$value`")
    }
}

object Assetings : AdminCommand("asset") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        if (StringUtils.containsAny(args.toLowerCase(), "--list", "-l")) {
            val props = Assets.all

            val mutableMap = TreeMap<String, String>()
            for (current in props.keys) {
                mutableMap[current] = System.getProperty(current) ?: "T/D"
            }

            event.channel.sendEmbedAsync {
                var longestKey = 0
                var longestVal = 0
                mutableMap.forEach { t, u ->
                    if (longestKey < t.length) longestKey = t.length
                    if (longestVal < u.length) longestVal = u.length
                }
                val list = mutableMap.map { entry ->
                    val (key, value) = entry
                    String.format("%-${longestKey}s: %${longestVal}s", key, value)
                }

                this += "```ldif\n"
                this += list.joinToString("\n")
                this += "```"
            }
            return
        }

        if (!args.contains(" ")) {
            if (!args.isEmpty() && System.getProperty(args) !== null) {
                System.getProperties().remove(args)
                return Settings.respond(event.channel, "Removed Property `$args`!")
            }
            else {
                return Settings.respond(event.channel, "No such property `$args`!")
            }
        }

        val (key, value) = args / 2
        val old = System.setProperty(key, value)
        if (old === null)
            Settings.respond(event.channel, "Set property `$key` to `$value`")
        else
            Settings.respond(event.channel, "Changed property `$key` from `$old` to `$value`")
    }
}

object Eval : AdminCommand("eval") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        val engine = ScriptEngineManager().getEngineByName("nashorn")
        engine["event"] = event
        engine["args"]  = args
        engine["bot"]   = bot

        val (api, author, channel, guild, me, message) = event
        engine["api"]     = api
        engine["author"]  = author
        engine["channel"] = channel
        engine["guild"]   = guild
        engine["me"]      = me
        engine["message"] = message
        var o: Any

        try {
            o = engine.eval(args).toString()
        }
        catch (ex: Throwable) {
            o = ex.cause ?: ex
        }

        respond(event.channel, o.toString())
    }

    //Used for binding via engine[key] = value
    operator fun ScriptEngine.set(key: String, value: Any) {
        put(key, value)
    }

    //I generate these receiver extensions to destruct
    //the GuildMessageReceiveEvent for the engine to bind
    operator fun GuildMessageReceivedEvent.component1() = this.jda
    operator fun GuildMessageReceivedEvent.component2() = this.author
    operator fun GuildMessageReceivedEvent.component3() = this.channel
    operator fun GuildMessageReceivedEvent.component4() = this.guild
    operator fun GuildMessageReceivedEvent.component5() = this.jda.selfUser
    operator fun GuildMessageReceivedEvent.component6() = this.message
}

object Shutdown : AdminCommand("shutdown") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        FutureBot.LOG info "Admin issued shutdown... (${event.author.id})"
        event.jda.shutdown(true)
    }
}

open class AdminCommand(override val name: String) : AbstractCommand(name) {
    override fun checkPermission(member: Member) = member.user.idLong == Permissions.BOT_OWNER || member.isOwner
}
