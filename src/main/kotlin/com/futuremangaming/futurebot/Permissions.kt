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

package com.futuremangaming.futurebot

import net.dv8tion.jda.core.Permission.BAN_MEMBERS
import net.dv8tion.jda.core.entities.Member


object Permissions {

    val MOD_ROLE_KEY = "role.mod"
    val SUB_ROLE_KEY = "role.sub"

    val TWITCH_USER_KEY = "twitch.user"

    val MOD_ROLE = { System.getProperty(MOD_ROLE_KEY) ?: "-1" }
    val SUB_ROLE = { System.getProperty(SUB_ROLE_KEY) ?: "-1" }
    val TWITCH_USER = { System.getProperty(TWITCH_USER_KEY) ?: "-1" }

    fun isModerator(member: Member): Boolean {
        val hasBan = member.hasPermission(BAN_MEMBERS)
        val hasRole = member.roles.any { it.id == MOD_ROLE() }
        val hasOwner = member.isOwner

        return hasOwner || hasBan || hasRole
    }

    fun isSubscriber(member: Member): Boolean {
        val hasRole = member.roles.any { it.id == SUB_ROLE() }
        return hasRole || isModerator(member)
    }

    fun isTwitch(member: Member) = member.user.id == TWITCH_USER()

}