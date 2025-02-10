package com.example.batch.demo.config;

import com.example.batch.demo.listener.FileMovingStepExecutionListener;
import com.example.batch.demo.model.Student;
import com.example.batch.demo.repository.StudentRepository;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    private final List<Resource> processedResources = new ArrayList<>();

    private static final String FILE_DIRECTORY = "file:D:/Integrations/batch-test/students/*.xml";

    // Main Batch Job Configuration
    @Bean
    public Job importStudentJob(StudentRepository studentRepository) {
        return jobBuilderFactory.get("importStudentJob")
                .start(checkForFilesStep())
                .on("NO_FILES").to(noOpStep())    // No files, go to noOpStep
                .from(checkForFilesStep())
                .on("*").to(studentStep(studentRepository))        // Files exist, process them
                .end()
                .build();
    }

    // Step 1: Check if files are present
    @Bean
    public Step checkForFilesStep() {
        return stepBuilderFactory.get("checkForFilesStep")
                .tasklet((contribution, chunkContext) -> {
                    Resource[] resources = getResources();
                    if (resources.length == 0) {
                        System.out.println("No files found.");
                        contribution.setExitStatus(new ExitStatus("NO_FILES"));
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

    // Reader for files
    @Bean
    @StepScope
    public ItemStreamReader<Student> studentReader() {
        processedResources.clear();  // Clear the list for every job execution
        Resource[] resources = getResources();

        if (resources.length == 0) { // Return an empty reader if no files are present
            System.out.println("No files found. Using empty reader.");
            return emptyItemReader();
        }

        MultiResourceItemReader<Student> reader = new MultiResourceItemReader<>();
        reader.setResources(resources);
        reader.setDelegate(studentItemReader());
        processedResources.addAll(Arrays.asList(resources));
        return reader;
    }

    // Handle case with no resources
    @Bean
    public ItemStreamReader<Student> emptyItemReader() {
        return new ItemStreamReader<Student>() {
            @Override
            public Student read() {
                return null;
            }

            @Override
            public void open(ExecutionContext executionContext) { }

            @Override
            public void update(ExecutionContext executionContext) { }

            @Override
            public void close() { }
        };
    }

    // Read resources from the directory
    private Resource[] getResources() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(FILE_DIRECTORY);
            System.out.println("Detected files: " + Arrays.toString(resources));
            return resources;
        } catch (IOException e) {
            System.err.println("Error reading resources: " + e.getMessage());
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

    @Bean
    public Jaxb2Marshaller studentUnmarshaller() {
        Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
        unmarshaller.setClassesToBeBound(Student.class);
        return unmarshaller;
    }

    // Processor
    @Bean
    public ItemProcessor<Student, Student> studentItemProcessor() {
        return student -> {
            System.out.println("Processing student: " + student);
            return student;
        };
    }

    // Writer Composite: Currently writing only to the database
//    @Bean
//    public CompositeItemWriter<Student> compositeItemWriter(StudentRepository studentRepository) {
//        CompositeItemWriter<Student> writer = new CompositeItemWriter<>();
//        writer.setDelegates(Collections.singletonList(studentItemWriter(studentRepository))); // Add more writers if needed
//        return writer;
//    }

    // Writer: Write to database
    @Bean
    public ItemWriter<Student> studentItemWriter(StudentRepository studentRepository) {
        return items -> {
            for (Student student : items) {
                System.out.println("Saving student to database: " + student);
                studentRepository.save(student);
            }

            try {
                Thread.sleep(2000);  // Delay after writing each chunk
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Writing delay interrupted", e);
            }
        };
    }
}
