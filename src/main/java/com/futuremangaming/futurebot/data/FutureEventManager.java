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

package com.futuremangaming.futurebot.data;

import com.futuremangaming.futurebot.FutureBot;
import com.futuremangaming.futurebot.LoggerFlag;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.hooks.InterfacedEventManager;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.*;

public class FutureEventManager extends InterfacedEventManager
{

    protected Set<EventListener> listeners = Collections.synchronizedSet(new HashSet<>());

    public FutureEventManager()
    {

    }

    @Override
    public void register(Object listener)
    {
        if (listener instanceof EventListener)
            listeners.add((EventListener) listener);
        else
            throw new IllegalArgumentException();
    }

    @Override
    public void unregister(Object listener)
    {
        if (listener instanceof EventListener)
            listeners.remove(listener);
    }

    @Override
    public List<Object> getRegisteredListeners()
    {
        return new LinkedList<>(listeners);
    }

    @Override
    public void handle(Event event)
    {
        for (EventListener listener : listeners)
        {
            try
            {
                listener.onEvent(event);
            }
            catch (Exception e)
            {
                FutureBot.log(ExceptionUtils.getStackTrace(e), LoggerFlag.JDA, LoggerFlag.ERROR);
            }
        }
    }
}
