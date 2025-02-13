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
                        @Autowired(required = false) @Qualifier("importStudentJob") Job studentJob,
                        @Autowired(required = false) @Qualifier("importTeacherJob") Job teacherJob) {
        this.jobLauncher = jobLauncher;
        this.studentJob = studentJob;
        this.teacherJob = teacherJob;
    }

    @Scheduled(fixedRateString = "${student.job.fixedRate}", initialDelayString = "${student.job.initialDelay}")
    public void scheduleJobsForStudent() {
        if (studentJob == null) {
            System.out.println("Student Batch jobs are disabled.");
            return;
        }
        System.out.println("Student Batch jobs have been started...");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(studentJob, jobParameters);
            System.out.println("Student Batch job has been triggered.");
        } catch (Exception e) {
            System.err.println("Error launching Student batch job: " + e.getMessage());
        }
    }

    @Scheduled(fixedRateString = "${teacher.job.fixedRate}", initialDelayString = "${teacher.job.initialDelay}")
    public void scheduleJobsForTeacher() {
        if (teacherJob == null) {
            System.out.println("Teacher Batch jobs are disabled.");
            return;
        }
        System.out.println("Teacher Batch jobs have been started...");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(teacherJob, jobParameters);
            System.out.println("Teacher Batch job has been triggered.");
        } catch (Exception e) {
            System.err.println("Error launching Teacher batch job: " + e.getMessage());
        }
    }
}