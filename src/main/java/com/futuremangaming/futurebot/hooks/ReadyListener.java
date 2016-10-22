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

package com.futuremangaming.futurebot.hooks;

import com.futuremangaming.futurebot.FutureBot;
import com.futuremangaming.futurebot.LoggerFlag;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import org.json.JSONObject;

import static com.futuremangaming.futurebot.FutureBot.log;

public class ReadyListener implements EventListener
{

    private final FutureBot bot;

    public ReadyListener(FutureBot bot)
    {
        this.bot = bot;
    }

    @Override
    public void onEvent(Event event)
    {
        if (event instanceof ReadyEvent)
        {
            JDA jda = event.getJDA();
            bot.setJDA(jda);
            JSONObject config = bot.getConfig();
            if (!config.isNull("guild_id"))
            {
                jda.getRegisteredListeners().parallelStream().forEach(jda::removeEventListener);
                GuildHook hook = new GuildHook(config.getString("guild_id"), bot);
                bot.initHardCommands(hook);
                jda.addEventListener(hook);
                jda.addEventListener(new InviteProtection(config.getString("guild_id"), bot));
                LiveAnnouncer announcer = bot.getAnnouncer();
                if (announcer != null)
                {
                    // TODO: Replace once JDA 3 allows setting status!
                    announcer.onLive(s ->
                        ((JDAImpl) jda).getClient().send(new JSONObject()
                            .put("op", 3)
                            .put("d", new JSONObject()
                                .put("game", new JSONObject()
                                    .put("name", s.getJSONObject("channel").getString("status"))
                                    .put("type", 1)
                                    .put("url", "https://twitch.tv/futuremangaming"))
                                .put("since", System.currentTimeMillis())
                                .put("afk", false)
                                .put("status", "online")).toString()
                    ));

                    announcer.onOffline(() ->
                        ((JDAImpl) jda).getClient().send(new JSONObject()
                            .put("op", 3)
                            .put("d", new JSONObject()
                                .put("game", JSONObject.NULL)
                                .put("since", System.currentTimeMillis())
                                .put("afk", false)
                                .put("status", "idle")).toString()
                    ));

                    if (announcer.isLive())
                        announcer.getOnLive().accept(announcer.getLastUpdate());
                }
            }
            else
            {
                log("'guild_id' was not populated!", LoggerFlag.WARNING);
            }
            log("Successfully connected to Discord!", LoggerFlag.SUCCESS);
        }
    }
}
