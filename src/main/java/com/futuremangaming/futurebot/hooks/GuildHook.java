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
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class GuildHook implements EventListener
{

    private String guildId;
    private FutureBot bot;

    public GuildHook(String guildId, FutureBot bot)
    {
        this.guildId = guildId;
        this.bot = bot;
    }

    @Override
    public void onEvent(Event event)
    {
        if (event instanceof GenericGuildMessageEvent)
        {
            if (!((GenericGuildMessageEvent) event).getGuild().getId().equals(guildId))
                return;
            if (event instanceof GuildMessageReceivedEvent)
                onMessage((GuildMessageReceivedEvent) event);
        }
    }

    private void onMessage(GuildMessageReceivedEvent event)
    {
        if (event.getAuthor().isBot())
            return;
        String[] content = event.getMessage().getRawContent().split("\\s+",2);
        String cmd = content[0];
        String args = "";
        if (content.length > 1)
            args = content[1];
        commandExecution(cmd, args, event.getChannel(), event.getMember());
    }

    private void commandExecution(String cmd, String args, TextChannel channel, Member member)
    {
        // will be replaced later
        switch (cmd.toLowerCase())
        {
            case "!ping":
                long time = System.currentTimeMillis();
                channel.sendMessage("Pong!").queue(m -> m.editMessage("**Ping**: " + (System.currentTimeMillis() - time)  + "ms").queue());
                break;
            case "!info":
                channel.sendMessage("Alpha build of FutureBot!").queue();
                break;
            case "!shutdown":
                if (bot.isAdmin(member))
                    channel.sendMessage("Shutting down!").queue(m -> bot.shutdown(true));
                break;
        }
    }

}
