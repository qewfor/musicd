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

import com.futuremangaming.futurebot.internal.CommandManagement
import gnu.trove.map.hash.TLongObjectHashMap
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.TextChannel


class Helpers(val management: CommandManagement) {

    internal val helpers = TLongObjectHashMap<HelpView>()
    val adapter = HelpAdapter(this)

    fun register(api: JDA) {
        if (adapter !in api.registeredListeners)
            api.addEventListener(adapter)
    }

    fun shutdown(api: JDA) {
        api.removeEventListener(adapter)
        helpers.forEachValue(HelpView::destroy)
    }

    fun display(channel: TextChannel, matching: () -> String) {
        val helper = of(channel)
        helper.show(management, matching)
    }

    fun display(channel: TextChannel) {
        val helper = of(channel)
        helper.show(management, new = true)
    }

    fun of(channel: TextChannel): HelpView {
        if (channel.idLong !in helpers)
            helpers.put(channel.idLong, HelpView(channel))
        return helpers[channel.idLong]!!
    }

}
