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

package com.futuremangaming.futurebot.command

import club.minnced.kjda.entities.sendEmbedAsync
import com.futuremangaming.futurebot.FutureBot
import com.futuremangaming.futurebot.internal.AbstractCommand
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

fun getSocial() = setOf(Merch, Twitter, Youtube, Twitch)

object Merch   : SocialCommand("merch",
    "Find the latest merch at the [shop](https://www.designbyhumans.com/shop/FuturemanGaming/)")
object Twitter : SocialCommand("twitter",
    "Follow Futureman on Twitter [@FuturemanGaming](https://twitter.com/futuremangaming)")
object Youtube : SocialCommand("youtube",
    "Subscribe to Futureman on Youtube: [FuturemanGaming](https://www.youtube.com/channel/UCfW0G1nRUW-r3wz6fWUXA4Q)")
object Twitch  : SocialCommand("twitch",
    "Follow Futureman on Twitch: [twitch.tv/FuturemanGaming](https://twitch.tv/FuturemanGaming)")

open class SocialCommand(name: String, val description: String) : AbstractCommand(name, null) {

    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        event.channel.sendEmbedAsync {
            this += this@SocialCommand.description
            color { 0x0079BF }
        }
    }

}
