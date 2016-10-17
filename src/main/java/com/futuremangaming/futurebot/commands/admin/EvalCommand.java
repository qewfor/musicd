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

package com.futuremangaming.futurebot.commands.admin;

import com.futuremangaming.futurebot.FutureBot;
import com.futuremangaming.futurebot.commands.Command;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class EvalCommand extends Command
{
    public EvalCommand()
    {
        super("eval");
    }

    @Override
    public String getReply(String args, GuildMessageReceivedEvent event, FutureBot bot)
    {
        Member member = event.getMember();
        if (!bot.isAdmin(member))
            return null;
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        engine.put("api", event.getJDA());
        engine.put("channel", event.getChannel());
        engine.put("guild", event.getGuild());
        engine.put("event", event);
        engine.put("bot", bot);
        engine.put("message", event.getMessage());
        engine.put("me", member);
        Object o;
        try
        {
            o = engine.eval(args);
        }
        catch (ScriptException e)
        {
            o = e.getMessage();
        }
        catch (Exception e)
        {
            o = e;
        }
        return o == null ? "null" : o.toString();
    }

    @Override
    public boolean isProtected()
    {
        return true;
    }
}
