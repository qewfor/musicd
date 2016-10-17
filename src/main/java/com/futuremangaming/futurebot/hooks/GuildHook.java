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
import com.futuremangaming.futurebot.commands.Command;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.requests.RestAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class GuildHook implements EventListener
{

    private List<Command> commands = new ArrayList<>();

    private String guildId;
    private FutureBot bot;

    public GuildHook(String guildId, FutureBot bot)
    {
        this.guildId = guildId;
        this.bot = bot;
    }

    public void registerCommand(Command... commands)
    {
        Collections.addAll(this.commands, commands);
    }

    public Command find(String alias)
    {
        for (Command c : commands)
            if (c.getAlias().equalsIgnoreCase(alias))
                return c;
        return null;
    }

    public boolean removeCommandIf(Predicate<Command> predicate)
    {
        return commands.removeIf(predicate);
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
        Command triggered = commandExecution(cmd, args, event);
        /*if (triggered != null)
            FutureBot.log("Command " + triggered.getAlias() + " was triggered", LoggerFlag.INFO);*/
    }

    public static RestAction<Message> sendMessage(String content, TextChannel channel)
    {
        if (content == null || content.isEmpty())
            return new RestAction.EmptyRestAction<>(null);
        try
        {
            return channel.sendMessage(content);
        } catch (Exception ignored)
        {
            return new RestAction.EmptyRestAction<>(null);
        }
    }

    private Command commandExecution(String cmd, String args, GuildMessageReceivedEvent event)
    {
        TextChannel channel = event.getChannel();
        for (Command c : commands)
        {
            if (c.isCommand(cmd))
            {
                sendMessage(c.getReply(args, event, bot), channel).queue();
                return c;
            }
        }
        return null;
    }
}
