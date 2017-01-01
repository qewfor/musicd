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

package com.futuremangaming.futurebot

import com.futuremangaming.futurebot.LoggerTag.WARN

/**
 * @author Florian Spieß
 * @since 2016-12-31
 */
fun main(args: Array<String>) {
    var log1 = getLogger("Logger #1")
    val log2 = getLogger("Logger #2")
    log1 = getLogger(log1.name)

    log2.filterOutput(false)
    log2.level = WARN
    log1.filterOutput(true)
    log2.level = WARN

    log2.log(RuntimeException("Test Message")) // should LOG
    log1.log(RuntimeException("Test Message")) // should LOG

    log2.log("Test All", *LoggerTag.values()) // should LOG
    log1.log("Test All", *LoggerTag.values()) // should LOG
    log1.trace("Test Ignored")      // should not LOG
    log2.trace("Test not Ignored")  // should LOG
    log1.error("Should always LOG") // should LOG

    log1.disable()
    log1.log("Test off") // should not LOG

    Logger.lazy(false) { "Test Log" }
}
