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

import com.futuremangaming.futurebot.AnsiCode.Companion.BLUE
import com.futuremangaming.futurebot.AnsiCode.Companion.CLEAN
import com.futuremangaming.futurebot.AnsiCode.Companion.CYAN
import com.futuremangaming.futurebot.AnsiCode.Companion.GREEN
import com.futuremangaming.futurebot.AnsiCode.Companion.RED
import com.futuremangaming.futurebot.AnsiCode.Companion.RED_LIGHT
import com.futuremangaming.futurebot.AnsiCode.Companion.RESET
import com.futuremangaming.futurebot.AnsiCode.Companion.WHITE_LIGHT
import com.futuremangaming.futurebot.AnsiCode.Companion.cyanLight
import com.futuremangaming.futurebot.LoggerTag.DEBUG
import com.futuremangaming.futurebot.LoggerTag.ERROR
import com.futuremangaming.futurebot.LoggerTag.INFO
import com.futuremangaming.futurebot.LoggerTag.INTERNAL
import com.futuremangaming.futurebot.LoggerTag.OFF
import com.futuremangaming.futurebot.LoggerTag.TRACE
import net.dv8tion.jda.core.utils.SimpleLog
import net.dv8tion.jda.core.utils.SimpleLog.Level
import org.apache.commons.lang3.exception.ExceptionUtils
import java.io.PrintStream
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAccessor
import java.util.HashMap
import java.util.regex.Pattern
import kotlin.jvm.JvmField as static

val loggers = HashMap<String, Logger>()
val printLock = Any()
val newLine = Pattern.compile("\n\r?")!!

fun getLogger(name: String): Logger =
    loggers.getOrPut(name) { Logger(name) }

/**
 * @author Florian Spieß
 * @since  2016-12-30
 */
open class Logger internal constructor(internal val name: String) {

    var level = LoggerTag.valueOf(System.getProperty("bot.log.level", "info").toUpperCase())
    var out = OUT
    var err = ERR
    var leveled = true
        private set

    companion object {

        @static
        val OUT: PrintStream = System.out
        @static
        val ERR: PrintStream = System.err
        @static
        val ZONE = { ZoneId.of(System.getProperty("app.time.zoneid", "UTC")) }
        @static
        val ANSI = { System.getProperty("app.log.ansi")?.toBoolean() ?: true }

        fun stackTags(tags: List<LoggerTag>): String {
            return tags
                    .map { it.toString(ANSI()) }
                    .joinToString(" ")
        }

        fun timeStamp(temporal: TemporalAccessor = OffsetDateTime.now()): String {
            val time = OffsetDateTime.from(temporal).atZoneSameInstant(ZONE())
            val hour = time[ChronoField.HOUR_OF_DAY]
            val minute  = time[ChronoField.MINUTE_OF_HOUR]
            val second  = time[ChronoField.SECOND_OF_MINUTE]

            return String.format("%02d:%02d:%02d", hour, minute, second)
        }

        inline fun lazy(out: PrintStream = OUT, message: () -> String): String? {
            synchronized(printLock) {
                val print = message()
                if (print.isBlank()) return null

                out.println(print)
                return print
            }
        }
    }

    fun log(message: String, error: Boolean, vararg tags: LoggerTag): String? {
        if (message.isBlank())
            return null
        if (leveled && tags.none { it.ordinal <= level.ordinal })
            return null
        tags.sortBy { it.ordinal }

        val listTags = tags.distinct().filter { it !== OFF }
        val head = "[${timeStamp()}] [$name] ${stackTags(listTags)} "
        val stream = if (error) err else out

        return lazy(stream) { "$head${newLine.matcher(message.trim()).replaceAll(System.lineSeparator() + head)}" }
    }

    fun log(message: Any, vararg tags: LoggerTag): String? = log(message.toString(), false, *tags)

    infix fun log(message: Throwable): String?   = log(ExceptionUtils.getStackTrace(message))
    infix fun trace(message: Any): String?       = log(message, TRACE)
    infix fun internal(message: Any): String?    = log(message, INTERNAL)
    infix fun debug(message: Any): String?       = log(message, DEBUG)
    infix fun info(message: Any): String?        = log(message, INFO)
    infix fun warn(message: Any): String?        = log(message, TRACE)
    infix fun error(message: Any): String?       = log(message.toString(), true, ERROR)
    infix fun error(message: Throwable): String? = log(ExceptionUtils.getStackTrace(message), true, ERROR)
    infix fun log(message: Any): String?         = info(message)

