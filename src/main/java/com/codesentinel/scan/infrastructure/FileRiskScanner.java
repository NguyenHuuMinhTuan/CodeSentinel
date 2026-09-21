package com.codesentinel.scan.infrastructure;

import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class FileRiskScanner {

    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            ".exe",
            ".bat",
            ".sh",
            ".ps1",
            ".dll"
    );

    public List<String> scanDangerousFiles(File directory) {

        List<String> suspiciousFiles = new ArrayList<>();

        scanRecursive(directory, suspiciousFiles);

        return suspiciousFiles;
    }

    private void scanRecursive(File file, List<String> suspiciousFiles) {

        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {

            File[] children = file.listFiles();

            if (children != null) {

                for (File child : children) {
                    scanRecursive(child, suspiciousFiles);
                }
            }

            return;
        }

        String fileName = file.getName().toLowerCase();

        for (String extension : DANGEROUS_EXTENSIONS) {

            if (fileName.endsWith(extension)) {

                suspiciousFiles.add(file.getAbsolutePath());
                break;
            }
        }
    }
}
