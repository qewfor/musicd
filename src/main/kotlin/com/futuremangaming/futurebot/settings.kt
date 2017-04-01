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
import java.io.File
import java.util.HashMap

val PATH = System.getProperty("bot.config") ?: "config/"
internal val configs: HashMap<String, Config> = hashMapOf()

fun getConfig(name: String): Config =
        configs.getOrPut(name) { Config() }

/**
 * @author Florian Spieß
 * @since  2016-12-30
 */
class Config internal constructor(val map: HashMap<String, Any>) : MutableMap<String, Any> by map {

    constructor() : this(HashMap())
    constructor(json: JSONObject) : this(json.toMap() as HashMap<String, Any>)

    companion object {
        fun fromJSON(name: String, file: File): Config =
                configs.getOrPut(name) { Config(JSONObject(file.readText())) }
    }

}
