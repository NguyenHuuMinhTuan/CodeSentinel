package com.codesentinel.scan.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegexRule {

    private String name;

    private String regex;

    private String severity;

    private String description;
}
