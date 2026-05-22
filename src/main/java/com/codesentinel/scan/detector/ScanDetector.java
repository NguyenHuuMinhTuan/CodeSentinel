package com.codesentinel.scan.detector;

import com.codesentinel.scan.model.Finding;

import java.io.File;
import java.util.List;

public interface ScanDetector {

    List<Finding> detect(File file);
}