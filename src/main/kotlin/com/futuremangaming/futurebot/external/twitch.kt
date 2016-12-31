/*
 *     Copyright 2016 FuturemanGaming
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

package com.futuremangaming.futurebot.external

import com.futuremangaming.futurebot.FutureBot
import net.dv8tion.jda.core.entities.Game.GameType.TWITCH
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.user.UserGameUpdateEvent
import net.dv8tion.jda.core.hooks.EventListener

/**
 * @author Florian Spieß
 * @since 2016-12-31
 */

class LiveListener : EventListener {

    override fun onEvent(event: Event?) {
        if (event is UserGameUpdateEvent) {
            val user = event.user
            val member = event.guild.getMember(user)
            if (user.id != "95559929384927232")
                return
            if (member.game.type === TWITCH && event.previousGame?.type !== TWITCH) {
                event.jda.presence.game = member.game
                announce("**Futureman** just went live!", event.guild.publicChannel)
            }
        }
    }
}

fun announce(message: String, channel: TextChannel) {
    try {
        channel.sendMessage(message).queue()
    }
    catch (ex: Exception) {
        FutureBot.LOG.internal(ex.toString())
    }
}
