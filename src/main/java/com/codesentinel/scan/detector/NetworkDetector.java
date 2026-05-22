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
public class NetworkDetector implements ScanDetector {

    // =========================
    // suspicious network keywords
    // =========================

    private static final List<String> NETWORK_PATTERNS =
            List.of(

                    "http://",
                    "https://",

                    "socket(",
                    "serversocket(",

                    "discord.com/api/webhooks",
                    "discordapp.com/api/webhooks",

                    "api.telegram.org",

                    "ngrok",

                    "pastebin",

                    "webhook"
            );

    @Override
    public List<Finding> detect(File file) {

        List<Finding> findings =
                new ArrayList<>();

        try {

            // =========================
            // đọc toàn bộ file
            // =========================

            String content =
                    Files.readString(file.toPath());

            // =========================
            // normalize
            // =========================

            String normalized =
                    normalize(content);

            // =========================
            // scan patterns
            // =========================

            for (String pattern : NETWORK_PATTERNS) {

                String normalizedPattern =
                        normalize(pattern);

                if (normalized.contains(
                        normalizedPattern
                )) {

                    findings.add(
                            Finding.builder()
                                    .file(
                                            file.getAbsolutePath()
                                    )
                                    .type(
                                            "NETWORK_ACCESS"
                                    )
                                    .severity(
                                            calculateSeverity(
                                                    pattern
                                            )
                                    )
                                    .matchedKeyword(
                                            pattern
                                    )
                                    .codeSnippet(
                                            extractSnippet(
                                                    content,
                                                    pattern
                                            )
                                    )
                                    .detector(
                                            "NetworkDetector"
                                    )
                                    .build()
                    );

                    log.warn(
                            "Network pattern detected: {} in {}",
                            pattern,
                            file.getName()
                    );
                }
            }

        } catch (Exception e) {

            log.error(
                    "NetworkDetector error: {}",
                    file.getAbsolutePath(),
                    e
            );
        }

        return findings;
    }

    // =========================
    // normalize content
    // =========================

    private String normalize(String content) {

        if (content == null) {
            return "";
        }

        return content
                .toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll(
                        "[\\u200B-\\u200D\\uFEFF]",
                        ""
                );
    }

    // =========================
    // severity calculation
    // =========================

    private String calculateSeverity(
            String pattern
    ) {

        String lower =
                pattern.toLowerCase();

        if (lower.contains("webhook")
                || lower.contains("telegram")
                || lower.contains("ngrok")) {

            return "HIGH";
        }

        if (lower.contains("socket")) {

            return "MEDIUM";
        }

        return "LOW";
    }

    // =========================
    // extract matched snippet
    // =========================

    private String extractSnippet(
            String content,
            String keyword
    ) {

        try {

            int index =
                    content.toLowerCase()
                            .indexOf(
                                    keyword.toLowerCase()
                            );

            if (index < 0) {
                return "";
            }

            int start =
                    Math.max(0, index - 40);

            int end =
                    Math.min(
                            content.length(),
                            index + 80
                    );

            return content.substring(
                    start,
                    end
            );

        } catch (Exception e) {

            return "";
        }
    }
}