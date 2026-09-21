package com.codesentinel.scan.domain.engine;

import com.codesentinel.scan.domain.detector.ScanDetector;
import com.codesentinel.scan.domain.model.Finding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Điều phối việc chạy toàn bộ {@link ScanDetector} lên cây thư mục đã giải nén.
 * Tuân theo Open/Closed Principle: thêm detector mới chỉ cần khai báo thêm bean
 * {@code @Component implements ScanDetector}, không cần sửa class này.
 */
@Slf4j
@Component
public class ScanEngine {

    private static final Set<String> READABLE_EXTENSIONS = Set.of(
            "java", "js", "ts", "py", "php", "txt", "html", "xml",
            "json", "yml", "yaml", "properties", "sh", "bat"
    );

    private final List<ScanDetector> detectors;

    public ScanEngine(List<ScanDetector> detectors) {

        this.detectors = detectors;

        log.info("Total detectors loaded: {}", detectors.size());

        for (ScanDetector detector : detectors) {

            log.info("Detector registered: {}", detector.getClass().getSimpleName());
        }
    }

    public List<Finding> scan(File root) {

        List<Finding> findings = new ArrayList<>();

        scanRecursive(root, findings);

        return findings;
    }

    private void scanRecursive(File file, List<Finding> findings) {

        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {

            File[] children = file.listFiles();

            if (children != null) {

                for (File child : children) {

                    scanRecursive(child, findings);
                }
            }

            return;
        }

        if (!isReadableFile(file)) {
            return;
        }

        for (ScanDetector detector : detectors) {

            try {

                findings.addAll(detector.detect(file));

            } catch (Exception e) {

                log.error("Detector failed: {}", detector.getClass().getSimpleName(), e);
            }
        }
    }

    private boolean isReadableFile(File file) {

        String fileName = file.getName();

        int lastDot = fileName.lastIndexOf('.');

        if (lastDot < 0) {
            return false;
        }

        String extension = fileName.substring(lastDot + 1).toLowerCase();

        return READABLE_EXTENSIONS.contains(extension);
    }
}
