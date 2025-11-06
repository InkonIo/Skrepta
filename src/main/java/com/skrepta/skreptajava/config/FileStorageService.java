package com.skrepta.skreptajava.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;

@Service
public class FileStorageService {

    private AmazonS3 s3client;

    @Value("${ps.access-key-id}")
    private String accessKey;

    @Value("${ps.secret-access-key}")
    private String secretKey;

    @Value("${ps.endpoint-url}")
    private String endpointUrl;

    @Value("${ps.bucket-name}")
    private String bucketName;

    @PostConstruct
    private void initializeAmazon() {
        AWSCredentials credentials = new BasicAWSCredentials(this.accessKey, this.secretKey);
        this.s3client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(this.endpointUrl, "us-east-1"))
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        PutObjectRequest request = new PutObjectRequest(bucketName, uniqueFileName, file.getInputStream(), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead);

        s3client.putObject(request);

        return s3client.getUrl(bucketName, uniqueFileName).toString();
    }

    public void deleteFile(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        s3client.deleteObject(bucketName, fileName);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
