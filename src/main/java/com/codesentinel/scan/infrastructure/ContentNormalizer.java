package com.codesentinel.scan.infrastructure;

public final class ContentNormalizer {

    private ContentNormalizer() {
    }

    /**
     * Chuẩn hoá mạnh: hạ thường, bỏ khoảng trắng/dấu nháy/dấu cộng/backtick
     * để chống các kỹ thuật bypass đơn giản (chèn khoảng trắng, nối chuỗi...).
     */
    public static String normalizeAggressive(String content) {

        if (content == null) {
            return "";
        }

        return content
                .toLowerCase()
                .replaceAll("\\s+", "")
                .replace("\"", "")
                .replace("'", "")
                .replace("+", "")
                .replace("`", "");
    }

    /**
     * Chuẩn hoá nhẹ: chỉ hạ thường, giữ nguyên cấu trúc dòng.
     */
    public static String normalizeLight(String content) {

        if (content == null) {
            return "";
        }

        return content.toLowerCase();
    }
}
