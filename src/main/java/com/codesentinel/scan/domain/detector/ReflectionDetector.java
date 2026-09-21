package com.codesentinel.scan.domain.detector;

import com.codesentinel.scan.domain.enums.FindingType;
import com.codesentinel.scan.domain.enums.SeverityLevel;
import com.codesentinel.scan.domain.model.Finding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class ReflectionDetector extends BaseDetector {

    private static final List<String> PATTERNS = List.of(
            "class.forname",
            "getmethod",
            "getdeclaredmethod",
            "getdeclaredfield",
            "setaccessible",
            "method.invoke",
            "invoke",
            "classloader",
            "defineclass",
            "java.lang.reflect"
    );

    @Override
    public List<Finding> detect(File file) {

        List<Finding> findings = new ArrayList<>();

        try {

            if (file == null || !file.exists() || file.isDirectory()) {
                return findings;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());

            if (bytes.length == 0) {
                return findings;
            }

            String content = new String(bytes, StandardCharsets.UTF_8);

            String normalized = normalize(content);

            String decoded = tryDecodeBase64(content);

            String merged = normalized + "\n" + normalize(decoded);

            boolean hasForName = merged.contains("class.forname");
            boolean hasGetMethod = merged.contains("getmethod");
            boolean hasInvoke = merged.contains("invoke");
            boolean hasClassLoader = merged.contains("classloader");
            boolean hasDefineClass = merged.contains("defineclass");

            int chainScore = calculateChainScore(
                    hasForName,
                    hasGetMethod,
                    hasInvoke,
                    hasClassLoader,
                    hasDefineClass
            );

            for (String pattern : PATTERNS) {

                String np = normalize(pattern);

                if (!merged.contains(np)) {
                    continue;
                }

                int severityScore = calculateSeverityScore(pattern, chainScore);

                findings.add(
                        Finding.builder()
                                .file(file.getAbsolutePath())
                                .line(findLineNumber(content, pattern))
                                .type(FindingType.REFLECTION_ABUSE)
                                .severity(mapSeverity(severityScore))
                                .matchedKeyword(pattern)
                                .codeSnippet(extractSnippet(content, pattern))
                                .detector("ReflectionDetector")
                                .build()
                );
            }

            if (chainScore >= 2) {

                findings.add(
                        Finding.builder()
                                .file(file.getAbsolutePath())
                                .type(FindingType.REFLECTION_CHAIN)
                                .severity(mapSeverity(chainScore * 30))
                                .matchedKeyword("reflection-chain")
                                .codeSnippet("Detected reflection execution chain pattern")
                                .detector("ReflectionDetector")
                                .build()
                );
            }

        } catch (Exception e) {
            log.error("[ReflectionDetector] ERROR", e);
        }

        return findings;
    }

    private String normalize(String content) {

        if (content == null) return "";

        return content
                .toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "");
    }

    private String tryDecodeBase64(String content) {

        try {

            String cleaned = content.replaceAll("[^A-Za-z0-9+/=]", "");

            if (cleaned.length() < 16 || cleaned.length() > 5000) {
                return "";
            }

            byte[] decoded = Base64.getDecoder().decode(cleaned);

            String result = new String(decoded, StandardCharsets.UTF_8);

            if (result.length() > 10000) return "";

            return result;

        } catch (Exception e) {
            return "";
        }
    }

    private int calculateChainScore(
            boolean hasForName,
            boolean hasGetMethod,
            boolean hasInvoke,
            boolean hasClassLoader,
            boolean hasDefineClass
    ) {

        int score = 0;

        if (hasForName) score += 1;
        if (hasGetMethod) score += 1;
        if (hasInvoke) score += 2;
        if (hasClassLoader) score += 2;
        if (hasDefineClass) score += 3;

        return score;
    }

    private int calculateSeverityScore(String pattern, int chainScore) {

        int base = 10;

        String p = pattern.toLowerCase();

        if (p.contains("defineclass") || p.contains("classloader")) {
            base += 40;
        }

        if (p.contains("invoke")) {
            base += 20;
        }

        return base + (chainScore * 10);
    }

    private SeverityLevel mapSeverity(int score) {

        if (score >= 80) return SeverityLevel.HIGH;
        if (score >= 50) return SeverityLevel.MEDIUM;
        return SeverityLevel.LOW;
    }

    private String extractSnippet(String content, String keyword) {

        try {

            int index = content.toLowerCase().indexOf(keyword.toLowerCase());

            if (index < 0) return "";

            int start = Math.max(0, index - 40);
            int end = Math.min(content.length(), index + 100);

            return content.substring(start, end);

        } catch (Exception e) {
            return "";
        }
    }

    private Integer findLineNumber(String content, String keyword) {

        try {

            String lowerContent = content.toLowerCase();

            String lowerKeyword = keyword.toLowerCase();

            int index = lowerContent.indexOf(lowerKeyword);

            if (index < 0) {
                return null;
            }

            int line = 1;

            for (int i = 0; i < index; i++) {

                if (content.charAt(i) == '\n') {
                    line++;
                }
            }

            return line;

        } catch (Exception e) {

            return null;
        }
    }
}
