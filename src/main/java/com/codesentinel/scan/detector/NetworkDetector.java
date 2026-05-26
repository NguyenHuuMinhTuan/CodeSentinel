package com.codesentinel.scan.detector;

import com.codesentinel.scan.model.Finding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class NetworkDetector implements ScanDetector {

    private static final List<String> NETWORK_PATTERNS = List.of(

            "http://",
            "https://",

            "socket(",
            "serversocket(",

            "discord.com/api/webhooks",
            "discordapp.com/api/webhooks",

            "api.telegram.org",

            "ngrok",
            "pastebin",
            "webhook",

            "newsocket(",
            "newserversocket(",

            "runtime.getruntime().exec",
            "processbuilder(",

            "httpurlconnection",
            "urlconnection",

            "okhttpclient",
            "resttemplate",
            "webclient",

            "curl ",
            "wget ",

            "inetaddress",
            "datagramsocket",
            "multicastsocket"
    );

    @Override
    public List<Finding> detect(File file) {

        List<Finding> findings = new ArrayList<>();

        try {

            // =========================
            // validate
            // =========================

            if (file == null
                    || !file.exists()
                    || file.isDirectory()) {

                return findings;
            }

            // =========================
            // read file
            // =========================

            byte[] bytes =
                    Files.readAllBytes(file.toPath());

            if (bytes.length == 0) {
                return findings;
            }

            String content =
                    new String(
                            bytes,
                            StandardCharsets.UTF_8
                    );

            // =========================
            // normalize
            // =========================

            String normalizedContent =
                    normalize(content);

            String decodedBase64 =
                    tryDecodeBase64(content);

            String mergedContent =
                    normalizedContent
                            + "\n"
                            + normalize(decodedBase64);

            // =========================
            // scan patterns
            // =========================

            for (String pattern : NETWORK_PATTERNS) {

                String normalizedPattern =
                        normalize(pattern);

                boolean matched =
                        mergedContent.contains(
                                normalizedPattern
                        );

                if (!matched) {
                    continue;
                }

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
    // severity
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
    // extract snippet
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
                    Math.max(
                            0,
                            index - 40
                    );

            int end =
                    Math.min(
                            content.length(),
                            index + 120
                    );

            return content.substring(
                    start,
                    end
            );

        } catch (Exception e) {

            return "";
        }
    }

    // =========================
    // decode base64
    // =========================

    private String tryDecodeBase64(
            String content
    ) {

        try {

            String cleaned =
                    content.replaceAll(
                            "[^A-Za-z0-9+/=]",
                            ""
                    );

            if (cleaned.length() < 16) {
                return "";
            }

            byte[] decoded =
                    Base64.getDecoder()
                            .decode(cleaned);

            return new String(
                    decoded,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            return "";
        }
    }
}