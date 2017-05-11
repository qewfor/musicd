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

import club.minnced.kjda.embed
import club.minnced.kjda.entities.editAsync
import club.minnced.kjda.entities.sendTextAsync
import club.minnced.kjda.get
import club.minnced.kjda.plusAssign
import com.futuremangaming.futurebot.Assets
import com.futuremangaming.futurebot.FutureBot
import com.futuremangaming.futurebot.Permissions
import com.futuremangaming.futurebot.getLogger
import com.futuremangaming.futurebot.internal.AbstractCommand
import com.futuremangaming.futurebot.internal.CommandGroup
import com.futuremangaming.futurebot.internal.giveaways.Giveaways
import com.futuremangaming.futurebot.music.delete
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors

fun getMods() = setOf(PruneCommand, GiveawayCommand, DrawCommand)

/** Clears `X` messages */
object PruneCommand : ModCommand("prune") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) = start {
        val reason = "Prune by %#s".format(event.author)
        val channel = event.channel
        val messages =
            channel.iterableHistory.get()!!.stream()
                .limit(100)
                .filter { ChronoUnit.WEEKS.between(it.creationTime, OffsetDateTime.now()) < 2 }
                .collect(Collectors.toList<Message>())

        if (messages.size > 1)
            channel.deleteMessages(messages).reason(reason).queue()
        else
            messages.first().delete(reason)

        respond(channel, "%s pruned **%d message(s)** in this channel.".format(event.author, messages.size))
        super.onVerified(args, event, bot)
    }

    private fun start(block: suspend CoroutineScope.() -> Unit) {
        launch(CommonPool, block = block)
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

        val author = event.author
        val message = event.message
        val enter = Giveaways.ENTER_EMOJI
        val close = Giveaways.CLOSE_EMOJI
        val msg = ga.open(enter = enter, close = close, sub = sub)
        msg.editAsync {
            this += "Giving away **$prize**! React with $enter to join and $close to close!"
            if (sub) embed {
                this += "👉🏽 This giveaway is only for subscribers!"
                color { Assets.MUSIC_EMBED_COLOR }
            }
        } then { message.delete("Giveaway started by %#s", author) }
        super.onVerified(args, event, bot)
    }

}

object DrawCommand : ModCommand("draw") {
    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        val channel = event.channel
        val ga = Giveaways.giveFor(channel)
        if (ga.entrySize() < 1) return respond(channel, "Not enough people entered to draw a winner!")

        val winner = ga.pollWinner()
        respond(channel, "Aaand the winner is ${event.jda.getUserById(winner).asMention}! 🎊 Congratulations 🎊")
        super.onVerified(args, event, bot)
    }
}

abstract class ModCommand(name: String) : AbstractCommand(name) {

    companion object {
        val LOG = getLogger("Moderation")
    }

    override val group = CommandGroup("Moderation", "mod")

    override fun onVerified(args: String, event: GuildMessageReceivedEvent, bot: FutureBot) {
        LOG.info("%#s used %s in %#s".format(event.author, name, event.channel))
    }

    override fun checkPermission(member: Member) = Permissions.isModerator(member)
}
