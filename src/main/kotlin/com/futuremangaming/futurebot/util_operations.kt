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

import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger
import javax.script.ScriptEngine
import kotlin.experimental.inv

operator fun JSONObject.set(key: String, value: Any) {
    put(key, value)
}
operator fun ScriptEngine.set(key: String, value: Any) {
    put(key, value)
}

operator fun AtomicInteger.inc(): AtomicInteger {
    this.andIncrement
    return this
}
operator fun AtomicInteger.dec(): AtomicInteger {
    this.andDecrement
    return this
}

operator fun Int.not() = inv()
operator fun Short.not() = inv()
operator fun Long.not() = inv()
operator fun Byte.not() = inv()

val mask0Regex = Regex("[\\[\\]*]")
val mask1Regex = Regex("[()*]")

fun String.mask0() = replace(mask0Regex, "\\\\$0")
fun String.mask1() = replace(mask1Regex, "\\\\$0")
