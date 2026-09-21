package com.codesentinel.scan.domain.detector;

import com.codesentinel.scan.domain.enums.FindingType;
import com.codesentinel.scan.domain.enums.SeverityLevel;
import com.codesentinel.scan.domain.model.Finding;
import com.codesentinel.scan.infrastructure.ContentNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KeywordDetector extends BaseDetector {

    private static final List<String> DANGEROUS_KEYWORDS = List.of(

            "runtime.getruntime()",
            "processbuilder",
            "cmd.exe",
            "powershell",
            "eval(",
            "wget",
            "curl",
            "system.load",
            "exec("
    );

    @Override
    public List<Finding> detect(File file) {

        List<Finding> findings = new ArrayList<>();

        try {

            List<String> lines = Files.readAllLines(file.toPath());

            for (int i = 0; i < lines.size(); i++) {

                String rawLine = lines.get(i);

                String line = ContentNormalizer.normalizeAggressive(rawLine);

                int lineNumber = i + 1;

                for (String keyword : DANGEROUS_KEYWORDS) {

                    if (line.contains(keyword)) {

                        findings.add(
                                Finding.builder()
                                        .file(file.getAbsolutePath())
                                        .line(lineNumber)
                                        .type(FindingType.DANGEROUS_KEYWORD)
                                        .severity(SeverityLevel.HIGH)
                                        .matchedKeyword(keyword)
                                        .codeSnippet(rawLine)
                                        .detector("KeywordDetector")
                                        .build()
                        );
                    }
                }
            }

        } catch (Exception e) {

            log.error("KeywordDetector error", e);
        }

        return findings;
    }

}
