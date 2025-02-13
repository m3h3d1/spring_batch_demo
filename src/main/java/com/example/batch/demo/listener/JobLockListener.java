package com.example.batch.demo.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class JobLockListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        if (!JobLockManager.acquireLock()) {
            System.out.println("Job is already running.");
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        JobLockManager.releaseLock();
    }
}
