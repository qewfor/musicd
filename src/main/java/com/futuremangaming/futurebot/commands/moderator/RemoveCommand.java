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
import com.futuremangaming.futurebot.data.DataBase;
import com.futuremangaming.futurebot.hooks.GuildHook;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class RemoveCommand extends Command
{

    private GuildHook hook;

    public RemoveCommand(GuildHook hook)
    {
        super("remove");
        this.hook = hook;
    }

    @Override
    public String getReply(String args, GuildMessageReceivedEvent event, FutureBot bot)
    {
        if (!event.getMember().getRoles().parallelStream().anyMatch(r -> r.getId().equals(bot.getModRole())))
            return null;
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 1)
            return "**Usage**: `" + Command.PREFIX + getAlias() + " <command>`\n\n**Info**: " +
                    "Commands removed with this may not remove them from the twitch chat!";
        String alias = parts[0];
        if (hook.find(alias) == null)
            return "Command `" + alias + "` not found!";
        hook.removeCommandIf(c -> c.getAlias().equalsIgnoreCase(alias));
        if (bot.getDataBase().isAvailable())
        {
            if (bot.getDataBase().removeFrom("Command", "alias = \"" + DataBase.sanitize(alias.toLowerCase()) + "\""))
                return "Deleted `" + alias + "`!";
        }
        return "Removed command **only** for current session, due to the database being unreachable.";
    }

    @Override
    public boolean isProtected()
    {
        return true;
    }
}
