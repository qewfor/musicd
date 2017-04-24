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

package com.futuremangaming.futurebot.command.help

import club.minnced.kjda.builders.KEmbedBuilder
import club.minnced.kjda.builders.embed
import club.minnced.kjda.then
import com.futuremangaming.futurebot.Assets
import com.futuremangaming.futurebot.internal.CommandManagement
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.requests.RestAction
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class HelpView(channel: TextChannel) {

    val message = AtomicLong(0)
    val channelId = channel.idLong
    val api: JDA = channel.jda
    val channel: TextChannel? get() = api.getTextChannelById(channelId)

    val currentPage = AtomicInteger(-1)

    fun showLast(management: CommandManagement)
        = show(management, management.groups.size - 1)
    fun showFirst(management: CommandManagement)
        = show(management, -1)

    fun showPrev(management: CommandManagement) {
        if (currentPage.get() == -1)
            showLast(management)
        else
            show(management, currentPage.decrementAndGet())
    }

    fun showNext(management: CommandManagement) {
        if (currentPage.get() == management.groups.size - 1)
            showFirst(management)
        else
            show(management, currentPage.incrementAndGet())
    }

    inline fun show(management: CommandManagement, matching: () -> String) {
        val name = matching().toLowerCase()
        var target = -1
        var page = -1
        management.groups.forEach {
            page++
            val key = it.key
            if (key.shortName.toLowerCase() == name || key.longName.toLowerCase() == name)
                target = page
        }
        show(management, target, true)
    }

    fun show(management: CommandManagement, page: Int = -1, new: Boolean = false) {
        currentPage.set(page)
        val groups = management.groups
        val block: AtomicReference<(KEmbedBuilder.() -> Unit)?> = AtomicReference()
        if (page < 0) {
            block.set {
                title { "Command Index" }

                description {
                    "Use pagination reactions or specify a page using the short-name!\n" +
                    "The `short-name` is located in the parenthesis after the name: **Moderation (__mod__)**"
                }

                color {
                    Assets.MUSIC_EMBED_COLOR
                }

                this += "\n"
                for ((group, _) in groups) {
                    this += "\n${group.longName} (`${group.shortName}`)"
                }
            }
        }
        else {
            var i = 0
            val prefix = management.prefix
            groups.forEach {
                val (key, value) = it
                if (i++ == page) {
                    block.set {
                        title {
                            "${key.longName} (${key.shortName})"
                        }
                        color { Assets.MUSIC_EMBED_COLOR }

                        this += value.joinToString("\n") { "-> `$prefix${it.name}`" }
                    }
                }
            }
        }

        if (block.get() === null) {
            show(management, -1)
        }
        else {
            val action: RestAction<Message>?
            if (new) {
                action = channel?.sendMessage(embed(block.get() as KEmbedBuilder.() -> Unit))
            } else {
                action = channel?.editMessageById(message.get(), embed(((block.get()) as KEmbedBuilder.() -> Unit)))
            }

            action?.queue { msg ->
                if (msg !== null)
                    addReactions(msg, isLast = page == groups.size - 1, isFirst = page == -1)
            }
        }
    }

    fun destroy() = true

    fun addReactions(msg: Message, isLast: Boolean, isFirst: Boolean) {
        if (msg.reactions.isNotEmpty() && msg.guild.selfMember.hasPermission(msg.textChannel, MESSAGE_MANAGE))
            msg.clearReactions().complete() // Clear messages before adding pagination

        message.set(msg.idLong)
        if (!isFirst) msg.addReaction(Assets.PAGES_FIRST) then {
            msg.addReaction(Assets.PAGES_PREV) then {
                if (!isLast) {
                    msg.addReaction(Assets.PAGES_NEXT).complete()
                    msg.addReaction(Assets.PAGES_LAST).queue()
                }
            }
        }
        else msg.addReaction(Assets.PAGES_NEXT) then {
            msg.addReaction(Assets.PAGES_LAST).queue()
        }
    }

}
