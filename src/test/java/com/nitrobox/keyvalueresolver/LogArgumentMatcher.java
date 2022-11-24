/*
 * KeyValueResolver - An dynamic Key-Value Store
 * Copyright (C) 2022 Nitrobox GmbH
 *
 * This Software is a fork of Roperty - An advanced property
 * management and retrival system
 * Copyright (C) 2013 PARSHIP GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nitrobox.keyvalueresolver;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.mockito.ArgumentMatcher;

/**
 * Created by dheid on 22.03.17.
 */
class LogArgumentMatcher implements ArgumentMatcher<ILoggingEvent> {

    private final Level level;
    private final CharSequence expectedLog;

    LogArgumentMatcher(Level level, CharSequence expectedLog) {
        this.level = level;
        this.expectedLog = expectedLog;
    }

    @Override
    public boolean matches(final ILoggingEvent argument) {
        return argument.getLevel().equals(level)
            && argument.getFormattedMessage().contains(expectedLog);
    }

    @Override
    public String toString() {
        return "[" + level + "] ..." + expectedLog + "...";
    }
}
