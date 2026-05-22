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

    // =========================
    // scan engine injection
    // =========================

    private final ScanEngine scanEngine;

    @Override
    public ScanSummary scanZip(
            MultipartFile file
    ) {

        try {

            // =========================
            // 1. save uploaded zip
            // =========================

            String uploadDir =
                    System.getProperty(
                            "java.io.tmpdir"
                    );

            File tempZip =
                    new File(
                            uploadDir,
                            file.getOriginalFilename()
                    );

            file.transferTo(tempZip);

            log.info(
                    "ZIP SAVED: {}",
                    tempZip.getAbsolutePath()
            );

            // =========================
            // 2. extract zip
            // =========================

            String extractDir =
                    uploadDir +
                            "/extract_" +
                            UUID.randomUUID();

            ZipUtil.extractZip(
                    tempZip.getAbsolutePath(),
                    extractDir
            );

            File extractFolder =
                    new File(extractDir);

            log.info(
                    "ZIP EXTRACTED: {}",
                    extractFolder.getAbsolutePath()
            );

            // =========================
            // 3. scan dangerous extensions
            // =========================

            List<String> suspiciousFiles =
                    FileRiskUtil.scanDangerousFiles(
                            extractFolder
                    );

            // =========================
            // 4. scan detectors
            // =========================

            List<Finding> findings =
                    scanEngine.scan(
                            extractFolder
                    );

            // =========================
            // 5. read project files
            // =========================

            List<FileInfo> files =
                    new ArrayList<>();

            Map<String, Integer> extensions =
                    new HashMap<>();

            try (Stream<Path> paths =
                         Files.walk(
                                 Path.of(extractDir)
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

            // =========================
            // 6. build response
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
    // helper method
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