    fun clean(err: Boolean = false) {
        lazy(out) { CLEAN }

        if (err) lazy(err) { CLEAN }
    }

    //////////////////////////////////
    /// Modifier
    //////////////////////////////////

    fun filterOutput(bool: Boolean) {
        leveled = bool
    }

    fun disable() {
        filterOutput(true)
        level = OFF
    }

}

enum class LoggerTag(internal val ansi: String) {
    OFF(     GREEN),
    ERROR(   RED),
    WARN(    RED_LIGHT),
    INFO(    WHITE_LIGHT),
    DEBUG(   WHITE_LIGHT),
    INTERNAL(CYAN),
    TRACE(   BLUE);

    override fun toString(): String {
        return toString(true)
    }

    fun toString(useAnsi: Boolean): String {
        return if (useAnsi) toAnsiString() else "[$name]"
    }

    fun toAnsiString(): String {
        return "[$ansi$name$RESET]"
    }

    companion object {
        fun convert(level: SimpleLog.Level): LoggerTag {
            when (level) {
                SimpleLog.Level.TRACE -> return TRACE
                SimpleLog.Level.WARNING -> return WARN
                SimpleLog.Level.DEBUG -> return DEBUG
                else -> return INFO
            }
        }
    }
}

@Suppress("unused")
class AnsiCode {
    companion object {
        val ESC = "\u001B"
        val RESET = "$ESC[0m"
        val CLEAN = "${ESC}c"


        val BLACK = "$ESC[30m"
        val BLACK_LIGHT  = "$ESC[30;1m"

        val RED   = "$ESC[31m"
        val RED_LIGHT    = "$ESC[31;1m"

        val GREEN = "$ESC[32m"
        val GREEN_LIGHT  = "$ESC[32;1m"

        val YELLOW = "$ESC[33m"
        val YELLOW_LIGHT = "$ESC[33;1m"

        val BLUE  = "$ESC[34m"
        val BLUE_LIGHT   = "$ESC[34;1m"

        val PINK  = "$ESC[35m"
        val PINK_LIGHT   = "$ESC[35;1m"

        val CYAN  = "$ESC[36m"
        val CYAN_LIGHT   = "$ESC[36;1m"

        val WHITE = "$ESC[37m"
        val WHITE_LIGHT  = "$ESC[37;1m"

        fun black(value: String): String       = "$BLACK$value$RESET"
        fun blackLight(value: String): String  = "$BLACK_LIGHT$value$RESET"

        fun red(value: String): String         = "$RED$value$RESET"
        fun redLight(value: String): String    = "$RED_LIGHT$value$RESET"

        fun green(value: String): String       = "$GREEN$value$RESET"
        fun greenLight(value: String): String  = "$GREEN_LIGHT$value$RESET"

        fun yellow(value: String): String      = "$YELLOW$value$RESET"
        fun yellowLight(value: String): String = "$YELLOW_LIGHT$value$RESET"

        fun blue(value: String): String        = "$BLUE$value$RESET"
        fun blueLight(value: String): String   = "$BLUE_LIGHT$value$RESET"

        fun pink(value: String): String        = "$PINK$value$RESET"
        fun pinkLight(value: String): String   = "$PINK_LIGHT$value$RESET"

        fun cyan(value: String): String        = "$CYAN$value$RESET"
        fun cyanLight(value: String): String   = "$CYAN_LIGHT$value$RESET"

        fun white(value: String): String       = "$WHITE$value$RESET"
        fun whiteLight(value: String): String  = "$WHITE_LIGHT$value$RESET"
    }
}

class SimpleLogger : Logger("JDA"), SimpleLog.LogListener {
    override fun onError(log: SimpleLog?, err: Throwable) {
        log(err)
    }

    override fun onLog(log: SimpleLog, logLevel: Level, message: Any?) {
        log((
            if (log.name == "JDA") ""
            else "(${cyanLight(log.name)}) ")
               + message, LoggerTag.convert(logLevel)
        )
    }
}
