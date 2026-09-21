package com.codesentinel.scan.domain.model;

import com.codesentinel.scan.domain.enums.FindingType;
import com.codesentinel.scan.domain.enums.SeverityLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Finding {

    private String file;

    private Integer line;

    private FindingType type;

    private SeverityLevel severity;

    private String matchedKeyword;

    private String codeSnippet;

    private String detector;
}
