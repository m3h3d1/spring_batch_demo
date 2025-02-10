package com.example.batch.demo.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileMovingStepExecutionListener extends StepExecutionListenerSupport {

    private final List<Resource> processedResources;

    public FileMovingStepExecutionListener(List<Resource> processedResources) {
        this.processedResources = processedResources;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        for (Resource resource : new ArrayList<>(processedResources)) {
            try {
                if (!resource.exists()) {
                    System.out.println("File " + resource.getFilename() + " does not exist, skipping move.");
                    continue; // Skip non-existent files
                }
                Path sourcePath = Paths.get(resource.getURI());
                Path targetDir = sourcePath.getParent().resolve("done");
                Files.createDirectories(targetDir);
                Path targetPath = targetDir.resolve(sourcePath.getFileName());

                // Handle possible file name conflicts
                if (Files.exists(targetPath)) {
                    String fileName = sourcePath.getFileName().toString();
                    int dotIndex = fileName.lastIndexOf('.');
                    String baseName = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
                    String extension = (dotIndex == -1) ? "" : fileName.substring(dotIndex);
                    targetPath = targetDir.resolve(baseName + "_" + System.currentTimeMillis() + extension);
                }

                Files.move(sourcePath, targetPath); // Move file to "done" folder
                System.out.println("Moved file from " + sourcePath + " to " + targetPath);
            } catch (IOException e) {
                System.out.println("Error moving file " + resource.getFilename() + ": " + e.getMessage());
            }
        }
        processedResources.clear();
        return stepExecution.getExitStatus();
    }
}
