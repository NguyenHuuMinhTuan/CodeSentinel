package com.codesentinel.scan.application;

public final class FileExtensions {

    private FileExtensions() {
    }

    public static String extractExtension(String fileName) {

        int lastDot = fileName.lastIndexOf(".");

        if (lastDot < 0) {
            return "";
        }

        return fileName.substring(lastDot + 1).toLowerCase();
    }
}
