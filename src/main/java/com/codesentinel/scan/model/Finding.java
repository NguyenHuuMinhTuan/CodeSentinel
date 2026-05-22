package com.codesentinel.scan.model;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Finding {

    private String file;

    private Integer line;

    private String type;

    private String severity;

    private String matchedKeyword;

    private String codeSnippet;

    private String detector;
}