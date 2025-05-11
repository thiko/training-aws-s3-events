# Exercise: Integrating Lambda with S3 Event Notifications

## Objective
Learn how to configure an AWS Lambda function to automatically process files as they are uploaded to an S3 bucket. This is a common serverless pattern for event-driven architectures.

## Overview
In this exercise, you'll create a Lambda function that is triggered whenever an object is created in an S3 bucket. The function will analyze the uploaded file's metadata, log information about the file, and optionally perform transformations depending on the file type.

## Steps

### Part 1: Create an S3 Bucket

1. **Navigate to the S3 service**
   - Sign in to the AWS Management Console
   - Go to the S3 service

2. **Create a new bucket**
   - Click "Create bucket"
   - Enter a unique bucket name: `lambda-trigger-bucket-[YourName]`
   - Select the region `eu-central-1` (Frankfurt)
   - Keep default settings for "Block Public Access" (all enabled)
   - Click "Create bucket"

### Part 2: Create a Lambda Function

1. **Navigate to the Lambda service**
   - Go to the Lambda service in the AWS Management Console

2. **Create a new Lambda function**
   - Click "Create function"
   - Select "Author from scratch"
   - Function name: `S3EventProcessor`
   - Runtime: Select "Java 21"
   - Architecture: arm64
   - Execution role: Create a new role with basic Lambda permissions
   - Click "Create function"

3. **Configure Lambda permissions**
   - In the "Configuration" tab, select "Permissions"
   - Click on the role name to open it in the IAM console
   - Add the "AmazonS3ReadOnlyAccess" policy to the role
   - Return to the Lambda function

4. **Provide the Lambda function code**
   - For Java:
     - Create a handler class in your Maven project under `src/main/java/com/tekkminds/awscourse/S3EventProcessor.java`:

```java
package com.tekkminds.awscourse;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class S3EventProcessor implements RequestHandler<S3Event, String> {

    private final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

    @Override
    public String handleRequest(S3Event s3Event, Context context) {
        // Process all records in the event (usually just one)
        for (S3EventNotificationRecord record : s3Event.getRecords()) {
            // Get S3 bucket and object key from the event
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getKey();
            
            context.getLogger().log("Received event for bucket: " + bucket + ", key: " + key);
            
            try {
                // Get object metadata
                ObjectMetadata metadata = s3Client.getObjectMetadata(bucket, key);
                
                // Log file information
                context.getLogger().log("File size: " + metadata.getContentLength() + " bytes");
                context.getLogger().log("Content type: " + metadata.getContentType());
                context.getLogger().log("Last modified: " + metadata.getLastModified());
                
                // Additional processing could be done here based on file type
                if (key.endsWith(".txt")) {
                    context.getLogger().log("Processing text file...");
                    // Add your text processing logic here
                } else if (key.endsWith(".jpg") || key.endsWith(".png")) {
                    context.getLogger().log("Processing image file...");
                    // Add your image processing logic here
                }
                
            } catch (Exception e) {
                context.getLogger().log("Error processing S3 event: " + e.getMessage());
                return "Error: " + e.getMessage();
            }
        }
        
        return "Successfully processed " + s3Event.getRecords().size() + " records.";
    }
}
```

   - Make sure your `pom.xml` includes the necessary dependencies and the Maven Shade plugin:

```xml
<dependencies>
    <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-lambda-java-core</artifactId>
        <version>1.2.3</version>
    </dependency>
    <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-lambda-java-events</artifactId>
        <version>3.15.0</version>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
        <version>2.31.40</version>
        <scope>compile</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.6.0</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

   - Build the JAR file using the provided Maven wrapper:
   ```bash
   ./mvnw clean package
   ```
   - Upload the generated JAR file from the `target` directory to Lambda
   - Set the handler to: `com.tekkminds.awscourse.S3EventProcessor::handleRequest`

### Part 3: Configure S3 Event Trigger

1. **Add a trigger to the Lambda function**
   - In the Lambda function configuration page, go to the "Function overview" section
   - Click "Add trigger"
   - Select "S3" from the dropdown
   - Configure the trigger:
     - Bucket: Select your previously created bucket
     - Event type: Select "All object create events"
     - Prefix: Leave empty (or specify a folder prefix if desired)
     - Suffix: Leave empty (or specify a file extension like `.jpg` if you want to filter)
   - Acknowledge the recursive invocation warning if shown
   - Click "Add"

2. **Verify the trigger configuration**
   - Check that the S3 trigger appears in the function overview
   - You can also verify by going to your S3 bucket properties:
     - Go to the S3 console
     - Select your bucket
     - Go to the "Properties" tab
     - Scroll down to "Event notifications"
     - Confirm that your Lambda function is listed

### Part 4: Test the Integration

1. **Upload a file to the S3 bucket**
   - Go to the S3 console
   - Navigate to your bucket
   - Click "Upload"
   - Select a file to upload (try different file types like .txt, .jpg, .pdf)
   - Click "Upload"

2. **Check Lambda execution**
   - Go back to the Lambda console
   - Select your function
   - Go to the "Monitor" tab
   - Click "View CloudWatch logs"
   - Find the most recent log stream and open it
   - Verify that your logs show the processing of the uploaded file

3. **Test with different file types**
   - Upload different types of files to see how your Lambda function handles them
   - Check the logs for each upload
   - Observe how the function identifies different file types

### Part 5: Optimize Event Filtering

1. **Update S3 event configuration**
   - Go to your S3 bucket
   - Select "Properties" tab
   - Find the event notification you created
   - Click "Edit"
   - Update the configuration to only trigger on specific file types:
     - Add a suffix like `.txt` or `.jpg`
   - Save the changes

2. **Test the filtered events**
   - Upload files that match your filter
   - Upload files that don't match your filter
   - Check the Lambda logs to verify that only the matching files trigger the function

## Verification

You have successfully completed this exercise when:
- Your Lambda function is triggered automatically when files are uploaded to your S3 bucket
- The function correctly extracts and logs metadata about the uploaded files
- You can see different handling logic based on file types
- The event filtering works as expected, triggering the function only for specified file types

## Common Issues and Troubleshooting

1. **Lambda not triggered**: 
   - Check the S3 event notification configuration
   - Verify Lambda execution role has proper permissions
   - Look for error messages in CloudTrail

2. **Permission errors**:
   - Ensure the Lambda execution role has the `AmazonS3ReadOnlyAccess` policy
   - For more advanced processing, you might need additional permissions

3. **Recursive triggers**:
   - If your Lambda function writes back to the same S3 bucket, ensure you're not creating an infinite loop
   - Use prefixes or suffixes to distinguish between input and output files

## Extended Learning

1. **File processing pipeline**:
   - Extend your function to process the content of uploaded files
   - For images: resize or add watermarks using libraries like ImageMagick
   - For text files: perform analysis, counting words, or extract metadata

2. **Multi-step workflow**:
   - Have your Lambda function trigger another AWS service like Step Functions
   - Create a workflow where files are processed in multiple stages

3. **Cross-service integration**:
   - When a file is uploaded, store metadata in DynamoDB
   - Send notifications via SNS when processing is complete
   - Index content in Amazon Elasticsearch for searching