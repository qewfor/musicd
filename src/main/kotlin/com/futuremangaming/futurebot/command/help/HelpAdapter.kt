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

import club.minnced.kjda.div
import club.minnced.kjda.entities.isSelf
import com.futuremangaming.futurebot.Assets
import com.futuremangaming.futurebot.FutureBot
import com.futuremangaming.futurebot.internal.Command
import com.futuremangaming.futurebot.internal.CommandGroup
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class HelpAdapter(val helpers: Helpers) : ListenerAdapter(), Command {
    override val name = "help"
    override val group = CommandGroup("Information", "info")

    override fun onCommand(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        if (args.isEmpty())
            return helpers.display(event.channel)
        else return helpers.display(event.channel) { (args / 2)[0] }
    }

    override fun onGenericGuildMessageReaction(event: GenericGuildMessageReactionEvent) {
        val user = event.user
        val react = event.reactionEmote
        val message = event.messageIdLong
        val channel = event.channel.idLong

        if (user.isSelf || user.isBot) return
        if (!helpers.helpers.containsKey(channel)) return

        val helper = helpers.helpers[channel]
        if (helper.message.get() == message) when (react.name) {
            Assets.PAGES_FIRST -> helper.showFirst(helpers.management)
            Assets.PAGES_PREV  -> helper.showPrev(helpers.management)
            Assets.PAGES_NEXT  -> helper.showNext(helpers.management)
            Assets.PAGES_LAST  -> helper.showLast(helpers.management)
        }
    }

}
