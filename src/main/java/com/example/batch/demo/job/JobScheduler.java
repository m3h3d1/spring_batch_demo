package com.example.batch.demo.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobScheduler {

    private final JobLauncher jobLauncher;
    private final Job studentJob;
    private final Job teacherJob;

    @Autowired
    public JobScheduler(JobLauncher jobLauncher,
                        @Qualifier("importStudentJob") Job studentJob,
                        @Qualifier("importTeacherJob") Job teacherJob) {
        this.jobLauncher = jobLauncher;
        this.studentJob = studentJob;
        this.teacherJob = teacherJob;
    }

    @Scheduled(fixedRate = 20000, initialDelay = 20000)
    public void scheduleJobs() {
        System.out.println("Batch jobs have been started...");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(studentJob, jobParameters);
            jobLauncher.run(teacherJob, jobParameters);
            System.out.println("Batch jobs have been triggered.");
        } catch (Exception e) {
            System.err.println("Error while launching batch jobs: " + e.getMessage());
        }
    }

    public void scheduleJob() {
        // Logic to schedule a job
        System.out.println("Job scheduled.");
    }

    public void stopJob() {
        // Logic to stop a job
        System.out.println("Job stopped.");
    }
}
