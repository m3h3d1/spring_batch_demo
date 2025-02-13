package com.example.batch.demo.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobScheduler {

    private final JobLauncher jobLauncher;
    private final Job studentJob;
    private final Job teacherJob;

    @Value("${student.job.fixedRate}")
    private long studentJobFixedRate;

    @Value("${student.job.initialDelay}")
    private long studentJobInitialDelay;

    @Value("${student.job.enabled}")
    private boolean studentJobEnabled;

    @Value("${teacher.job.fixedRate}")
    private long teacherJobFixedRate;

    @Value("${teacher.job.initialDelay}")
    private long teacherJobInitialDelay;

    @Value("${teacher.job.enabled}")
    private boolean teacherJobEnabled;

    @Autowired
    public JobScheduler(JobLauncher jobLauncher,
                        @Qualifier("importStudentJob") Job studentJob,
                        @Qualifier("importTeacherJob") Job teacherJob) {
        this.jobLauncher = jobLauncher;
        this.studentJob = studentJob;
        this.teacherJob = teacherJob;
    }

    @Scheduled(fixedRateString = "${student.job.fixedRate}", initialDelayString = "${student.job.initialDelay}")
    public void scheduleJobsForStudent() {
        if (!studentJobEnabled) {
            System.out.println("Student Batch jobs are disabled.");
            return;
        }
        System.out.println("Student Batch jobs have been started...");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(studentJob, jobParameters);
            System.out.println("Student Batch jobs have been triggered.");
        } catch (Exception e) {
            System.err.println("Error while launching Student batch jobs: " + e.getMessage());
        }
    }

    @Scheduled(fixedRateString = "${teacher.job.fixedRate}", initialDelayString = "${teacher.job.initialDelay}")
    public void scheduleJobsForTeacher() {
        if (!teacherJobEnabled) {
            System.out.println("Teacher Batch jobs are disabled.");
            return;
        }
        System.out.println("Teacher Batch jobs have been started...");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(teacherJob, jobParameters);
            System.out.println("Teacher Batch jobs have been triggered.");
        } catch (Exception e) {
            System.err.println("Error while launching Teacher batch jobs: " + e.getMessage());
        }
    }
}