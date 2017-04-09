/*
 *     Copyright 2014-2017 FuturemanGaming
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

package com.futuremangaming.futurebot

import net.dv8tion.jda.core.utils.SimpleLog
import net.dv8tion.jda.core.utils.SimpleLog.Level
import net.dv8tion.jda.core.utils.SimpleLog.Level.DEBUG
import net.dv8tion.jda.core.utils.SimpleLog.Level.FATAL
import net.dv8tion.jda.core.utils.SimpleLog.Level.INFO
import net.dv8tion.jda.core.utils.SimpleLog.Level.TRACE
import net.dv8tion.jda.core.utils.SimpleLog.Level.WARNING
import org.slf4j.LoggerFactory

fun getLogger(name: String) = LoggerFactory.getLogger(name)!!
fun getLogger(clazz: Class<*>) = LoggerFactory.getLogger(clazz)!!

class SimpleLogger : SimpleLog.LogListener {
    override fun onError(log: SimpleLog, err: Throwable) {
        val name = log.name
        val logger = getLogger(if (name.contains("JDA")) name else "JDA_$name")
        logger.error("Failure ", err)
    }

    override fun onLog(log: SimpleLog, logLevel: Level, message: Any?) {
        val name = log.name
        val logger = getLogger(if (name.contains("JDA")) name else "JDA_$name")
        if (message is Throwable) return onError(log, message)
        logger.apply { when (logLevel) {
            INFO    -> info(message.toString())
            WARNING -> warn(message.toString())
            TRACE   -> trace(message.toString())
            FATAL   -> error(message.toString())
            DEBUG   -> debug(message.toString())
            else    -> { }
        } }
    }

}
