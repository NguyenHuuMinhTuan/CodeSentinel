package com.codesentinel.scan.rule;

import java.util.List;

import static com.codesentinel.scan.enums.SeverityLevel.HIGH;

public class RuleRegistry {

    public static List<DetectionRule> getRules() {

        return List.of(

                DetectionRule.builder()
                        .name("Runtime Exec")
                        .regex("runtime\\s*\\.\\s*getruntime")
                        .type("COMMAND_EXECUTION")
                        .severity(HIGH)
                        .description("Dangerous runtime execution")
                        .build()

        );
    }
}