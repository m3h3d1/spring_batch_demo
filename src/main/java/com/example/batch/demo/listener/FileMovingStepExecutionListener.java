package com.example.batch.demo.listener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.listener.ChunkListenerSupport;
import org.springframework.core.io.Resource;

public class FileMovingStepExecutionListener extends ChunkListenerSupport {

    private final List<Resource> processedResources;

    public FileMovingStepExecutionListener(List<Resource> processedResources) {
        this.processedResources = processedResources;
    }

    // @Override
    // public ExitStatus afterStep(StepExecution stepExecution) {

    //     return stepExecution.getExitStatus();
    // }

    @Override
    public void beforeChunk(org.springframework.batch.core.scope.context.ChunkContext context) {
        // Actions to perform before each chunk
        System.out.println("Before chunk: " + context.getStepContext().getStepName());
    }

    @Override
    public void afterChunk(org.springframework.batch.core.scope.context.ChunkContext context) {
        // Actions to perform after each chunk
        System.out.println("After chunk: " + context.getStepContext().getStepName());

        for (Resource resource : new ArrayList<>(processedResources)) {
            try {
                if (!resource.exists()) {
                    System.out.println("File " + resource.getFilename() + " does not exist, skipping move.");
                    continue;
                }
                Path sourcePath = Paths.get(resource.getURI());
                Path targetPath = sourcePath.getParent().resolve("done").resolve(sourcePath.getFileName());
                Files.createDirectories(targetPath.getParent());

                // Handle file conflicts
                if (Files.exists(targetPath)) {
                    String fileName = sourcePath.getFileName().toString();
                    int dotIndex = fileName.lastIndexOf('.');
                    String baseName = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
                    String extension = (dotIndex == -1) ? "" : fileName.substring(dotIndex);
                    targetPath = targetPath.getParent().resolve(baseName + "_" + System.currentTimeMillis() + extension);
                }

                Files.move(sourcePath, targetPath); // Move file
                System.out.println("Moved file from " + sourcePath + " to " + targetPath);
            } catch (IOException e) {
                System.err.println("Error moving file " + resource.getFilename() + ": " + e.getMessage());
            }
        }
        processedResources.clear();
    }

    @Override
    public void afterChunkError(org.springframework.batch.core.scope.context.ChunkContext context) {
        // Actions to perform in case of chunk error
        System.err.println("Error in chunk: " + context.getStepContext().getStepName());
    }
}