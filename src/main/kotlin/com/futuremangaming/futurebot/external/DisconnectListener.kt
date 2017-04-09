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

package com.futuremangaming.futurebot.external

import com.futuremangaming.futurebot.getLogger
import net.dv8tion.jda.core.events.DisconnectEvent
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ExceptionEvent
import net.dv8tion.jda.core.hooks.EventListener

object DisconnectListener : EventListener {

    val LOG = getLogger("Connection")

    override fun onEvent(event: Event): Unit = when (event) {
        is DisconnectEvent -> {
            val client = event.clientCloseFrame
            val code = event.closeCode
            if (code !== null)
                LOG.warn("Disconnected [Server] ${code.code}: ${code.meaning}")
            else if (client !== null)
                LOG.warn("Disconnected [Client] ${client.closeCode}: ${client.closeReason}")
            else LOG.error("Disconnected for unknown reason! ${event.serviceCloseFrame.closeReason}")
            Unit
        }
        is ExceptionEvent -> {
            if (!event.isLogged)
                LOG.warn("Unhandled JDA Exception", event.cause)
            Unit
        }
        else -> { }
    }
}