package com.codesentinel.scan.detector;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public abstract class BaseDetector
        implements ScanDetector {

    protected String readContent(File file) {

        try {

            byte[] bytes =
                    Files.readAllBytes(file.toPath());

            return new String(
                    bytes,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            return "";
        }
    }

    protected String extractSnippet(
            String content,
            int index
    ) {

        try {

            int start =
                    Math.max(0, index - 40);

            int end =
                    Math.min(
                            content.length(),
                            index + 100
                    );

            return content.substring(
                    start,
                    end
            );

        } catch (Exception e) {

            return "";
        }
    }

    protected Integer findLineNumber(
            String content,
            int index
    ) {

        try {

            return content.substring(0, index)
                    .split("\n")
                    .length;

        } catch (Exception e) {

            return null;
        }
    }
}