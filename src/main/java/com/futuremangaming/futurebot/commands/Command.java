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

package com.futuremangaming.futurebot.commands;

import com.futuremangaming.futurebot.FutureBot;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.regex.Pattern;

public class Command
{

    public static final String PREFIX = "!";

    private final String alias;
    private final String reply;
    private final Pattern cmdPattern;

    public Command(String alias, String reply)
    {
        this.alias = alias.toLowerCase();
        this.reply = reply;
        cmdPattern = Pattern.compile("^\\Q" + PREFIX + "\\E\\s*\\Q" + alias + "\\E(?:\\s*(.+))?$", Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
    }

    public Command(String alias)
    {
        this(alias, null);
    }

    public String getReply(String args, GuildMessageReceivedEvent event, FutureBot bot) // Used for legacy
    {
        return reply;
    }

    public String getAlias()
    {
        return alias;
    }

    public boolean isCommand(String cmd)
    {
        return cmdPattern.matcher(cmd).find();
    }

    public boolean isProtected()
    {
        return false;
    }
}
