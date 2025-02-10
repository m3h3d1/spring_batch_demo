package com.example.batch.demo.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobScheduler {

    private final JobLauncher jobLauncher;
    private final Job job;

    @Autowired
    public JobScheduler(JobLauncher jobLauncher, Job job) {
        this.jobLauncher = jobLauncher;
        this.job = job;
    }

    @Scheduled(fixedRate = 20000, initialDelay = 20000)
    public void scheduleJob() {
        System.out.println("Batch job has been started...");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(job, jobParameters);
            System.out.println("Batch job has been triggered.");
        } catch (Exception e) {
            System.err.println("Error while launching batch job: " + e.getMessage());
        }
    }

    public void stopJob() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
