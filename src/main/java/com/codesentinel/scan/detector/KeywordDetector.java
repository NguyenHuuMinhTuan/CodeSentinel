package com.codesentinel.scan.detector;

import com.codesentinel.scan.model.Finding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KeywordDetector implements ScanDetector {

    private static final List<String> DANGEROUS_KEYWORDS =
            List.of(

                    "Runtime.getRuntime()",
                    "ProcessBuilder",
                    "powershell",
                    "cmd.exe",
                    "wget",
                    "curl",
                    "rm -rf",
                    "eval("
            );

    @Override
    public List<Finding> detect(File file) {

        List<Finding> findings =
                new ArrayList<>();

        try {

            // =========================
            // đọc toàn bộ nội dung file
            // =========================

            String content =
                    Files.readString(file.toPath());

            // =========================
            // normalize
            // =========================

            String normalized =
                    content.toLowerCase()
                            .replaceAll("\\s+", "");

            // =========================
            // scan keyword
            // =========================

            for (String keyword : DANGEROUS_KEYWORDS) {

                String normalizedKeyword =
                        keyword.toLowerCase()
                                .replaceAll("\\s+", "");

                if (normalized.contains(
                        normalizedKeyword
                )) {

                    findings.add(
                            Finding.builder()
                                    .file(
                                            file.getAbsolutePath()
                                    )
                                    .type(
                                            "DANGEROUS_KEYWORD"
                                    )
                                    .severity("HIGH")
                                    .matchedKeyword(keyword)
                                    .detector(
                                            "KeywordDetector"
                                    )
                                    .build()
                    );

                    log.warn(
                            "Dangerous keyword detected: {} in {}",
                            keyword,
                            file.getName()
                    );
                }
            }

        } catch (Exception e) {

            log.error(
                    "KeywordDetector error: {}",
                    file.getAbsolutePath(),
                    e
            );
        }

        return findings;
    }
}