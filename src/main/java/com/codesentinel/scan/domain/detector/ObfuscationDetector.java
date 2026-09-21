package com.codesentinel.scan.domain.detector;

import com.codesentinel.scan.domain.enums.FindingType;
import com.codesentinel.scan.domain.enums.SeverityLevel;
import com.codesentinel.scan.domain.model.Finding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ObfuscationDetector extends BaseDetector {

    private static final Pattern INVISIBLE_PATTERN =
            Pattern.compile("[\\u200B-\\u200D\\uFEFF]");

    private static final Pattern HEX_PATTERN =
            Pattern.compile("(\\\\x[0-9a-fA-F]{2}){3,}");

    private static final Pattern LONG_RANDOM_PATTERN =
            Pattern.compile("[A-Za-z0-9+/]{100,}");

    private static final Pattern CONCAT_PATTERN =
            Pattern.compile("(\".*?\"\\s*\\+\\s*){3,}");

    @Override
    public List<Finding> detect(File file) {

        List<Finding> findings = new ArrayList<>();

        try {

            String content = Files.readString(file.toPath());

            findings.addAll(detectInvisibleUnicode(file, content));
            findings.addAll(detectHexEncoding(file, content));
            findings.addAll(detectLongRandomStrings(file, content));
            findings.addAll(detectExcessiveConcat(file, content));

        } catch (Exception e) {

            log.error("ObfuscationDetector error: {}", file.getAbsolutePath(), e);
        }

        return findings;
    }

    private List<Finding> detectInvisibleUnicode(File file, String content) {

        List<Finding> findings = new ArrayList<>();

        Matcher matcher = INVISIBLE_PATTERN.matcher(content);

        while (matcher.find()) {

            findings.add(
                    Finding.builder()
                            .file(file.getAbsolutePath())
                            .type(FindingType.INVISIBLE_UNICODE)
                            .severity(SeverityLevel.HIGH)
                            .matchedKeyword("Invisible Unicode")
                            .codeSnippet(extractSnippet(content, matcher.start()))
                            .detector("ObfuscationDetector")
                            .build()
            );
        }

        return findings;
    }

    private List<Finding> detectHexEncoding(File file, String content) {

        List<Finding> findings = new ArrayList<>();

        Matcher matcher = HEX_PATTERN.matcher(content);

        while (matcher.find()) {

            findings.add(
                    Finding.builder()
                            .file(file.getAbsolutePath())
                            .type(FindingType.HEX_OBFUSCATION)
                            .severity(SeverityLevel.HIGH)
                            .matchedKeyword(matcher.group())
                            .codeSnippet(extractSnippet(content, matcher.start()))
                            .detector("ObfuscationDetector")
                            .build()
            );
        }

        return findings;
    }

    private List<Finding> detectLongRandomStrings(File file, String content) {

        List<Finding> findings = new ArrayList<>();

        Matcher matcher = LONG_RANDOM_PATTERN.matcher(content);

        while (matcher.find()) {

            findings.add(
                    Finding.builder()
                            .file(file.getAbsolutePath())
                            .type(FindingType.SUSPICIOUS_RANDOM_STRING)
                            .severity(SeverityLevel.MEDIUM)
                            .matchedKeyword(matcher.group())
                            .codeSnippet(extractSnippet(content, matcher.start()))
                            .detector("ObfuscationDetector")
                            .build()
            );
        }

        return findings;
    }

    private List<Finding> detectExcessiveConcat(File file, String content) {

        List<Finding> findings = new ArrayList<>();

        Matcher matcher = CONCAT_PATTERN.matcher(content);

        while (matcher.find()) {

            findings.add(
                    Finding.builder()
                            .file(file.getAbsolutePath())
                            .type(FindingType.STRING_OBFUSCATION)
                            .severity(SeverityLevel.MEDIUM)
                            .matchedKeyword("String Concatenation")
                            .codeSnippet(extractSnippet(content, matcher.start()))
                            .detector("ObfuscationDetector")
                            .build()
            );
        }

        return findings;
    }
}
