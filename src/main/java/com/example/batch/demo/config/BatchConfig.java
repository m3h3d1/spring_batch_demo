package com.example.batch.demo.config;

import com.example.batch.demo.listener.FileMovingStepExecutionListener;
import com.example.batch.demo.listener.JobLockListener;
import com.example.batch.demo.model.Student;
import com.example.batch.demo.model.Teacher;
import com.example.batch.demo.repository.StudentRepository;
import com.example.batch.demo.repository.TeacherRepository;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.listener.ChunkListenerSupport;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    private final List<Resource> processedResources = new ArrayList<>();

    private static final String STUDENT_FILE_DIRECTORY = "file:D:/Integrations/batch-test/students/*.xml";
    private static final String TEACHER_FILE_DIRECTORY = "file:D:/Integrations/batch-test/teachers/*.xml";

    // Main Batch Job Configuration for Students
    @Bean
    @Lazy
    public Job importStudentJob(StudentRepository studentRepository) {
        Step checkForFilesStep = checkForFilesStep(STUDENT_FILE_DIRECTORY, "checkForStudentFilesStep");
        return jobBuilderFactory.get("importStudentJob")
                .start(checkForFilesStep)
                .on("NO_FILES").fail() // No files, set exit status to fail
                .from(checkForFilesStep)
                .on("*").to(studentStep(studentRepository)) // Files exist, process them
                .end()
                .listener(new JobLockListener()) // Add JobLockListener
                .build();
    }

    // Main Batch Job Configuration for Teachers
    @Bean
    @Lazy
    public Job importTeacherJob(TeacherRepository teacherRepository) {
        Step checkForFilesStep = checkForFilesStep(TEACHER_FILE_DIRECTORY, "checkForTeacherFilesStep");
        return jobBuilderFactory.get("importTeacherJob")
                .start(checkForFilesStep)
                .on("NO_FILES").fail() // No files, set exit status to fail
                .from(checkForFilesStep)
                .on("*").to(teacherStep(teacherRepository)) // Files exist, process them
                .end()
                .listener(new JobLockListener()) // Add JobLockListener
                .build();
    }

    // Step 1: Check if files are present
    public Step checkForFilesStep(String directory, String stepName) {
        return stepBuilderFactory.get(stepName)
                .tasklet((contribution, chunkContext) -> {
                    Resource[] resources = getResources(directory);
                    if (resources.length == 0) {
                        System.out.println("No files found.");
                        contribution
                                .setExitStatus(new ExitStatus("NO_FILES", "No files found in directory: " + directory));
                    } else {
                        System.out.println("Files found for processing: " + Arrays.toString(resources));
                    }
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    // Step 2: Process students
    @Bean
    public Step studentStep(StudentRepository studentRepository) {
        return stepBuilderFactory.get("studentStep")
                .<Student, Student>chunk(2)
                .reader(studentReader())
                .processor(studentItemProcessor())
                .writer(studentItemWriter(studentRepository))
                .listener(new FileMovingStepExecutionListener(processedResources)) // Add listener for every chunk
                .build();
    }

    // Step 2: Process teachers
    @Bean
    public Step teacherStep(TeacherRepository teacherRepository) {
        return stepBuilderFactory.get("teacherStep")
                .<Teacher, Teacher>chunk(4)
                .reader(teacherReader())
                .processor(teacherItemProcessor())
                .writer(teacherItemWriter(teacherRepository))
                .listener(new FileMovingStepExecutionListener(processedResources))
                .build();
    }

    // Fallback Step: Do nothing if no files are present
    @Bean
    public Step noOpStep() {
        return stepBuilderFactory.get("noOpStep")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("No files found. Skipping processing.");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    // Reader for student files
    @Bean
    @StepScope
    public ItemStreamReader<Student> studentReader() {
        processedResources.clear(); // Clear the list for every job execution
        Resource[] resources = getResources(STUDENT_FILE_DIRECTORY);

        if (resources.length == 0) { // Return an empty reader if no files are present
            System.out.println("No files found. Using empty reader.");
            return emptyItemReaderStudent();
        }

        MultiResourceItemReader<Student> reader = new MultiResourceItemReader<>();
        reader.setResources(new Resource[] { resources[0] }); // Only process the first file
        reader.setDelegate(studentItemReader());
        processedResources.add(resources[0]);
        return reader;
    }

    // Reader for teacher files
    @Bean
    @StepScope
    public ItemStreamReader<Teacher> teacherReader() {
        processedResources.clear(); // Clear the list for every job execution
        Resource[] resources = getResources(TEACHER_FILE_DIRECTORY);

        if (resources.length == 0) { // Return an empty reader if no files are present
            System.out.println("No files found. Using empty reader.");
            return emptyItemReaderTeacher();
        }

        MultiResourceItemReader<Teacher> reader = new MultiResourceItemReader<>();
        reader.setResources(new Resource[] { resources[0] }); // Only process the first file
        reader.setDelegate(teacherItemReader());
        processedResources.add(resources[0]);
        return reader;
    }

    // Handle case with no resources
    @Bean
    public ItemStreamReader<Student> emptyItemReaderStudent() {
        return new ItemStreamReader<Student>() {
            @Override
            public Student read() {
                return null;
            }

            @Override
            public void open(ExecutionContext executionContext) {
            }

            @Override
            public void update(ExecutionContext executionContext) {
            }

            @Override
            public void close() {
            }
        };
    }

    @Bean
    public ItemStreamReader<Teacher> emptyItemReaderTeacher() {
        return new ItemStreamReader<Teacher>() {
            @Override
            public Teacher read() {
                return null;
            }

            @Override
            public void open(ExecutionContext executionContext) {
            }

            @Override
            public void update(ExecutionContext executionContext) {
            }

            @Override
            public void close() {
            }
        };
    }

    // Read resources from the directory
    private Resource[] getResources(String directory) {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(directory);
            System.out.println("Detected files in directory " + directory + ": " + Arrays.toString(resources));
            return resources;
        } catch (IOException e) {
            System.err.println("Error reading resources from directory " + directory + ": " + e.getMessage());
            return new Resource[0];
        }
    }

    // XML Reader (specific to Student format)
    @Bean
    public StaxEventItemReader<Student> studentItemReader() {
        StaxEventItemReader<Student> reader = new StaxEventItemReader<>();
        reader.setFragmentRootElementName("student");
        reader.setUnmarshaller(studentUnmarshaller());
        reader.setStrict(true);
        return reader;
    }

    // XML Reader (specific to Teacher format)
    @Bean
    public StaxEventItemReader<Teacher> teacherItemReader() {
        StaxEventItemReader<Teacher> reader = new StaxEventItemReader<>();
        reader.setFragmentRootElementName("teacher");
        reader.setUnmarshaller(teacherUnmarshaller());
        reader.setStrict(true);
        return reader;
    }

    @Bean
    public Jaxb2Marshaller studentUnmarshaller() {
        Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
        unmarshaller.setClassesToBeBound(Student.class);
        return unmarshaller;
    }

    @Bean
    public Jaxb2Marshaller teacherUnmarshaller() {
        Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
        unmarshaller.setClassesToBeBound(Teacher.class);
        return unmarshaller;
    }

    // Processor for students
    @Bean
    public ItemProcessor<Student, Student> studentItemProcessor() {
        return student -> {
            System.out.println("Processing student: " + student);
            return student;
        };
    }

    // Processor for teachers
    @Bean
    public ItemProcessor<Teacher, Teacher> teacherItemProcessor() {
        return teacher -> {
            System.out.println("Processing teacher: " + teacher);
            return teacher;
        };
    }

    // Writer: Write students to database
    public ItemWriter<Student> studentItemWriter(StudentRepository studentRepository) {
        return items -> {
            for (Student student : items) {
                System.out.println("Saving student to database: " + student);
                studentRepository.save(student);
            }

            try {
                Thread.sleep(2000); // Delay after writing each chunk
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Writing delay interrupted", e);
            }
        };
    }

    // Writer: Write teachers to database
    public ItemWriter<Teacher> teacherItemWriter(TeacherRepository teacherRepository) {
        return items -> {
            for (Teacher teacher : items) {
                System.out.println("Saving teacher to database: " + teacher);
                teacherRepository.save(teacher);
            }

            try {
                Thread.sleep(2000); // Delay after writing each chunk
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Writing delay interrupted", e);
            }
        };
    }
}