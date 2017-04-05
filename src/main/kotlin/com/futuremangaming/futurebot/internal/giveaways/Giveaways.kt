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
import com.futuremangaming.futurebot.getLogger
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
