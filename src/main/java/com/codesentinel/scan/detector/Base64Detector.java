package com.codesentinel.scan.detector;

import com.codesentinel.scan.model.Finding;
import com.codesentinel.scan.util.ContentNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class Base64Detector implements ScanDetector {

    /*
     * =========================
     * BASE64 REGEX
     * =========================
     *
     * match:
     * cG93ZXJzaGVsbA==
     * Y21kLmV4ZQ==
     *
     * hỗ trợ:
     * + /
     * - _
     */

    private static final Pattern BASE64_PATTERN =
            Pattern.compile(
                    "[\"']([A-Za-z0-9+/=_-]{8,})[\"']"
            );

    /*
     * =========================
     * DANGEROUS PAYLOADS
     * =========================
     */

    private static final List<String>
            DANGEROUS_KEYWORDS =
            List.of(

                    "powershell",
                    "cmd.exe",
                    "wget",
                    "curl",
                    "runtime",
                    "processbuilder",
                    "exec",
                    "eval",
                    "bash",
                    "socket",
                    "webhook",
                    "discord",
                    "token",
                    "stealer"
            );

    @Override
    public List<Finding> detect(
            File file
    ) {

        List<Finding> findings =
                new ArrayList<>();

        try {

            /*
             * =========================
             * READ FILE
             * =========================
             */

            List<String> lines =
                    Files.readAllLines(
                            file.toPath()
                    );

            /*
             * =========================
             * SCAN EACH LINE
             * =========================
             */

            for (int i = 0;
                 i < lines.size();
                 i++) {

                String rawLine =
                        lines.get(i);

                int lineNumber =
                        i + 1;

                /*
                 * extract base64 strings
                 */

                List<String> candidates =
                        extractBase64Strings(
                                rawLine
                        );


                /*
                 * scan từng candidate
                 */

                for (String encoded : candidates) {

                    /*
                     * validate
                     */

                    if (!isValidBase64(encoded)) {

                        log.debug(
                                "Invalid base64: {}",
                                encoded
                        );

                        continue;
                    }

                    /*
                     * decode
                     */

                    String decoded =
                            decodeBase64(encoded);


                    /*
                     * detect dangerous payload
                     */

                    if (containsDangerousPayload(
                            decoded
                    )) {

                        findings.add(
                                Finding.builder()
                                        .file(
                                                file.getAbsolutePath()
                                        )
                                        .line(
                                                lineNumber
                                        )
                                        .type(
                                                "BASE64_OBFUSCATION"
                                        )
                                        .severity(
                                                "HIGH"
                                        )
                                        .matchedKeyword(
                                                encoded
                                        )
                                        .codeSnippet(
                                                rawLine
                                        )
                                        .detector(
                                                "Base64Detector"
                                        )
                                        .build()
                        );
                    }
                }
            }

        } catch (Exception e) {

            log.error(
                    "Base64Detector error",
                    e
            );
        }

        return findings;
    }

    /*
     * =========================
     * EXTRACT BASE64 STRINGS
     * =========================
     */

    private List<String>
    extractBase64Strings(
            String content
    ) {

        List<String> result =
                new ArrayList<>();

        Matcher matcher =
                BASE64_PATTERN.matcher(
                        content
                );

        while (matcher.find()) {

            result.add(
                    matcher.group(1)
            );
        }

        return result;
    }

    /*
     * =========================
     * VALIDATE BASE64
     * =========================
     */

    private boolean isValidBase64(
            String value
    ) {

        try {

            Base64.getDecoder()
                    .decode(value);

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    /*
     * =========================
     * DECODE BASE64
     * =========================
     */

    private String decodeBase64(
            String encoded
    ) {

        try {

            byte[] decodedBytes =
                    Base64.getDecoder()
                            .decode(encoded);

            return new String(
                    decodedBytes,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            return "";
        }
    }

    /*
     * =========================
     * DETECT DANGEROUS PAYLOAD
     * =========================
     */

    private boolean
    containsDangerousPayload(
            String decoded
    ) {

        String lower =
                decoded.toLowerCase();

        return DANGEROUS_KEYWORDS
                .stream()
                .anyMatch(
                        lower::contains
                );
    }
}