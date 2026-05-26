package com.codesentinel.scan.util;

public class ContentNormalizer {

    private ContentNormalizer() {
    }

    // =========================
    // aggressive normalize
    // anti bypass
    // =========================

    public static String
    normalizeAggressive(
            String content
    ) {

        if (content == null) {
            return "";
        }

        return content

                // lowercase
                .toLowerCase()

                // remove spaces/newlines/tabs
                .replaceAll("\\s+", "")

                // remove quotes
                .replace("\"", "")
                .replace("'", "")

                // remove concat
                .replace("+", "")

                // remove backticks
                .replace("`", "");
    }

    // =========================
    // light normalize
    // preserve structure
    // =========================

    public static String
    normalizeLight(
            String content
    ) {

        if (content == null) {
            return "";
        }

        return content
                .toLowerCase();
    }
}