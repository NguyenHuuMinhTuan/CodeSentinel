package com.codesentinel.scan.domain.detector;

import com.codesentinel.scan.domain.enums.FindingType;
import com.codesentinel.scan.domain.enums.SeverityLevel;
import com.codesentinel.scan.domain.model.Finding;
import com.codesentinel.scan.domain.model.RegexRule;
import com.codesentinel.scan.infrastructure.ContentNormalizer;
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
public class RegexDetector extends BaseDetector {

    private static final List<RegexRule> RULES = List.of(

            RegexRule.builder()
                    .type(FindingType.SUSPICIOUS_URL)
                    .regex("https?://[^\\s\"']+")
                    .severity(SeverityLevel.MEDIUM)
                    .description("Suspicious URL detected")
                    .build(),

            RegexRule.builder()
                    .type(FindingType.DISCORD_WEBHOOK)
                    .regex("https://discord\\.com/api/webhooks/.*")
                    .severity(SeverityLevel.HIGH)
                    .description("Discord webhook detected")
                    .build(),

            RegexRule.builder()
                    .type(FindingType.IP_ADDRESS)
                    .regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")
                    .severity(SeverityLevel.LOW)
                    .description("IP address detected")
                    .build()
    );

    @Override
    public List<Finding> detect(File file) {

        List<Finding> findings = new ArrayList<>();

        try {

            List<String> lines = Files.readAllLines(file.toPath());

            for (int i = 0; i < lines.size(); i++) {

                String rawLine = lines.get(i);

                String line = ContentNormalizer.normalizeLight(rawLine);

                int lineNumber = i + 1;

                for (RegexRule rule : RULES) {

                    Pattern pattern = Pattern.compile(rule.getRegex());

                    Matcher matcher = pattern.matcher(line);

                    while (matcher.find()) {

                        findings.add(
                                Finding.builder()
                                        .file(file.getAbsolutePath())
                                        .line(lineNumber)
                                        .type(rule.getType())
                                        .severity(rule.getSeverity())
                                        .matchedKeyword(matcher.group())
                                        .codeSnippet(rawLine)
                                        .detector("RegexDetector")
                                        .build()
                        );
                    }
                }
            }

        } catch (Exception e) {

            log.error("RegexDetector error", e);
        }

        return findings;
    }
}
