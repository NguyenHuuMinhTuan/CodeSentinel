package com.codesentinel.scan.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileWalker {

    public static List<File> walk(
            File root
    ) {

        List<File> files =
                new ArrayList<>();

        scan(root, files);

        return files;
    }

    private static void scan(
            File file,
            List<File> files
    ) {

        if (file == null || !file.exists()) {
            return;
        }

        if (file.isFile()) {

            files.add(file);
            return;
        }

        File[] children =
                file.listFiles();

        if (children == null) {
            return;
        }

        for (File child : children) {

            scan(child, files);
        }
    }
}