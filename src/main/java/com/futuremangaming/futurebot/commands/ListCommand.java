/*
 *     Copyright 2016 FuturemanGaming
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

package com.futuremangaming.futurebot.commands;

import com.futuremangaming.futurebot.FutureBot;
import com.futuremangaming.futurebot.hooks.GuildHook;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.LinkedList;
import java.util.List;

public class ListCommand extends Command
{

    private GuildHook hook;

    public ListCommand(GuildHook hook)
    {
        super("list");
        this.hook = hook;
    }

    @Override
    public String getReply(String args, GuildMessageReceivedEvent event, FutureBot bot)
    {
        StringBuilder builder = new StringBuilder();
        List<Command> commands = new LinkedList<>(hook.getCommands());

        builder.append("**Administrator**:");
        commands.stream().filter(
                c -> c.getClass().getPackage().getName().equals("com.futuremangaming.futurebot.commands.admin")
        ).map(c -> " `" + c.toString() + "`" + (c.isProtected() ? "\\*" : "")).forEach(builder::append);

        builder.append("\n\n**Moderator**:");
        commands.stream().filter(
                c -> c.getClass().getPackage().getName().equals("com.futuremangaming.futurebot.commands.moderator")
        ).map(c -> " `" + c.toString() + "`" + (c.isProtected() ? "\\*" : "")).forEach(builder::append);

        builder.append("\n\n**Regular**:");
        commands.stream().filter(
                c -> c.getClass().getPackage().getName().equals("com.futuremangaming.futurebot.commands.regular")
        ).map(c -> " `" + c.toString() + "`" + (c.isProtected() ? "\\*" : "")).forEach(builder::append);

        builder.append("\n\n**Custom**:");
        commands.stream().filter(
                c -> c.getClass().getSimpleName().equals("Command")
        ).map(c -> " `" + c.toString() + "`" + (c.isProtected() ? "\\*" : "")).forEach(builder::append);

        return builder.append("\n\n\\* These commands cannot be removed\n\n_Note: This list will be removed in beta and will instead be hosted and linked to!_").toString();
    }

    @Override
    public boolean isProtected()
    {
        return true;
    }
}
