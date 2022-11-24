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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class ReadWriteLockTool {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    public <T> T readLocked(Supplier<T> supplier) {
        readLock.lock();
        try {
            return supplier.get();
        } finally {
            readLock.unlock();
        }
    }

    public void readLocked(Runnable runnable) {
        readLock.lock();
        try {
            runnable.run();
        } finally {
            readLock.unlock();
        }
    }

    public <T> T writeLocked(Supplier<T> supplier) {
        writeLock.lock();
        try {
            return supplier.get();
        } finally {
            writeLock.unlock();
        }
    }

    public void writeLocked(Runnable runnable) {
        writeLock.lock();
        try {
            runnable.run();
        } finally {
            writeLock.unlock();
        }
    }
}
