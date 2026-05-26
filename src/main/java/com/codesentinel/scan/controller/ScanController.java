package com.codesentinel.scan.controller;

import com.codesentinel.scan.model.ScanSummary;
import com.codesentinel.scan.service.ScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/scans")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    @PostMapping("/upload")
    public ScanSummary uploadZip(@RequestParam("file") MultipartFile file) {

        return scanService.scanZip(file);
    }
}