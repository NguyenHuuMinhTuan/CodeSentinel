package com.codesentinel.scan.service.impl;

import com.codesentinel.scan.engine.ScanEngine;
import com.codesentinel.scan.model.FileInfo;
import com.codesentinel.scan.model.Finding;
import com.codesentinel.scan.model.ScanSummary;
import com.codesentinel.scan.service.ScanService;
import com.codesentinel.scan.util.ArchiveUtil;
import com.codesentinel.scan.util.FileRiskUtil;
import com.codesentinel.scan.util.ZipUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanServiceImpl implements ScanService {

    private final ScanEngine scanEngine;

    // =========================
    // CONFIG
    // =========================

    private static final long MAX_UPLOAD_SIZE =
            100L * 1024 * 1024; // 100MB

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(
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

    @Override
    public ScanSummary scanZip(
            MultipartFile file
    ) {

        File uploadedFile = null;
        File scanTarget = null;

        String scanId =
                UUID.randomUUID().toString();

        try {

            log.info("========== SCAN START ==========");
            log.info("SCAN ID: {}", scanId);

            // =========================
            // validate upload
            // =========================

            validateUpload(file);

            String originalName =
                    file.getOriginalFilename();

            log.info("UPLOAD FILE: {}", originalName);
            log.info("UPLOAD SIZE: {}", file.getSize());
            log.info("CONTENT TYPE: {}", file.getContentType());

            // =========================
            // prepare upload path
            // =========================

            String uploadDir =
                    System.getProperty(
                            "java.io.tmpdir"
                    );

            String safeFileName =
                    UUID.randomUUID()
                            + "_"
                            + originalName;

            uploadedFile =
                    new File(
                            uploadDir,
                            safeFileName
                    );


            file.transferTo(uploadedFile);

                String extractDir =
                        ArchiveUtil.extract(
                                uploadedFile.getAbsolutePath()
                        );

                ZipUtil.extractZip(
                        uploadedFile.getAbsolutePath(),
                        extractDir
                );

                scanTarget =
                        new File(extractDir);

                log.info(
                        "ZIP EXTRACTED: {}",
                        scanTarget.getAbsolutePath()
                );



            // =========================
            // suspicious file scan
            // =========================

            List<String> suspiciousFiles =
                    FileRiskUtil.scanDangerousFiles(
                            scanTarget
                    );

            // =========================
            // detector scan
            // =========================

            List<Finding> findings =
                    scanEngine.scan(
                            scanTarget
                    );

            // =========================
            // build file info
            // =========================

            List<FileInfo> files =
                    new ArrayList<>();

            Map<String, Integer> extensions =
                    new HashMap<>();

            buildFileInfos(
                    scanTarget,
                    files,
                    extensions
            );

            log.info(
                    "TOTAL FILES: {}",
                    files.size()
            );

            log.info(
                    "TOTAL FINDINGS: {}",
                    findings.size()
            );

            log.info("========== SCAN END ==========");

            // =========================
            // response
            // =========================

            return ScanSummary.builder()
                    .totalFiles(
                            files.size()
                    )
                    .extensions(
                            extensions
                    )
                    .files(
                            files
                    )
                    .suspiciousFiles(
                            suspiciousFiles
                    )
                    .findings(
                            findings
                    )
                    .build();

        } catch (Exception e) {

            log.error(
                    "SCAN ERROR | scanId={}",
                    scanId,
                    e
            );

            throw new RuntimeException(
                    "Scan failed",
                    e
            );

        } finally {

            // =========================
            // cleanup temp files
            // =========================

            cleanup(uploadedFile);

            if (
                    scanTarget != null
                            && scanTarget.isDirectory()
            ) {
                cleanup(scanTarget);
            }
        }
    }

    // =========================
    // VALIDATE
    // =========================

    private void validateUpload(
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {

            throw new RuntimeException(
                    "Uploaded file is empty"
            );
        }

        if (file.getSize() > MAX_UPLOAD_SIZE) {

            throw new RuntimeException(
                    "File too large"
            );
        }

        String originalName =
                file.getOriginalFilename();

        if (
                originalName == null
                        || originalName.isBlank()
        ) {

            throw new RuntimeException(
                    "Invalid file name"
            );
        }

        String extension =
                extractExtension(
                        originalName
                );

        if (
                !ALLOWED_EXTENSIONS.contains(
                        extension
                )
        ) {

            throw new RuntimeException(
                    "File type not allowed: "
                            + extension
            );
        }
    }

    // =========================
    // BUILD FILE INFO
    // =========================

    private void buildFileInfos(
            File scanTarget,
            List<FileInfo> files,
            Map<String, Integer> extensions
    ) throws Exception {

        if (scanTarget.isFile()) {

            String fileName =
                    scanTarget.getName();

            String extension =
                    extractExtension(fileName);

            extensions.put(
                    extension,
                    1
            );

            files.add(
                    FileInfo.builder()
                            .fileName(fileName)
                            .extension(extension)
                            .path(
                                    scanTarget.getAbsolutePath()
                            )
                            .build()
            );

            return;
        }

        try (
                Stream<Path> paths =
                        Files.walk(
                                scanTarget.toPath()
                        )
        ) {

            paths
                    .filter(Files::isRegularFile)
                    .forEach(path -> {

                        String fileName =
                                path.getFileName()
                                        .toString();

                        String extension =
                                extractExtension(
                                        fileName
                                );

                        extensions.put(
                                extension,
                                extensions.getOrDefault(
                                        extension,
                                        0
                                ) + 1
                        );

                        files.add(
                                FileInfo.builder()
                                        .fileName(fileName)
                                        .extension(extension)
                                        .path(
                                                path.toString()
                                        )
                                        .build()
                        );
                    });
        }
    }

    // =========================
    // CLEANUP
    // =========================

    private void cleanup(File file) {

        try {

            if (
                    file == null
                            || !file.exists()
            ) {
                return;
            }

            if (file.isDirectory()) {

                Files.walk(file.toPath())
                        .sorted(
                                Comparator.reverseOrder()
                        )
                        .map(Path::toFile)
                        .forEach(File::delete);

            } else {

                file.delete();
            }

        } catch (Exception e) {

            log.error(
                    "Cleanup failed: {}",
                    file.getAbsolutePath(),
                    e
            );
        }
    }

    // =========================
    // HELPER
    // =========================

    private String extractExtension(
            String fileName
    ) {

        int lastDot =
                fileName.lastIndexOf(".");

        if (lastDot < 0) {
            return "";
        }

        return fileName
                .substring(lastDot + 1)
                .toLowerCase();
    }
}