package com.codesentinel.scan.application;

import com.codesentinel.shared.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Kiểm tra hợp lệ file upload trước khi đưa vào pipeline quét (SRP:
 * tách khỏi ScanServiceImpl, vốn trước đây vừa validate vừa extract vừa scan).
 */
@Component
public class UploadValidator {

    private static final long MAX_UPLOAD_SIZE = 100L * 1024 * 1024; // 100MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "zip",
            "rar",

            "java",
            "js",
            "py",
            "php",
            "html",
            "xml",
            "json",
            "yml",
            "yaml",
            "txt"
    );

    public void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new BadRequestException("File too large");
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new BadRequestException("Invalid file name");
        }

        String extension = FileExtensions.extractExtension(originalName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("File type not allowed: " + extension);
        }
    }
}
