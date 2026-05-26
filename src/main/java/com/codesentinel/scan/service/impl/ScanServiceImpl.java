package com.codesentinel.scan.service.impl;

import com.codesentinel.scan.engine.ScanEngine;
import com.codesentinel.scan.model.FileInfo;
import com.codesentinel.scan.model.Finding;
import com.codesentinel.scan.model.ScanSummary;
import com.codesentinel.scan.service.ScanService;
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

    @Override
    public ScanSummary scanZip(
            MultipartFile file
    ) {

        try {

            // =========================
            // validate upload
            // =========================

            if (file == null || file.isEmpty()) {

                throw new RuntimeException(
                        "Uploaded file is empty"
                );
            }

            String originalName =
                    file.getOriginalFilename();

            if (originalName == null || originalName.isBlank()) {

                throw new RuntimeException(
                        "Invalid file name"
                );
            }

            log.info("========== SCAN START ==========");
            log.info("UPLOAD FILE: {}", originalName);
            log.info("UPLOAD SIZE: {}", file.getSize());

            log.info("ORIGINAL FILE: {}", originalName);
            log.info("CONTENT TYPE: {}", file.getContentType());

            // =========================
            // prepare temp paths
            // =========================

            String uploadDir =
                    System.getProperty(
                            "java.io.tmpdir"
                    );

            String safeFileName =
                    UUID.randomUUID()
                            + "_"
                            + originalName;

            File uploadedFile =
                    new File(
                            uploadDir,
                            safeFileName
                    );

            // =========================
            // save uploaded file
            // =========================

            file.transferTo(uploadedFile);

            log.info(
                    "FILE SAVED: {}",
                    uploadedFile.getAbsolutePath()
            );

            log.info(
                    "SAVED FILE SIZE: {} bytes",
                    uploadedFile.length()
            );

            // =========================
            // detect zip or single file
            // =========================

            boolean isZip =
                    originalName
                            .toLowerCase()
                            .endsWith(".zip");

            File scanTarget;

            if (isZip) {

                // =========================
                // extract zip
                // =========================

                String extractDir =
                        uploadDir
                                + File.separator
                                + "extract_"
                                + UUID.randomUUID();

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

            } else {

                // =========================
                // single file scan
                // =========================

                scanTarget =
                        uploadedFile;

                log.info(
                        "SINGLE FILE MODE ENABLED"
                );
            }

            // =========================
            // suspicious extension scan
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
            // build file list
            // =========================

            List<FileInfo> files =
                    new ArrayList<>();

            Map<String, Integer> extensions =
                    new HashMap<>();

            if (scanTarget.isFile()) {

                // =========================
                // single file info
                // =========================

                String fileName =
                        scanTarget.getName();

                String extension =
                        extractExtension(
                                fileName
                        );

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

            } else {

                // =========================
                // folder scan info
                // =========================

                try (Stream<Path> paths =
                             Files.walk(
                                     scanTarget.toPath()
                             )) {

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
                    "SCAN ERROR",
                    e
            );

            throw new RuntimeException(
                    "Scan failed",
                    e
            );
        }
    }

    // =========================
    // helper
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