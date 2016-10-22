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

import com.futuremangaming.futurebot.LoggerFlag;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.rmi.UnexpectedException;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.futuremangaming.futurebot.FutureBot.log;

public class LiveAnnouncer
{

    private Runnable offlineRunnable = null;
    private Consumer<JSONObject> onLive = null;

    private String clientId;
    private String route;
    private boolean executed = false;
    private volatile JSONObject lastUpdate = null;
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public LiveAnnouncer(String route, String client_id)
    {
        this.route = route;
        this.clientId = client_id;
        executorService.scheduleAtFixedRate(this::execute, 0, 3, TimeUnit.MINUTES);
    }

    public void onLive(Consumer<JSONObject> consumer)
    {
        this.onLive = consumer;
    }

    public void onOffline(Runnable runnable)
    {
        this.offlineRunnable = runnable;
    }

    /* Getters & Setters */

    public boolean isLive()
    {
        return lastUpdate != null;
    }

    public JSONObject getLastUpdate()
    {
        return lastUpdate;
    }

    public Consumer<JSONObject> getOnLive()
    {
        return onLive;
    }

    public Runnable getOnOffline()
    {
        return offlineRunnable;
    }

    /* Internals */

    public void post(String post)
    {
        try
        {
            HttpResponse<String> response = Unirest.post(route + "/slack").header("content-type", "application/json").body(post)
                    .asString();
            if (response.getStatus() >= 300)
                log("Got status code '" + response.getStatus() + ": " + response.getStatusText() + "' trying to announce live stream!", LoggerFlag.FATAL);
            if (response.getStatus() == 400)
                throw new IllegalArgumentException("Response: " + new JSONObject(response.getBody()).toString());
        }
        catch (UnirestException e)
        {
            log("Encountered UnirestException trying to post to webhook.", LoggerFlag.WARNING);
            log(e.toString(), LoggerFlag.ERROR);
        }
    }

    private synchronized void execute()
    {
        try
        {
            this.lastUpdate = getStream();
        } catch (UnexpectedException e) // received status code 400
        {
            log(e.getMessage(), LoggerFlag.WARNING);
            return;
        }
        if (lastUpdate == null)
        {
            if (executed && offlineRunnable != null)
                offlineRunnable.run();
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
        if (!lastUpdate.isNull("game"))
            reformatted.put("footer", "Streaming " + lastUpdate.getString("game"));
        if (!lastUpdate.isNull("preview"))
            reformatted.put("image_url", lastUpdate.getJSONObject("preview").getString("medium") + "?rng=" + Integer.toHexString(new Random().nextInt()));

        JSONObject channel = lastUpdate.getJSONObject("channel");
        reformatted.put("color", "#6441A5");
        reformatted.put("title", channel.get("status")).put("title_link", channel.getString("url"));
        reformatted.put("author_name", channel.get("name"));
        reformatted.put("author_icon", channel.get("logo"));
        reformatted.put("author_link", channel.get("url"));
        try
        {
            post(post.toString());
        } catch (Exception e)
        {
            log(e.getMessage(), LoggerFlag.ERROR);
        } finally
        {
            if (onLive != null)
                onLive.accept(lastUpdate);
        }
    }

    private JSONObject getStream() throws UnexpectedException
    {
        try
        {
            HttpResponse<JsonNode> response = Unirest.get("https://api.twitch.tv/kraken/streams/futuremangaming")
                    .header("accept","application/vnd.twitchtv.v3+json")
                    .header("content-type", "application/json")
                    .header("client-id", clientId).asJson();
            JSONObject object = response.getBody().getObject();
            if (response.getStatus() == 400)
                throw new UnexpectedException("Unexpected 400 response: " + (Objects.isNull(object) ? "null" : object.toString()));
            if (response.getStatus() >= 300)
            {
                log(
                        "Got status " + response.getStatus() + ": " + response.getStatusText() + " trying to query stream!",
                        LoggerFlag.FATAL,
                        LoggerFlag.ERROR
                );
                if (object != null)
                    log("JSON: " + object.toString(), LoggerFlag.INFO);
                return null;
            } else if (object.isNull("stream")) return null;
            return object.getJSONObject("stream");
        }
        catch (UnirestException e)
        {
            log("Encountered UnirestException trying to query stream!", LoggerFlag.WARNING);
            log(e.toString(), LoggerFlag.ERROR);
            return null;
        }

    }

    public void shutdown()
    {
        executorService.shutdown();
    }

}
