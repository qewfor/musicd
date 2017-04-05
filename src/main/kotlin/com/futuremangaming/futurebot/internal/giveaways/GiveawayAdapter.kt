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

package com.futuremangaming.futurebot.internal.giveaways

import com.futuremangaming.futurebot.Permissions
import com.futuremangaming.futurebot.internal.giveaways.Giveaways
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class GiveawayAdapter(val giveaway: Giveaway) : ListenerAdapter() {

    override fun onGenericMessageReaction(event: GenericMessageReactionEvent?) {
        super.onGenericMessageReaction(event)
    }

    override fun onMessageReactionRemoveAll(event: MessageReactionRemoveAllEvent) {
        val id = event.messageId.toLong()
        if (id != giveaway.message) return

        giveaway.reset(id)
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.user == event.jda.selfUser) return
        val id = event.user.idLong
        val messageId = event.messageId.toLong()
        val react = event.reaction.emote.id ?: event.reaction.emote.name

        val channel = event.channel as? TextChannel ?: return
        val guild = channel.guild
        val member = guild.getMember(event.user)

        if (messageId != giveaway.message) return
        if (react != giveaway.join) {
            if (react == giveaway.end && Giveaways.canClose(member))
                giveaway.close()
            return
        }

        if (giveaway.sub && !Permissions.isSubscriber(member)) return

        giveaway.enter(id)
    }

    override fun onMessageReactionRemove(event: MessageReactionRemoveEvent) {
        if (event.user == event.jda.selfUser) return
        val id = event.user.idLong
        val messageId = event.messageId.toLong()

        if (messageId != giveaway.message) return
        if (event.reaction.emote.id ?: event.reaction.emote.name != giveaway.join) return

        giveaway.exit(id)
    }
}
