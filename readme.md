# Spring Batch Demo

This is a Spring Batch application that processes student and teacher data. It uses batch jobs for file processing and implements a job locking mechanism to avoid concurrent executions.

## Key Features

- **Job Locking:** Ensures that only one instance of a job runs at a time.
- **Batch Processing:** Processes student and teacher data in chunks.
- **XML File Processing:** Reads and processes XML files containing student and teacher data.

## Build and Run

1. **Build the Project:**  
   Use the Maven wrapper included in the project.
   ```sh
   ./mvnw clean install
   ```
