package com.codesentinel.scan.detector;

import com.codesentinel.scan.model.Finding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ObfuscationDetector implements ScanDetector {

    private static final List<String>
            OBFUSCATION_PATTERNS = List.of(

            "eval(",
            "atob(",
            "fromcharcode",
            "base64",
            "decode",
            "unescape"
    );

    // =========================
    // invisible unicode chars
    // =========================

    private static final Pattern INVISIBLE_PATTERN =
            Pattern.compile(
                    "[\\u200B-\\u200D\\uFEFF]"
            );

    // =========================
    // hex encoded pattern
    // =========================

    private static final Pattern HEX_PATTERN =
            Pattern.compile(
                    "(\\\\x[0-9a-fA-F]{2}){3,}"
            );

    // =========================
    // suspicious long string
    // =========================

    private static final Pattern LONG_RANDOM_PATTERN =
            Pattern.compile(
                    "[A-Za-z0-9+/]{100,}"
            );

    // =========================
    // excessive string concat
    // =========================

    private static final Pattern CONCAT_PATTERN =
            Pattern.compile(
                    "(\".*?\"\\s*\\+\\s*){3,}"
            );

    @Override
    public List<Finding> detect(File file) {

        List<Finding> findings =
                new ArrayList<>();

        try {

            String content =
                    Files.readString(file.toPath());

            // =========================
            // invisible unicode
            // =========================

            findings.addAll(
                    detectInvisibleUnicode(
                            file,
                            content
                    )
            );

            // =========================
            // hex encoding
            // =========================

            findings.addAll(
                    detectHexEncoding(
                            file,
                            content
                    )
            );

            // =========================
            // suspicious random string
            // =========================

            findings.addAll(
                    detectLongRandomStrings(
                            file,
                            content
                    )
            );

            // =========================
            // excessive concat
            // =========================

            findings.addAll(
                    detectExcessiveConcat(
                            file,
                            content
                    )
            );

        } catch (Exception e) {

            log.error(
                    "ObfuscationDetector error: {}",
                    file.getAbsolutePath(),
                    e
            );
        }

        return findings;
    }

    // =========================
    // invisible unicode detect
    // =========================

    private List<Finding> detectInvisibleUnicode(
            File file,
            String content
    ) {

        List<Finding> findings =
                new ArrayList<>();

        Matcher matcher =
                INVISIBLE_PATTERN.matcher(content);

        while (matcher.find()) {

            findings.add(
                    Finding.builder()
                            .file(
                                    file.getAbsolutePath()
                            )
                            .type(
                                    "INVISIBLE_UNICODE"
                            )
                            .severity(
                                    "HIGH"
                            )
                            .matchedKeyword(
                                    "Invisible Unicode"
                            )
                            .codeSnippet(
                                    extractSnippet(
                                            content,
                                            matcher.start()
                                    )
                            )
                            .detector(
                                    "ObfuscationDetector"
                            )
                            .build()
            );
        }

        return findings;
    }

    // =========================
    // hex obfuscation detect
    // =========================

    private List<Finding> detectHexEncoding(
            File file,
            String content
    ) {

        List<Finding> findings =
                new ArrayList<>();

        Matcher matcher =
                HEX_PATTERN.matcher(content);

        while (matcher.find()) {

            findings.add(
                    Finding.builder()
                            .file(
                                    file.getAbsolutePath()
                            )
                            .type(
                                    "HEX_OBFUSCATION"
                            )
                            .severity(
                                    "HIGH"
                            )
                            .matchedKeyword(
                                    matcher.group()
                            )
                            .codeSnippet(
                                    extractSnippet(
                                            content,
                                            matcher.start()
                                    )
                            )
                            .detector(
                                    "ObfuscationDetector"
                            )
                            .build()
            );
        }

        return findings;
    }

    // =========================
    // suspicious long string
    // =========================

    private List<Finding> detectLongRandomStrings(
            File file,
            String content
    ) {

        List<Finding> findings =
                new ArrayList<>();

        Matcher matcher =
                LONG_RANDOM_PATTERN.matcher(content);

        while (matcher.find()) {

            findings.add(
                    Finding.builder()
                            .file(
                                    file.getAbsolutePath()
                            )
                            .type(
                                    "SUSPICIOUS_RANDOM_STRING"
                            )
                            .severity(
                                    "MEDIUM"
                            )
                            .matchedKeyword(
                                    matcher.group()
                            )
                            .codeSnippet(
                                    extractSnippet(
                                            content,
                                            matcher.start()
                                    )
                            )
                            .detector(
                                    "ObfuscationDetector"
                            )
                            .build()
            );
        }

        return findings;
    }

    // =========================
    // excessive concat detect
    // =========================

    private List<Finding> detectExcessiveConcat(
            File file,
            String content
    ) {

        List<Finding> findings =
                new ArrayList<>();

        Matcher matcher =
                CONCAT_PATTERN.matcher(content);

        while (matcher.find()) {

            findings.add(
                    Finding.builder()
                            .file(
                                    file.getAbsolutePath()
                            )
                            .type(
                                    "STRING_OBFUSCATION"
                            )
                            .severity(
                                    "MEDIUM"
                            )
                            .matchedKeyword(
                                    "String Concatenation"
                            )
                            .codeSnippet(
                                    extractSnippet(
                                            content,
                                            matcher.start()
                                    )
                            )
                            .detector(
                                    "ObfuscationDetector"
                            )
                            .build()
            );
        }

        return findings;
    }

    // =========================
    // snippet helper
    // =========================

    private String extractSnippet(
            String content,
            int index
    ) {

        try {

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