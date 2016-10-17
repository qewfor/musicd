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

package com.futuremangaming.futurebot.commands.moderator;

import com.futuremangaming.futurebot.FutureBot;
import com.futuremangaming.futurebot.commands.Command;
import com.futuremangaming.futurebot.hooks.GuildHook;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class AddCommand extends Command
{

    private final GuildHook hook;

    public AddCommand(GuildHook hook)
    {
        super("add");
        this.hook = hook;
    }

    @Override
    public String getReply(String args, GuildMessageReceivedEvent event, FutureBot bot)
    {
        if (!bot.isAdmin(event.getMember()) && !bot.isMod(event.getMember()))
            return null;
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2)
            return "**Usage**: `" + Command.PREFIX + getAlias() + " <trigger> <reply>`\n\n**Info**: " +
                    "Commands added with this route are case-insensitive!";
        String alias = parts[0];
        String reply = parts[1];
        hook.registerCommand(new Command(alias, reply));
        if (bot.getDataBase().isAvailable())
        {
            if (bot.getDataBase().insertInto("Command(alias, reply, type)", alias.toLowerCase(), reply, 0))
            {
                return "Successfully created new Command!";
            }
        }
        return "Created command **only** for current session, due to the database being unreachable.";
    }

    @Override
    public boolean isProtected()
    {
        return true;
    }
}
