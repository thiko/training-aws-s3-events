package com.tekkminds.awscourse;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

public class S3EventProcessor implements RequestHandler<S3Event, String> {

    private final S3Client s3Client = S3Client.create();

    @Override
    public String handleRequest(S3Event s3Event, Context context) {
        // Process all records in the event (usually just one)
        for (S3EventNotificationRecord record : s3Event.getRecords()) {
            // Get S3 bucket and object key from the event
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getKey();

            context.getLogger().log("Received event for bucket: " + bucket + ", key: " + key);

            try {
                // Get object metadata (called HeadObject in SDK v2)
                HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();

                HeadObjectResponse objectMetadata = s3Client.headObject(headObjectRequest);

                // Log file information
                context.getLogger().log("File size: " + objectMetadata.contentLength() + " bytes");
                context.getLogger().log("Content type: " + objectMetadata.contentType());
                context.getLogger().log("Last modified: " + objectMetadata.lastModified());

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
