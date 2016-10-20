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

package com.futuremangaming.futurebot.commands.moderator;

import com.futuremangaming.futurebot.FutureBot;
import com.futuremangaming.futurebot.commands.Command;
import com.futuremangaming.futurebot.hooks.GuildHook;
import com.futuremangaming.futurebot.hooks.LiveAnnouncer;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.requests.RestAction;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        Color c = event.getMember().getColor();
        String hex = "#" + Integer.toHexString(c.getRed()) + Integer.toHexString(c.getGreen()) + Integer.toHexString(c.getBlue());

        try
        {
            announce(args, new HashSet<>(), event, hex);
        } catch (IllegalArgumentException | JSONException e)
        {
            return e.getMessage() + "\n\n**Help**: <https://goo.gl/E50ibr> - `-custom` populates an attachment!";
        }

        event.getMessage().deleteMessage().queue(
            RestAction.DEFAULT_SUCCESS, e -> GuildHook.sendMessage("\uD83D\uDC4C\uD83C\uDFFD", event.getChannel()).queue()
        );

        return null;
    }

    private void announce(String input, Set<AnnounceFlag> flags, GuildMessageReceivedEvent event, String color)
    {
        if (input == null || input.isEmpty())
            throw new IllegalArgumentException("Missing message!");
        AnnounceFlag flag = AnnounceFlag.forString(input);
        if (flag != AnnounceFlag.DEFAULT)
        {
            if (flag == AnnounceFlag.COLOR)
                color = "#" + flag.getValue(input);
            else if (flag == AnnounceFlag.TWITCH)
                color = "#6441A5";
            flags.add(flag);
            announce(flag.remove(input), flags, event, color);
            return;
        }

        JSONObject object = new JSONObject();
        JSONArray attachments = new JSONArray();
        object.put("username", "FutureBot")
                .put("icon_url", event.getJDA().getSelfInfo().getAvatarUrl())
                .put("attachments", attachments);
        if (flags.contains(AnnounceFlag.EVERYONE))
            object.put("text", "@everyone");

        JSONObject attachment;
        if (flags.contains(AnnounceFlag.CUSTOM))
            attachment = new JSONObject(input);
        else attachment = new JSONObject().put("text", input);
        attachments.put(attachment);

        attachment.put("color", color);
        attachment.put("author_icon", event.getAuthor().getAvatarUrl());
        attachment.put("author_name", event.getMember().getEffectiveName());
        attachment.put("mrkdwn_in", new JSONArray("[\"text\"]"));
        attachment.put("footer", "Posted at " +
                OffsetDateTime.now().atZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("HH:mm:ss a - MMM, dd yyyy zzz")));

        announcer.post(object.toString());
    }

    @Override
    public boolean isProtected()
    {
        return true;
    }

    private enum AnnounceFlag
    {
        COLOR("^\\s*-#([0-9a-fA-F]{6})\\s*"),
        TWITCH("^\\s*-twitch\\s*"),
        EVERYONE("^\\s*-(all|everyone)\\s*"),
        CUSTOM("^\\s*-custom\\s*"),
        DEFAULT("$^");

        private Pattern trigger;

        AnnounceFlag(String regex)
        {
            trigger = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }

        public String remove(String string)
        {
            return string.replaceFirst(trigger.pattern(), "");
        }

        public String getValue(String string)
        {
            Matcher m = trigger.matcher(string);
            if (m.find())
                return m.group(1);
            return "";
        }

        public static AnnounceFlag forString(String message)
        {
            for (AnnounceFlag flag : values())
            {
                if (flag.trigger.matcher(message).find())
                    return flag;
            }
            return DEFAULT;
        }
    }

}
