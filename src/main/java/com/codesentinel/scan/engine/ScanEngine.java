package com.codesentinel.scan.engine;

import com.codesentinel.scan.detector.ScanDetector;
import com.codesentinel.scan.model.Finding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ScanEngine {

    private final List<ScanDetector> detectors;

    public ScanEngine(
            List<ScanDetector> detectors
    ) {

        this.detectors = detectors;

        log.warn(
                "TOTAL DETECTORS LOADED: {}",
                detectors.size()
        );

        for (ScanDetector detector
                : detectors) {

            log.warn(
                    "DETECTOR: {}",
                    detector.getClass()
                            .getSimpleName()
            );
        }
    }

    // =========================
    // main scan
    // =========================

    public List<Finding> scan(
            File root
    ) {

        List<Finding> findings =
                new ArrayList<>();

        scanRecursive(
                root,
                findings
        );

        return findings;
    }

    // =========================
    // recursive scan
    // =========================

    private void scanRecursive(
            File file,
            List<Finding> findings
    ) {

        if (file == null || !file.exists()) {
            return;
        }

        // =========================
        // folder
        // =========================

        if (file.isDirectory()) {

            File[] children =
                    file.listFiles();

            if (children != null) {

                for (File child : children) {

                    scanRecursive(
                            child,
                            findings
                    );
                }
            }

            return;
        }

        // =========================
        // skip unreadable files
        // =========================

        if (!isReadableFile(file)) {
            return;
        }

        // =========================
        // run detectors
        // =========================

        for (ScanDetector detector : detectors) {

            try {

                log.info(
                        "Running detector: {} on {}",
                        detector.getClass()
                                .getSimpleName(),
                        file.getName()
                );

                findings.addAll(
                        detector.detect(file)
                );

            } catch (Exception e) {

                log.error(
                        "Detector failed: {}",
                        detector.getClass()
                                .getSimpleName(),
                        e
                );
            }
        }
    }

    // =========================
    // readable files only
    // =========================

    private boolean isReadableFile(
            File file
    ) {

        String lower =
                file.getName()
                        .toLowerCase();

        return lower.endsWith(".java")
                || lower.endsWith(".js")
                || lower.endsWith(".ts")
                || lower.endsWith(".py")
                || lower.endsWith(".php")
                || lower.endsWith(".txt")
                || lower.endsWith(".html")
                || lower.endsWith(".xml")
                || lower.endsWith(".json")
                || lower.endsWith(".yml")
                || lower.endsWith(".yaml")
                || lower.endsWith(".properties")
                || lower.endsWith(".sh")
                || lower.endsWith(".bat");
    }
}