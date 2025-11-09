package com.skrepta.skreptajava.category.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryIconService {

    private final AmazonS3 s3client;

    @Value("${ps.bucket-name}")
    private String bucketName;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".png", ".jpg", ".jpeg", ".webp", ".svg");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String ICONS_FOLDER = "categories/icons/";

    /**
     * Загружает иконку категории в S3
     * @param file файл для загрузки
     * @param categoryId ID категории
     * @return публичный URL загруженного файла
     */
    public String uploadCategoryIcon(MultipartFile file, Long categoryId) throws IOException {
        validateFile(file);

        String fileExtension = getFileExtension(file.getOriginalFilename());
        String uniqueFileName = ICONS_FOLDER + "category_" + categoryId + "_" + UUID.randomUUID().toString() + fileExtension;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        PutObjectRequest request = new PutObjectRequest(bucketName, uniqueFileName, file.getInputStream(), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead);

        s3client.putObject(request);

        String fileUrl = s3client.getUrl(bucketName, uniqueFileName).toString();
        log.info("Category icon uploaded successfully: {}", fileUrl);

        return fileUrl;
    }

    /**
     * Удаляет иконку категории из S3
     * @param fileUrl полный URL файла
     */
    public void deleteCategoryIcon(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            // Добавляем папку обратно
            String key = ICONS_FOLDER + fileName;
            
            s3client.deleteObject(bucketName, key);
            log.info("Category icon deleted successfully: {}", key);
        } catch (Exception e) {
            log.error("Error deleting category icon from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to delete category icon from S3", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Размер файла превышает максимально допустимый (5MB)");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Отсутствует имя файла");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Недопустимый тип файла. Разрешены: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}