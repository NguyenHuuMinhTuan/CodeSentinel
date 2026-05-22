package com.codesentinel.scan.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

@Slf4j
public class KeywordDetectUtil {

    private static final List<String> DANGEROUS_KEYWORDS = List.of(

            "Runtime.getRuntime()",
            "ProcessBuilder",
            "cmd.exe",
            "powershell",
            "eval(",
            "base64",
            "wget",
            "curl",
            "chmod 777",
            "rm -rf",
            "System.load",
            "exec("
    );

    public static Map<String, List<String>> scanKeyword(File directory) {

        Map<String, List<String>> suspiciousResults = new HashMap<>();

        scanRecursive(directory, suspiciousResults);

        return suspiciousResults;
    }

    private static void scanRecursive(
            File file,
            Map<String, List<String>> suspiciousResults
    ) {

        if (file == null || !file.exists()) {
            return;
        }

        // folder
        if (file.isDirectory()) {

            File[] children = file.listFiles();

            if (children != null) {

                for (File child : children) {
                    scanRecursive(child, suspiciousResults);
                }
            }

            return;
        }

        // chỉ scan text/source file
        if (!isReadableFile(file.getName())) {
            return;
        }

        scanFileContent(file, suspiciousResults);
    }

    private static boolean isReadableFile(String fileName) {

        String lower = fileName.toLowerCase();

        return lower.endsWith(".java")
                || lower.endsWith(".js")
                || lower.endsWith(".ts")
                || lower.endsWith(".py")
                || lower.endsWith(".php")
                || lower.endsWith(".txt")
                || lower.endsWith(".sh")
                || lower.endsWith(".bat");
    }

    private static void scanFileContent(
            File file,
            Map<String, List<String>> suspiciousResults
    ) {

        try (BufferedReader reader =
                     new BufferedReader(new FileReader(file))) {

            String line;

            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {

                lineNumber++;

                for (String keyword : DANGEROUS_KEYWORDS) {

                    if (line.contains(keyword)) {

                        String message =
                                "Line " + lineNumber +
                                        " -> " + keyword;

                        suspiciousResults
                                .computeIfAbsent(
                                        file.getAbsolutePath(),
                                        k -> new ArrayList<>()
                                )
                                .add(message);

                        log.warn(
                                "Suspicious keyword detected: {}",
                                message
                        );
                    }
                }
            }

        } catch (Exception e) {

            log.error(
                    "Cannot scan file: {}",
                    file.getAbsolutePath(),
                    e
            );
        }
    }
}