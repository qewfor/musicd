/*
 * Copyright 2016 FuturemanGaming
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

package com.futuremangaming.futurebot.hooks;

import com.futuremangaming.futurebot.FutureBot;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.utils.PermissionUtil;

import java.util.regex.Pattern;

public class InviteProtection implements EventListener
{

    private static final Pattern INVITE = Pattern.compile("(?:https?://)?(?:www\\.)?discord(?:app\\.com|\\.gg)/\\S+");

    private final String guildId;
    private final FutureBot bot;

    public InviteProtection(String guildId, FutureBot bot)
    {
        this.guildId = guildId;
        this.bot = bot;
    }

    @Override
    public void onEvent(Event event)
    {
        if (event instanceof GuildMessageReceivedEvent)
            onMessage((GuildMessageReceivedEvent) event);
    }

    private void onMessage(GuildMessageReceivedEvent event)
    {
        if (event.getMessage().isWebhookMessage() || event.getAuthor().isBot())
            return;
        Member member = event.getMember();
        if (!INVITE.matcher(event.getMessage().getRawContent()).find()
                || !event.getGuild().getId().equals(guildId)
                || bot.isAdmin(member) || bot.isMod(member) || bot.isSub(member)
                || !PermissionUtil.checkPermission(event.getChannel(), event.getGuild().getSelfMember(), Permission.MESSAGE_MANAGE))
            return;
        event.getMessage().deleteMessage().queue(
            m -> GuildHook.sendMessage(
                member.getAsMention() + ", sending invites to other discord servers is a subscriber privilege!", event.getChannel()
            ).queue()
        );
    }
}
