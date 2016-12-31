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
@file:JvmName("FutureEventManager")
package com.futuremangaming.futurebot.internal

import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.hooks.IEventManager
import java.lang.Thread.NORM_PRIORITY
import java.util.LinkedList
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory

/**
 * @author Florian Spieß
 * @since  2016-12-30
 */
class FutureEventManager(val async: Boolean) : IEventManager {

    private var listeners: Set<EventListener> = ConcurrentHashMap.newKeySet()
    val eventQueue: BlockingQueue<Event> = LinkedBlockingQueue()
    val pool: ExecutorService?

    override fun getRegisteredListeners(): MutableList<Any> = LinkedList(listeners)

    override fun handle(event: Event?) {
        eventQueue.offer(event)
    }

    override fun register(listener: Any?) {
        listeners += (listener as? EventListener) ?: throw IllegalArgumentException()
    }

    override fun unregister(listener: Any?) {
        listeners -= (listener as? EventListener) ?: throw IllegalArgumentException()
    }

    init {
        val thread: Thread
        if (async) pool = Executors.newCachedThreadPool(Factory())
        else pool = null
        thread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                val event = eventQueue.take()
                val r = Runnable {
                    for (listener in listeners)
                        listener.onEvent(event)
                }

                if (async)
                    pool?.execute(r)
                else
                    r.run()
                if (event is ShutdownEvent)
                    pool?.shutdown()
            }
        }
        thread.name = "EventManager-mW"
        thread.isDaemon = true
        thread.priority = NORM_PRIORITY
        thread.start()
    }
}

internal class Factory : ThreadFactory {
    override fun newThread(r: Runnable): Thread {
        val thread = Thread(r)
        thread.name = "EventThread"
        thread.isDaemon = true
        return thread
    }
}
