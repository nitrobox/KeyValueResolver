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
