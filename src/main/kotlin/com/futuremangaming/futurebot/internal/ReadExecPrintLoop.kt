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
import com.futuremangaming.futurebot.set
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.EventListener
import java.io.Writer
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException


class ReadExecPrintLoop(
    val channelId: Long, val userId: Long,
    val api: JDA, val bot: FutureBot) : EventListener {
    val engine: ScriptEngine by lazy {
        val e = ScriptEngineManager().getEngineByExtension("js")
        e.context.writer = ChannelWriter(api, channelId)
        e.context.errorWriter = ChannelWriter(api, channelId, true)
        e["bot"] = bot
        e["api"] = api
        e["writer"] = e.context.writer
        e.eval("""
            imports = JavaImporter(java.lang, java.util, java.io, java.nio,
                 Packages.net.dv8tion.jda.core,
                 Packages.net.dv8tion.jda.core.utils,
                 Packages.net.dv8tion.jda.core.entities,
                 Packages.net.dv8tion.jda.core.requests,
                 Packages.net.dv8tion.jda.core.managers)
            function log(s) {
                if (s == null)
                    return;
                writer.append(s.toString());
                writer.flush();
            }
        """)
        return@lazy e
    }

    init {
        api.addEventListener(this)
    }

    override fun onEvent(event: Event) {
        if (event is GuildMessageReceivedEvent) {
            if (event.channel.idLong == channelId && event.author.idLong == userId)
                onMessage(event)
        }
    }

    fun onMessage(event: GuildMessageReceivedEvent) {
        val content = event.message.rawContent
        if (content.isNullOrEmpty()) return
        if (!content.startsWith("```")) return

        var code = content.substring(3, content.length - 3).trim()

        if (code.startsWith("js", true))
            code = code.substring(2).trim()

        if (code.equals("exit", true)) {
            event.message.addReaction("👌🏻").queue()
            return api.removeEventListener(this)
        }

        engine["event"] = event
        engine["channel"] = event.channel
        engine["guild"] = event.guild
        engine["message"] = event.message
        engine["self"] = event.jda.selfUser
        engine["author"] = event.author

        try {
            engine.eval(code)
        }
        catch (ex: Exception) {
            if (ex !is ScriptException)
                ex.printStackTrace()
            else {
                val writer = engine.context.errorWriter
                writer.append(ex.cause.toString())
                writer.flush()
            }
        }
    }
}

class ChannelWriter(
    val api: JDA, val channelId: Long, val error: Boolean = false) : Writer() {

    companion object {
        const val ERROR_2000 = "```diff\n- Output too large. See console.```"
    }
    var stringBuilder = StringBuilder()

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        stringBuilder.append(String(cbuf, off, len))
        if (stringBuilder.length > 1000)
            flush()
    }

    override fun flush() {
        if (stringBuilder.isEmpty()) return
        val msgBuilder = newBuilder()
        val string = stringBuilder.toString()
        val channel = api.getTextChannelById(channelId)
        stringBuilder = StringBuilder()

        if (string.length > 2000) {
            println("\nEVAL OUTPUT\n")
            println(string)
            println("\n=======\n")

            return channel?.sendMessage(ERROR_2000)?.queue() ?: Unit
        }

        msgBuilder.append(string).append("```")
        channel?.sendMessage(msgBuilder.build())?.queue()
    }

    override fun close() { }

    fun newBuilder(): MessageBuilder {
        val builder = MessageBuilder().append("```")
        if (error)
            builder.append("diff\n- Error -\n")
        return builder
    }

}
