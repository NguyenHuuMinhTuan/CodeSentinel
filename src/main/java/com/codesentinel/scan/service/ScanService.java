package com.codesentinel.scan.service;

import com.codesentinel.scan.model.ScanSummary;
import org.springframework.web.multipart.MultipartFile;

public interface ScanService {

    ScanSummary scanZip(MultipartFile file);
}
