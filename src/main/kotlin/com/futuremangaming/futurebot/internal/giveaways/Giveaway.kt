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

import club.minnced.kjda.embed
import club.minnced.kjda.entities.editAsync
import club.minnced.kjda.entities.sendText
import club.minnced.kjda.plusAssign
import club.minnced.kjda.then
import com.futuremangaming.futurebot.internal.Giveaways
import gnu.trove.list.array.TLongArrayList
import net.dv8tion.jda.core.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import java.security.SecureRandom

class Giveaway(val channel: TextChannel) { // actual giveaway

    private val participants = TLongArrayList()
    internal var join: String? = null
    internal var end: String? = null
    internal var sub: Boolean = false
    private val random = SecureRandom()
    private val adapter = GiveawayAdapter(this)

    var message: Long = -1

    fun open(enter: String, close: String, sub: Boolean = false): Message {
        if (join !== null) close()

        join = enter
        end = close
        this.sub = sub

        participants.clear()

        channel.jda.addEventListener(adapter)

        val message = channel.sendText { "Starting Giveaway..." }
        this.message = message.idLong
        message.addReaction(enter).complete()
        message.addReaction(close).complete()
        if (channel.guild.selfMember.hasPermission(channel, MESSAGE_MANAGE))
            message.pin().queue()
        return message
    }

    fun close(entries: Int = entrySize()) {
        if (join === null) return
        join = null

        channel.jda.removeEventListener(adapter)
        channel.getMessageById(message) then {
            this?.editAsync {
                this += "🔒 Giveaway is closed 🔒"
                embed {
                    this += "Type `!draw` to poll the next winner! [Entries: $entries]"
                    color { 0x50aace }
                }
            }

            if (channel.guild.selfMember.hasPermission(channel, MESSAGE_MANAGE))
                this?.unpin()?.queue()
        }
    }

    fun reset(message: Long) {
        participants.clear()

        if (join !== null)
            channel.addReactionById(message, join).queue()
        channel.addReactionById(message, end).queue()
        Giveaways.LOG.info("Giveaway in #${channel.name} was reset!")
    }

    fun enter(id: Long) {
        participants.add(id)
    }

    fun exit(id: Long) {
        participants.remove(id)
    }

    fun entrySize() = participants.size()

    fun pollWinner(): Long {
        close()

        val rng = random.nextInt(participants.size())
        val winner = participants[rng]
        participants.remove(winner)

        return winner
    }

}
