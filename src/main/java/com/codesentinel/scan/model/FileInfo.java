package com.codesentinel.scan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class FileInfo {

    private String fileName;

    private String extension;

    private String path;
}