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

package com.futuremangaming.futurebot;

import net.dv8tion.jda.core.utils.SimpleLog;

public class SimpleLogRedirection implements SimpleLog.LogListener
{
    @Override
    public void onLog(SimpleLog log, SimpleLog.Level logLevel, Object message)
    {
        if (message == null) return;
        if (message instanceof Throwable)
        {
            onError(log, (Throwable) message);
            return;
        }
        LoggerFlag flag;
        switch (logLevel)
        {
            case INFO:
                flag = LoggerFlag.INFO;
                break;
            case WARNING:
                flag = LoggerFlag.WARNING;
                break;
            case FATAL:
                flag = LoggerFlag.FATAL;
                break;
            case DEBUG:
            case TRACE:
            case OFF:
                return;
            default:
                FutureBot.log(message.toString(), LoggerFlag.JDA);
                return;
        }
        FutureBot.log(message.toString(), LoggerFlag.JDA, flag);
    }

    @Override
    public void onError(SimpleLog log, Throwable err)
    {
        FutureBot.log(err.toString(), LoggerFlag.JDA, LoggerFlag.ERROR);
    }
}
