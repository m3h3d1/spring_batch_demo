package com.example.batch.demo.listener;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JobLockManager {

    private static final Lock lock = new ReentrantLock();

    public static boolean acquireLock() {
        return lock.tryLock();
    }

    public static void releaseLock() {
        lock.unlock();
    }
}
