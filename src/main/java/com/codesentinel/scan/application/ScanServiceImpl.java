package com.codesentinel.scan.application;

import com.codesentinel.scan.domain.engine.ScanEngine;
import com.codesentinel.scan.domain.model.Finding;
import com.codesentinel.scan.domain.model.ScanSummary;
import com.codesentinel.scan.infrastructure.ArchiveExtractor;
import com.codesentinel.scan.infrastructure.FileRiskScanner;
import com.codesentinel.scan.infrastructure.TempWorkspaceCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Điều phối luồng quét: validate -> extract -> scan -> build report -> cleanup.
 * Mỗi bước được uỷ quyền cho một collaborator có trách nhiệm duy nhất (SRP),
 * class này chỉ còn vai trò orchestrator.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanServiceImpl implements ScanService {

    private final UploadValidator uploadValidator;

    private final ArchiveExtractor archiveExtractor;

    private final FileRiskScanner fileRiskScanner;

    private final ScanEngine scanEngine;

    private final ScanReportBuilder scanReportBuilder;

    private final TempWorkspaceCleaner tempWorkspaceCleaner;

    @Override
    public ScanSummary scanZip(MultipartFile file) {

        String scanId = UUID.randomUUID().toString();

        File uploadedFile = null;
        File scanTarget = null;

        try {

            log.info("========== SCAN START ========== scanId={}", scanId);

            uploadValidator.validate(file);

            log.info(
                    "UPLOAD FILE: {} ({} bytes, {})",
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType()
            );

            uploadedFile = storeToTempFile(file);

            scanTarget = archiveExtractor.extract(uploadedFile);

            log.info("ARCHIVE EXTRACTED: {}", scanTarget.getAbsolutePath());

            List<String> suspiciousFiles = fileRiskScanner.scanDangerousFiles(scanTarget);

            List<Finding> findings = scanEngine.scan(scanTarget);

            ScanReportBuilder.Report report = scanReportBuilder.build(scanTarget);

            log.info(
                    "SCAN DONE: {} files, {} findings",
                    report.files().size(),
                    findings.size()
            );

            return ScanSummary.builder()
                    .totalFiles(report.files().size())
                    .extensions(report.extensions())
                    .files(report.files())
                    .suspiciousFiles(suspiciousFiles)
                    .findings(findings)
                    .build();

        } catch (Exception e) {

            log.error("SCAN ERROR | scanId={}", scanId, e);

            throw new RuntimeException("Scan failed", e);

        } finally {

            tempWorkspaceCleaner.cleanup(uploadedFile);
            tempWorkspaceCleaner.cleanup(scanTarget);

            log.info("========== SCAN END ========== scanId={}", scanId);
        }
    }

    private File storeToTempFile(MultipartFile file) throws Exception {

        String uploadDir = System.getProperty("java.io.tmpdir");

        String safeFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        File uploadedFile = new File(uploadDir, safeFileName);

        file.transferTo(uploadedFile);

        return uploadedFile;
    }
}
