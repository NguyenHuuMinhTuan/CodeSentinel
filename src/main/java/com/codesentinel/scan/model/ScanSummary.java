package com.codesentinel.scan.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ScanSummary {

    private Integer totalFiles;

    private Map<String, Integer> extensions;

    private List<FileInfo> files;

    private List<String> suspiciousFiles;

    private List<Finding> findings;
}