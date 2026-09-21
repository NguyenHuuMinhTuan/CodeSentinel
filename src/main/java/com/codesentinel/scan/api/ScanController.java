package com.codesentinel.scan.api;

import com.codesentinel.scan.application.ScanService;
import com.codesentinel.scan.domain.model.ScanSummary;
import com.codesentinel.shared.exception.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/scans")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    @PostMapping("/upload")
    public ApiResponse<ScanSummary> uploadZip(@RequestParam("file") MultipartFile file) {

        ScanSummary summary = scanService.scanZip(file);

        return ApiResponse.success(summary, "Scan completed successfully");
    }
}
