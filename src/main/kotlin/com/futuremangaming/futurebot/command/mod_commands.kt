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

import Giveaways
import club.minnced.kjda.entities.editAsync
import club.minnced.kjda.entities.sendTextAsync
import club.minnced.kjda.plusAssign
import club.minnced.kjda.then
import com.futuremangaming.futurebot.FutureBot
import com.futuremangaming.futurebot.Permissions
import com.futuremangaming.futurebot.internal.AbstractCommand
import com.futuremangaming.futurebot.music.delete
import net.dv8tion.jda.core.Permission.MANAGE_PERMISSIONS
import net.dv8tion.jda.core.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

fun getMods() = setOf<ModCommand>(LockCommand, PruneCommand, GiveawayCommand, DrawCommand)

/** Locks the current TextChannel down. */
object LockCommand : ModCommand("lock") {

    override fun checkPermission(channel: TextChannel): Boolean {
        val canTalk = channel.canTalk()
        val canPerm = channel.guild.selfMember.hasPermission(channel, MANAGE_PERMISSIONS)

        if (!canPerm && canTalk) channel.sendTextAsync {
            "I am unable to lock this channel. Please allow me to manage permissions for this channel."
        }

        return canPerm
    }
}

/** Clears `X` messages */
object PruneCommand : ModCommand("prune") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        event.channel.history.retrievePast(100) then {
            val messages = this?.filter { ChronoUnit.WEEKS.between(it.creationTime, OffsetDateTime.now()) < 2 }!!
            if (messages.size > 1)
                event.channel.deleteMessages(messages).queue()
            else
                delete(messages.first())
            respond(event.channel, "${event.author.asMention} pruned **${messages.size} message(s)** in this channel.")
        }
    }

    override fun checkPermission(channel: TextChannel): Boolean {
        val canTalk = channel.canTalk()
        val canPerm = channel.guild.selfMember.hasPermission(channel, MESSAGE_MANAGE)

        if (!canPerm && canTalk) channel.sendTextAsync {
            "I am unable to prune messages in this channel. Please allow me to manage messages for this channel."
        }

        return canPerm
    }
}

object GiveawayCommand : ModCommand("giveaway") {

    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {

        val ga = Giveaways.giveFor(event.channel)
        val prize: String
        val sub: Boolean
        if (args.startsWith("-s ")) {
            prize = args.substring("-s ".length)
            sub = true
        }
        else {
            prize = args
            sub = false
        }

        val msg = ga.open(enter = "📩", close = "🔒", sub = sub)
        msg.editAsync {
            this += "Giving away **$prize**! React with 📩 to join and 🔒 to close!"
        } then { delete(event.message) }

    }

}

object DrawCommand : ModCommand("draw") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {

        val ga = Giveaways.giveFor(event.channel)
        if (ga.entrySize() < 1) return respond(event.channel, "Not enough people entered to draw a winner!")

        val winner = ga.pollWinner()
        respond(event.channel, "Aaand the winner is ${event.jda.getUserById(winner).asMention}! 🎊 Congratulations 🎊")

    }
}

abstract class ModCommand(name: String) : AbstractCommand(name) {
    override fun checkPermission(member: Member) = Permissions.isModerator(member)
}