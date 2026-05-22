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
public class ReflectionDetector implements ScanDetector {

    // =========================
    // reflection patterns
    // =========================

    private static final List<String> REFLECTION_PATTERNS =
            List.of(

                    "Class.forName",

                    "getDeclaredMethod",
                    "getDeclaredMethods",

                    "getDeclaredField",
                    "getDeclaredFields",

                    "setAccessible(true)",

                    "Method.invoke",
                    "invoke(",

                    "ClassLoader",

                    "defineClass",

                    "java.lang.reflect"
            );

    @Override
    public List<Finding> detect(File file) {

        List<Finding> findings =
                new ArrayList<>();

        try {

            // =========================
            // read file content
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

            for (String pattern : REFLECTION_PATTERNS) {

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
                                            "REFLECTION_ABUSE"
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
                                            "ReflectionDetector"
                                    )
                                    .build()
                    );

                    log.warn(
                            "Reflection pattern detected: {} in {}",
                            pattern,
                            file.getName()
                    );
                }
            }

        } catch (Exception e) {

            log.error(
                    "ReflectionDetector error: {}",
                    file.getAbsolutePath(),
                    e
            );
        }

        return findings;
    }

    // =========================
    // normalize content
    // =========================

    private String normalize(
            String content
    ) {

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
    // severity calculator
    // =========================

    private String calculateSeverity(
            String pattern
    ) {

        String lower =
                pattern.toLowerCase();

        if (lower.contains("defineclass")
                || lower.contains("setaccessible")) {

            return "HIGH";
        }

        if (lower.contains("invoke")
                || lower.contains("forname")) {

            return "MEDIUM";
        }

        return "LOW";
    }

    // =========================
    // snippet extractor
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