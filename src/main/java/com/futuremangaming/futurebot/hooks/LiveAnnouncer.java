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
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LiveAnnouncer
{

    private String clientId;
    private String route;
    private boolean executed = false;
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public LiveAnnouncer(String route, String client_id)
    {
        this.route = route;
        this.clientId = client_id;
        executorService.scheduleAtFixedRate(this::execute, 0, 2, TimeUnit.MINUTES);
    }

    public void post(String post)
    {
        try
        {
            HttpResponse<String> response = Unirest.post(route + "/slack").header("content-type", "application/json").body(post)
                    .asString();
            if (response.getStatus() >= 300)
                FutureBot.log("Got status code '" + response.getStatus() + ": " + response.getStatusText() + "' trying to announce live stream!", LoggerFlag.FATAL);
            if (response.getStatus() == 400)
                throw new IllegalArgumentException("Response: " + new JSONObject(response.getBody()).toString());
        }
        catch (UnirestException e)
        {
            FutureBot.log("Encountered UnirestException trying to post to webhook.", LoggerFlag.FATAL, LoggerFlag.ERROR);
        }
    }

    private synchronized void execute()
    {
        JSONObject stream = getStream();
        if (stream == null)
        {
            executed = false;
            return;
        }
        if (executed)
            return;
        executed = true;
        JSONObject post = new JSONObject();
        JSONArray attachments = new JSONArray();
        post.put("username", "FutureBot")
            .put("icon_url", "https://cdn.discordapp.com/avatars/237295476683177984/c670f0cc4340f7d64f0b47849f730dca.jpg")
            .put("attachments", attachments);

        JSONObject reformatted = new JSONObject();
        attachments.put(reformatted);
        if (!stream.isNull("game"))
            reformatted.put("footer", "Streaming " + stream.getString("game"));
        if (!stream.isNull("preview"))
            reformatted.put("image_url", stream.getJSONObject("preview").getString("medium") + "?rng=" + Integer.toHexString(new Random().nextInt()));

        JSONObject channel = stream.getJSONObject("channel");
        reformatted.put("color", "#6441A5");
        reformatted.put("title", channel.get("status")).put("title_link", channel.getString("url"));
        reformatted.put("author_name", channel.get("name"));
        reformatted.put("author_icon", channel.get("logo"));
        reformatted.put("author_link", channel.get("url"));
        try
        {
            post(post.toString());
        } catch (IllegalArgumentException e)
        {
            FutureBot.log(e.getMessage(), LoggerFlag.ERROR);
        }
    }

    private JSONObject getStream()
    {
        try
        {
            HttpResponse<JsonNode> response = Unirest.get("https://api.twitch.tv/kraken/streams?stream_type=live&channel=futuremangaming")
                    .header("accept","application/vnd.twitchtv.v3+json")
                    .header("content-type", "application/json")
                    .header("client-id", clientId).asJson();
            if (response.getStatus() >= 300 || response.getBody().getObject().isNull("streams"))
                return null;
            JSONArray streams = response.getBody().getObject().getJSONArray("streams");
            if (streams.length() == 1)
            {
                JSONObject obj = streams.getJSONObject(0);
                if (!obj.isNull("_id"))
                    return obj;
            }
            return null;
        }
        catch (UnirestException e)
        {
            return null;
        }

    }

    public void shutdown()
    {
        executorService.shutdown();
    }

}
