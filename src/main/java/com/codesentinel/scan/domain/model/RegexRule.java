package com.codesentinel.scan.domain.model;

import com.codesentinel.scan.domain.enums.FindingType;
import com.codesentinel.scan.domain.enums.SeverityLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegexRule {

    private FindingType type;

    private String regex;

    private SeverityLevel severity;

    private String description;
}
