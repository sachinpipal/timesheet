package com.example.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureFileUploadConfig {
    @Autowired
    private AzureFileUploadConfigurationProperties azureFileUploadConfigurationProperties;

    @Bean
    public BlobContainerClient blobContainerClient() {

        // Building Blob Service Client..
        BlobServiceClient blobServiceClient =
                new BlobServiceClientBuilder().connectionString(azureFileUploadConfigurationProperties.getConnectionString()).buildClient();

        // Building Blob Container Client..
        BlobContainerClient blobContainerClient =
                blobServiceClient.getBlobContainerClient(azureFileUploadConfigurationProperties.getContainerName());

        return blobContainerClient;

    }

    public AzureFileUploadConfigurationProperties getAzureFileUploadConfigurationProperties() {
        return azureFileUploadConfigurationProperties;
    }

}
