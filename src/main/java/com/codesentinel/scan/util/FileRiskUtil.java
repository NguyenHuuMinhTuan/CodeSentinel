package com.codesentinel.scan.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FileRiskUtil {

    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            ".exe",
            ".bat",
            ".sh",
            ".ps1",
            ".dll"
    );

    public static List<String> scanDangerousFiles(File directory) {

        List<String> suspiciousFiles = new ArrayList<>();

        scanRecursive(directory, suspiciousFiles);

        return suspiciousFiles;
    }

    private static void scanRecursive(
            File file,
            List<String> suspiciousFiles
    ) {

        if (file == null || !file.exists()) {
            return;
        }

        // nếu là folder
        if (file.isDirectory()) {

            File[] children = file.listFiles();

            if (children != null) {

                for (File child : children) {
                    scanRecursive(child, suspiciousFiles);
                }
            }

            return;
        }

        // check extension
        String fileName = file.getName().toLowerCase();

        for (String extension : DANGEROUS_EXTENSIONS) {

            if (fileName.endsWith(extension)) {

                suspiciousFiles.add(file.getAbsolutePath());

                break;
            }
        }
    }
}