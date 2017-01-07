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
@file:JvmName("StaticLoggerBinder")
package org.slf4j.impl

import net.dv8tion.jda.core.utils.SimpleLog
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.ILoggerFactory
import org.slf4j.helpers.MarkerIgnoringBase

/**
 * @author Florian Spieß
 * @since  2017-01-05
 */
class StaticLoggerBinder : MarkerIgnoringBase() {

    companion object {
        private val LOG = SimpleLog.getLog("SLF4J")

        @JvmStatic
        fun getSingleton(): StaticLoggerBinder = StaticLoggerBinder()

    }

    fun getLoggerFactory(): ILoggerFactory = ILoggerFactory { getSingleton() }

    // INFO
    override fun info(msg: String?): Unit = LOG.info(msg)
    override fun info(format: String?, arg: Any?) = info(String.format(format!!.replace("{}", "%s"), arg))
    override fun info(format: String?, arg1: Any?, arg2: Any?) = info(String.format(format!!.replace("{}", "%s"), arg1, arg2))
    override fun info(format: String?, vararg arguments: Any?) = info(String.format(format!!.replace("{}", "%s"), *arguments))
    override fun info(msg: String?, t: Throwable?) = info("$msg\n${ExceptionUtils.getStackTrace(t)}")

    // ERROR
    override fun error(msg: String?): Unit = LOG.fatal(msg)
    override fun error(format: String?, arg: Any?): Unit = error(String.format(format!!.replace("{}", "%s"), arg))
    override fun error(format: String?, arg1: Any?, arg2: Any?): Unit = error(String.format(format!!.replace("{}", "%s"), arg1, arg2))
    override fun error(format: String?, vararg arguments: Any?): Unit = error(String.format(format!!.replace("{}", "%s"), *arguments))
    override fun error(msg: String?, t: Throwable?): Unit = error("$msg\n${ExceptionUtils.getStackTrace(t)}")

    // WARN
    override fun warn(msg: String?): Unit = LOG.warn(msg)
    override fun warn(format: String?, arg: Any?) { }
    override fun warn(format: String?, vararg arguments: Any?) { }
    override fun warn(format: String?, arg1: Any?, arg2: Any?) { }
    override fun warn(msg: String?, t: Throwable?) { }

    // DEBUG
    override fun debug(msg: String?) { }
    override fun debug(format: String?, arg: Any?) { }
    override fun debug(format: String?, arg1: Any?, arg2: Any?) {     }
    override fun debug(format: String?, vararg arguments: Any?) { }
    override fun debug(msg: String?, t: Throwable?) { }

    // TRACE
    override fun trace(msg: String?) { }
    override fun trace(format: String?, arg: Any?) { }
    override fun trace(format: String?, arg1: Any?, arg2: Any?) { }
    override fun trace(format: String?, vararg arguments: Any?) { }
    override fun trace(msg: String?, t: Throwable?) { }

    override fun isInfoEnabled(): Boolean = true
    override fun isErrorEnabled(): Boolean = true

    override fun isWarnEnabled(): Boolean = false
    override fun isDebugEnabled(): Boolean = false
    override fun isTraceEnabled(): Boolean = false

}
