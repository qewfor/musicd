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
package com.futuremangaming.futurebot.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.core.audio.AudioSendHandler

/**
 * @author Florian Spieß
 * @since  2017-01-02
 */
class PlayerSendHandler(private val player: AudioPlayer) : AudioSendHandler {

    private var lastFrame: AudioFrame? = null

    override fun provide20MsAudio(): ByteArray? {
        if (lastFrame === null) {
            lastFrame = player.provide()
        }

        val data = lastFrame?.data
        lastFrame = null

        return data
    }

    override fun canProvide(): Boolean {
        if (lastFrame === null) {
            lastFrame = player.provide()
        }

        return lastFrame !== null
    }

    override fun isOpus(): Boolean = true
}
