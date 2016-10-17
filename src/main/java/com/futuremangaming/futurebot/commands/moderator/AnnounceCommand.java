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
import com.futuremangaming.futurebot.hooks.LiveAnnouncer;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AnnounceCommand extends Command
{

    private LiveAnnouncer announcer;

    public AnnounceCommand(LiveAnnouncer announcer)
    {
        super("announce");
        this.announcer = announcer;
    }

    @Override
    public String getReply(String args, GuildMessageReceivedEvent event, FutureBot bot)
    {
        if (announcer == null || (!bot.isAdmin(event.getMember()) && !bot.isMod(event.getMember())))
            return null;
        JSONObject object = new JSONObject();
        JSONArray attachments = new JSONArray();
        object.put("username", "FutureBot")
              .put("icon_url", event.getJDA().getSelfInfo().getAvatarUrl())
              .put("attachments", attachments);

        JSONObject attachment = new JSONObject();
        attachments.put(attachment);

        Color c = event.getMember().getColor();

        attachment.put("color", "#" + Integer.toHexString(c.getRed()) + Integer.toHexString(c.getGreen()) + Integer.toHexString(c.getBlue()));
        attachment.put("author_icon", event.getAuthor().getAvatarUrl());
        attachment.put("author_name", event.getMember().getEffectiveName());
        attachment.put("text", args);
        attachment.put("mrkdwn_in", new JSONArray("[\"text\"]"));
        attachment.put("footer", "Posted at " +
                OffsetDateTime.now().atZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("HH:mm:ss a - MMM, dd yyyy zzz")));

        announcer.post(object.toString());

        return "\uD83D\uDC4C\uD83C\uDFFD";
    }

    @Override
    public boolean isProtected()
    {
        return true;
    }
}
