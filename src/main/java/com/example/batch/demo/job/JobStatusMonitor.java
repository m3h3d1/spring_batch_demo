package com.example.batch.demo.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class JobStatusMonitor {

    private final TaskScheduler taskScheduler;
    private final JobScheduler jobScheduler;
    private final AtomicInteger itemCount = new AtomicInteger(0);
    private boolean isSchedulerRunning = false;

    @Autowired
    public JobStatusMonitor(TaskScheduler taskScheduler, JobScheduler jobScheduler) {
        this.taskScheduler = taskScheduler;
        this.jobScheduler = jobScheduler;
    }

    // Method to start the job scheduler
    public void startScheduler() {
        if (!isSchedulerRunning) {
            jobScheduler.scheduleJob();
            isSchedulerRunning = true;
        }
    }

    // Method to stop the job scheduler
    public void stopScheduler() {
        if (isSchedulerRunning) {
            jobScheduler.stopJob();
            isSchedulerRunning = false;
        }
    }

    // Scheduled method to monitor job status
    @Scheduled(fixedRate = 5000) // Check every 5 seconds
    public void monitorJobStatus() {
        // Logic to monitor job status and item count
        System.out.println("Current item count: " + itemCount.get());
        // Additional logic to check job status can be added here
    }

    // Method to update item count
    public void updateItemCount(int count) {
        itemCount.addAndGet(count);
    }

    // Method to reset item count
    public void resetItemCount() {
        itemCount.set(0);
    }

    // Method to check if the scheduler is running
    public boolean isSchedulerRunning() {
        return isSchedulerRunning;
    }
}