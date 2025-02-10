package com.example.batch.demo.config;

import com.example.batch.demo.listener.FileMovingStepExecutionListener;
import com.example.batch.demo.model.Student;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    private List<Resource> processedResources = new ArrayList<>();

    @Bean
    public Job importStudentJob() {
        return jobBuilderFactory.get("importStudentJob")
                .start(checkForFilesStep())
                .on("NO_FILES")  // Custom status if no files are found
                .to(noOpStep())  // Skip processing if no files are detected
                .from(checkForFilesStep())
                .on("*")         // Process normally if files are found
                .to(studentStep())
                .end()
                .build();
    }

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

    @Bean
    public Step studentStep() {
        return stepBuilderFactory.get("studentStep")
                .<Student, Student>chunk(5)
                .reader(studentReader())
                .processor(studentItemProcessor())
                .writer(compositeItemWriter())
                .listener(new FileMovingStepExecutionListener(processedResources))
                .build();
    }

    @Bean
    public Step noOpStep() {
        return stepBuilderFactory.get("noOpStep")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("No files found. Skipping processing.");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<Student> studentReader() {
        // Clears the processed files list for the current job execution
        processedResources.clear();

        Resource[] resources = getResources();
        if (resources.length == 0) {
            System.out.println("No files found. Using empty reader.");
            return emptyItemReader();
        }

        MultiResourceItemReader<Student> reader = new MultiResourceItemReader<>();
        reader.setResources(resources); // Dynamically assign resources
        reader.setDelegate(studentItemReader());
        processedResources.addAll(Arrays.asList(resources));
        return reader;
    }

    @Bean
    public ItemStreamReader<Student> emptyItemReader() {
        return new ItemStreamReader<Student>() {
            @Override
            public Student read() {
                return null;
            }
            @Override
            public void open(ExecutionContext executionContext) throws ItemStreamException { }
            @Override
            public void update(ExecutionContext executionContext) throws ItemStreamException { }
            @Override
            public void close() throws ItemStreamException { }
        };
    }

//    @Bean
    public Resource[] getResources() {
        try {
            // Fetch all XML files in the directory
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("file:D:/Integrations/batch-test/students/*.xml");
            System.out.println("Detected files: " + Arrays.toString(resources));
            return resources;
        } catch (IOException e) {
            System.out.println("Error reading resources: " + e);
            return new Resource[0]; // Return an empty array if an error occurs
        }
    }

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

    @Bean
    public ItemProcessor<Student, Student> studentItemProcessor() {
        return student -> {
            System.out.println("Processing student: " + student);
            return student;
        };
    }

    @Bean
    public ItemWriter<Student> studentItemWriter() {
        return items -> {
            for (Student student : items) {
                System.out.println("Writing student to database: " + student);
            }
        };
    }

    @Bean
    public ItemWriter<Student> fileMovingItemWriter() {
        return items -> {
            System.out.println("Moving file after processing");
            for (Resource resource : processedResources) {
                try {
                    Path sourcePath = Paths.get(resource.getURI());
                    Path targetPath = sourcePath.getParent().resolve("done").resolve(sourcePath.getFileName());
                    Files.createDirectories(targetPath.getParent());
                    Files.move(sourcePath, targetPath);
                    System.out.println("Moved file from " + sourcePath + " to " + targetPath);
                } catch (IOException e) {
                    System.out.println("Error moving file: " + e);
                }
            }
            processedResources.clear();
        };
    }

    @Bean
    public CompositeItemWriter<Student> compositeItemWriter() {
        CompositeItemWriter<Student> writer = new CompositeItemWriter<>();
        writer.setDelegates(Arrays.asList(studentItemWriter()));
        // writer.setDelegates(Arrays.asList(studentItemWriter(),
        // fileMovingItemWriter()));
        return writer;
    }
}