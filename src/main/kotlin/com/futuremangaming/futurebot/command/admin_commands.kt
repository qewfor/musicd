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

import com.futuremangaming.futurebot.FutureBot
import com.futuremangaming.futurebot.internal.AbstractCommand
import com.futuremangaming.futurebot.internal.Command
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

/**
 * @author Florian Spieß
 * @since  2016-12-31
 */

val owner = "86699011792191488" // 86699011792191488

fun getAdmin() = setOf<Command>(Eval(), Shutdown())

class Eval : AdminCommand("eval") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        val engine = ScriptEngineManager().getEngineByName("nashorn")
        engine["event"] = event
        engine["args"]  = args
        engine["bot"]   = bot

        engine["api"]     = event.jda
        engine["author"]  = event.author
        engine["channel"] = event.channel
        engine["guild"]   = event.guild
        engine["me"]      = event.jda.selfUser
        var o: Any

        try {
            o = engine.eval(args) ?: "null"
        }
        catch (ex: Throwable) {
            o = ex.cause ?: ex
        }

        respond(event.channel, o.toString())
    }

    operator fun ScriptEngine.set(key: String, value: Any) {
        put(key, value)
    }
}

class Shutdown : AdminCommand("shutdown") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        FutureBot.LOG.info("Admin issued shutdown... (${event.author.id})")
        event.jda.shutdownNow(true)
    }
}

open class AdminCommand(override val name: String) : AbstractCommand(name) {

    override fun checkPermission(member: Member): Boolean =
            super.checkPermission(member) && member.user.id == owner

}