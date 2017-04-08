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
@file:JvmName("Music")
package com.futuremangaming.futurebot.music

import club.minnced.kjda.entities.sendTextAsync
import com.futuremangaming.futurebot.getLogger
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.exceptions.PermissionException

val LOG = getLogger("MusicModule")

data class TrackRequest(
    val manager: PlayerRemote,
    val id: String,
    val member: Member,
    val channel: TextChannel,
    val message: Message
)

fun delete(message: Message) {
    try {
        message.delete().queue()
    }
    catch (ex: PermissionException) { }
}

fun send(channel: MessageChannel, msg: String) {
    try {
        channel.sendTextAsync { msg } catch { }
    }
    catch (ex: PermissionException) { }
    catch (ex: Exception) {
        LOG.log(ex)
    }
}
