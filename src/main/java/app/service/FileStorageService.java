package app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads/complaints}")
    private String uploadDir;

    // =====================================================
    // SAVE COMPLAINT IMAGE
    // =====================================================

    public String saveImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException(
                    "Complaint image is compulsory."
            );
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !contentType.toLowerCase().startsWith("image/")) {

            throw new RuntimeException(
                    "Only image files are allowed."
            );
        }

        // -------------------------------------------------
        // ALLOWED IMAGE TYPES
        // -------------------------------------------------

        String extension = getExtension(file);

        if (!extension.equals(".jpg")
                && !extension.equals(".jpeg")
                && !extension.equals(".png")
                && !extension.equals(".webp")) {

            throw new RuntimeException(
                    "Only JPG, JPEG, PNG and WEBP images are allowed."
            );
        }

        try {

            // -------------------------------------------------
            // CREATE UPLOAD DIRECTORY
            // -------------------------------------------------

            Path uploadPath = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadPath);

            // -------------------------------------------------
            // UNIQUE FILE NAME
            // -------------------------------------------------

            String fileName = UUID.randomUUID()
                    .toString()
                    + extension;

            Path targetPath = uploadPath
                    .resolve(fileName)
                    .normalize();

            // -------------------------------------------------
            // PATH TRAVERSAL PROTECTION
            // -------------------------------------------------

            if (!targetPath.startsWith(uploadPath)) {

                throw new RuntimeException(
                        "Invalid file path."
                );
            }

            // -------------------------------------------------
            // COPY IMAGE
            // IMPORTANT:
            // try-with-resources closes the InputStream
            // -------------------------------------------------

            try (InputStream inputStream = file.getInputStream()) {

                Files.copy(
                        inputStream,
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            // -------------------------------------------------
            // RETURN URL
            // -------------------------------------------------

            return "/uploads/complaints/" + fileName;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to store complaint image.",
                    e
            );
        }
    }

    // =====================================================
    // SAVE MULTIPLE COMPLAINT IMAGES
    // =====================================================

    public String saveImages(java.util.List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            throw new RuntimeException("Complaint image/proof is compulsory.");
        }

        java.util.List<String> savedUrls = new java.util.ArrayList<>();

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                savedUrls.add(saveImage(file));
            }
        }

        if (savedUrls.isEmpty()) {
            throw new RuntimeException("Complaint image/proof is compulsory.");
        }

        return String.join(",", savedUrls);
    }

    // =====================================================
    // GET FILE EXTENSION
    // =====================================================

    private String getExtension(MultipartFile file) {

        String originalFilename =
                file.getOriginalFilename();

        if (!StringUtils.hasText(originalFilename)) {

            throw new RuntimeException(
                    "Image filename is missing."
            );
        }

        String cleanName =
                StringUtils.cleanPath(originalFilename);

        int lastDot =
                cleanName.lastIndexOf('.');

        if (lastDot < 0
                || lastDot == cleanName.length() - 1) {

            throw new RuntimeException(
                    "Image file extension is missing."
            );
        }

        return cleanName
                .substring(lastDot)
                .toLowerCase();
    }
}