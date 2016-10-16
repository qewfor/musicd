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

package com.futuremangaming.futurebot;

import com.futuremangaming.futurebot.data.DataBase;
import com.futuremangaming.futurebot.hooks.GuildHook;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.utils.SimpleLog;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

public class FutureBot
{

    private JDA jda;
    private DataBase dataBase = null;
    private JSONObject config;
    private Set<String> admins = new HashSet<>();

    public FutureBot() throws Exception
    {
        config = retrieve(new File("config.json"));
        checkConfig();
        if (!config.isNull("ansi") && config.get("ansi") instanceof Boolean)
            LoggerFlag.useColor = config.getBoolean("ansi");
        dataBase = connectDatabase();
        JSONArray array = config.getJSONArray("administrators");
        for (int i = 0; i < array.length(); i++)
            admins.add(array.getString(i));
    }

    /* Getters & Setters */

    public JSONObject getConfig()
    {
        return config;
    }

    public DataBase getDataBase()
    {
        return dataBase;
    }

    public boolean isAdmin(Member member)
    {
        return admins.contains(member.getUser().getId());
    }

    /* Connection Management */

    public void connectDiscord(boolean block) throws LoginException, InterruptedException, RateLimitedException
    {
        log("Establishing connection to discord...", LoggerFlag.INFO);
        JDABuilder builder = new JDABuilder(AccountType.BOT)
                .setToken(config.getString("authorization"))
                .setAudioEnabled(false)
                .setBulkDeleteSplittingEnabled(false)
                .setEnableShutdownHook(true)
                .addListener((EventListener) event -> {
                    if (event instanceof ReadyEvent)
                    {
                        jda = event.getJDA();
                        if (!config.isNull("guild_id"))
                            jda.addEventListener(new GuildHook(config.getString("guild_id"), this));
                        else log("'guild_id' was not populated!", LoggerFlag.WARNING);
                        log("Successfully connected to Discord!", LoggerFlag.SUCCESS);
                    }
                });
        if (block)
            builder.buildBlocking();
        else builder.buildAsync();
    }

    public void shutdown(boolean free)
    {
        try
        {
            log("Shutting down...", LoggerFlag.INFO);
            if (jda != null) jda.shutdown(free); // annoy dv8 about this not working in 3.x
            if (dataBase != null) dataBase.close();
        }
        catch (Exception e)
        {
            log(e.toString(), LoggerFlag.ERROR, LoggerFlag.FATAL);
        }
        log("Cleared all connections!" , LoggerFlag.SUCCESS);
    }

    /* Private Methods */

    private File generateFile(String name, JSONObject contents) throws IOException
    {
        File f = new File(name);
        if (!f.createNewFile())
            throw new IOException(name + " was missing and we were unable to generate it.");
        FileUtils.writeStringToFile(f, contents.toString(2), "UTF-8");
        return f;
    }

    private JSONObject retrieve(File resource)
    {
        try
        {
            String output = FileUtils.readFileToString(resource, "UTF-8");
            return new JSONObject(output);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        catch (IOException ignored)
        {
        }
        return null;
    }

    private DataBase connectDatabase() throws Exception
    {
        JSONObject database = config.getJSONObject("database");
        log("Establishing connection to database...", LoggerFlag.INFO);
        DataBase db = new DataBase(
                database.getString("ip"),
                database.getInt("port"),
                database.getString("database"),
                database.getString("username"),
                database.getString("password"));
        if (db.isAvailable())
        {
            log("Database connection established!", LoggerFlag.SUCCESS);
        }
        else
        {
            log("Database connection failed.", LoggerFlag.FATAL);
            log("SQLError: " + db.getError(), LoggerFlag.ERROR);
        }
        return db;
    }

    private void checkConfig() throws IOException
    {
        if (config == null)
        {
            config = new JSONObject();
            config
                    .put("guild_id", "id of server to operate on")
                    .put("authorization", "bot token here")
                    .put("administrators", new JSONArray().put("administrator ids here"))
                    .put("database", new JSONObject().put("ip", "").put("database", "").put("username", "").put
                            ("password", "").put("port", 3302));
            generateFile("config.json", config);
            throw new IOException("'config.json' has been generated in the current working directory. " +
                    "Please populate the contained JSON-Object with valid information.");
        }
    }

    /* Static Methods */

    /**
     * Used for logging.
     *
     * @param message
     *      The Message to log
     * @param flags
     *      The varargs {@link LoggerFlag Flags}
     */
    public static void log(String message, LoggerFlag... flags)
    {
        LocalTime time = LocalTime.now();
        StringBuilder builder = new StringBuilder(String.format("[%s:%s:%s]",
                (time.getHour() < 10 ? "0" : "") + time.getHour(),
                (time.getMinute() < 10 ? "0" : "") + time.getMinute(),
                (time.getSecond() < 10 ? "0" : "") + time.getSecond()));
        for (LoggerFlag flag : flags)
            builder.append(" ").append(flag.toString());
        synchronized (System.err)
        {
            System.out.println(builder.append(" ").append(message.replace("\n", "\n + \t\t")).toString());
        }
    }

    public static void main(String[] args) throws Exception
    {
        SimpleLog.addListener(new SimpleLogRedirection());
        SimpleLog.LEVEL = SimpleLog.Level.OFF;
        try
        {
            FutureBot bot = new FutureBot();
            bot.connectDiscord(true);
            //bot.shutdown(true);
        }
        catch (JSONException e)
        {
            log(String.format("One of the configuration files was not populated correctly. Please delete it and run " +
                    "the script again. (%s)", e.getMessage()), LoggerFlag.FATAL, LoggerFlag.ERROR);
            System.exit(-1);
        }
    }

}
