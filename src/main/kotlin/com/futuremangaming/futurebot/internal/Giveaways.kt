package com.futuremangaming.futurebot.internal

import com.futuremangaming.futurebot.Permissions
import com.futuremangaming.futurebot.getLogger
import com.futuremangaming.futurebot.internal.giveaways.Giveaway
import gnu.trove.map.hash.TLongObjectHashMap
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel

object Giveaways {

    private val MAP = TLongObjectHashMap<Giveaway>()
    private val ENTER_EMOJI_KEY = "giveaway.enter"
    private val CLOSE_EMOJI_KEY = "giveaway.close"

    val ENTER_EMOJI: String get() = System.getProperty(ENTER_EMOJI_KEY) ?: "📩"
    val CLOSE_EMOJI: String get() = System.getProperty(CLOSE_EMOJI_KEY) ?: "🔒"

    val LOG = getLogger("Giveaway")

    fun giveFor(channel: TextChannel): Giveaway {
        if (!MAP.containsKey(channel.idLong))
            MAP.put(channel.idLong, Giveaway(channel))
        return MAP[channel.idLong]
    }

    fun canClose(mem: Member)
        = Permissions.isModerator(mem)

}
