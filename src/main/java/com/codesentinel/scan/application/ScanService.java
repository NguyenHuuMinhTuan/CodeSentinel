package com.codesentinel.scan.application;

import com.codesentinel.scan.domain.model.ScanSummary;
import org.springframework.web.multipart.MultipartFile;

public interface ScanService {

    ScanSummary scanZip(MultipartFile file);
}
