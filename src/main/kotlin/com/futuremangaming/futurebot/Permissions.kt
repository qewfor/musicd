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

import net.dv8tion.jda.core.Permission.ADMINISTRATOR
import net.dv8tion.jda.core.Permission.BAN_MEMBERS
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role


object Permissions {

    val ADMIN_ROLE_KEY = "role.admin"
    val MOD_ROLE_KEY = "role.mod"
    val SUB_ROLE_KEY = "role.sub"

    val TWITCH_USER_KEY = "twitch.user"

    val ADMIN_ROLE: String get() = System.getProperty(ADMIN_ROLE_KEY) ?: "-1"
    val MOD_ROLE: String get() = System.getProperty(MOD_ROLE_KEY) ?: "-1"
    val SUB_ROLE: String get() = System.getProperty(SUB_ROLE_KEY) ?: "-1"
    val TWITCH_USER: String get() =  System.getProperty(TWITCH_USER_KEY) ?: "-1"

    fun isSubscriber(member: Member): Boolean {
        val subRole = member.guild.getRoleById(SUB_ROLE)
        return hasRole(member, subRole) || isModerator(member)
    }

    fun isModerator(member: Member): Boolean {
        if (isAdmin(member) || member.hasPermission(BAN_MEMBERS))
            return true

        val modRole = member.guild.getRoleById(MOD_ROLE)
        return hasRole(member, modRole)
    }

    fun isAdmin(member: Member): Boolean {
        if (isOwner(member) || member.hasPermission(ADMINISTRATOR))
            return true
        val adminRole = member.guild.getRoleById(ADMIN_ROLE)
        return hasRole(member, adminRole)
    }

    fun isOwner(member: Member)
            = member.isOwner

    private fun hasRole(member: Member, role: Role?)
        = member.roles.firstOrNull()?.position ?: -1 >= role?.position ?: Int.MAX_VALUE

    fun isTwitch(member: Member) = member.user.id == TWITCH_USER

}
