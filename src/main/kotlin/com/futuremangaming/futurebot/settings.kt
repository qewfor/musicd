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

import net.dv8tion.jda.core.utils.IOUtil
import org.json.JSONObject
import java.io.File
import java.util.HashMap

val PATH = System.getProperty("user.dir") + "/config/"
internal val configs: HashMap<String, Config> = HashMap()

fun getConfig(name: String): Config {
    return configs.getOrPut(name) { Config() }
}

/**
 * @author Florian Spieß
 * @since  2016-12-30
 */
class Config internal constructor() : HashMap<String, Any>() {

    constructor(map: Map<String, Any>) : this() {
        super.putAll(map)
    }

    companion object {
        fun fromJSON(file: File): Config {
            return Config(JSONObject(String(IOUtil.readFully(file))).toMap())
        }
    }

}
