package com.codesentinel.scan.rule;

import com.codesentinel.scan.enums.SeverityLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DetectionRule {

    private String name;

    private String regex;

    private String type;

    private SeverityLevel severity;

    private String description;
}