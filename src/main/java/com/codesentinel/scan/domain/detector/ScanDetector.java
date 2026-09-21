package com.codesentinel.scan.domain.detector;

import com.codesentinel.scan.domain.model.Finding;

import java.io.File;
import java.util.List;

public interface ScanDetector {

    List<Finding> detect(File file);
}
