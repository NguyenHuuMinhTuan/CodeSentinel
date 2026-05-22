package com.codesentinel.scan.detector;

import com.codesentinel.scan.model.Finding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class Base64Detector implements ScanDetector {

    // regex tìm chuỗi base64 dài
    private static final Pattern BASE64_PATTERN =
            Pattern.compile(
                    "[A-Za-z0-9+/]{20,}={0,2}"
            );

    @Override
    public List<Finding> detect(File file) {

        List<Finding> findings =
                new ArrayList<>();

        try {

            String content =
                    Files.readString(file.toPath());

            List<String> base64Strings =
                    extractBase64Strings(content);

            for (String encoded : base64Strings) {

                if (!isValidBase64(encoded)) {
                    continue;
                }

                String decoded =
                        decodeBase64(encoded);

                if (containsDangerousPayload(decoded)) {

                    findings.add(
                            Finding.builder()
                                    .file(
                                            file.getAbsolutePath()
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
                                    .codeSnippet(decoded)
                                    .detector(
                                            "Base64Detector"
                                    )
                                    .build()
                    );
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

    // =========================
    // extract chuỗi base64
    // =========================

    private List<String> extractBase64Strings(
            String content
    ) {

        List<String> result =
                new ArrayList<>();

        Matcher matcher =
                BASE64_PATTERN.matcher(content);

        while (matcher.find()) {

            result.add(matcher.group());
        }

        return result;
    }

    // =========================
    // validate base64
    // =========================

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

    // =========================
    // decode
    // =========================

    private String decodeBase64(
            String encoded
    ) {

        try {

            byte[] decodedBytes =
                    Base64.getDecoder()
                            .decode(encoded);

            return new String(decodedBytes);

        } catch (Exception e) {

            return "";
        }
    }

    // =========================
    // detect payload nguy hiểm
    // =========================

    private boolean containsDangerousPayload(
            String decoded
    ) {

        String lower =
                decoded.toLowerCase();

        return lower.contains("powershell")
                || lower.contains("cmd.exe")
                || lower.contains("wget")
                || lower.contains("curl")
                || lower.contains("runtime");
    }
}