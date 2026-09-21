package com.codesentinel.scan.application;

import com.codesentinel.scan.domain.model.FileInfo;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Duyệt cây thư mục đã giải nén để liệt kê file + thống kê theo phần mở rộng.
 * Tách riêng khỏi ScanServiceImpl theo Single Responsibility Principle.
 */
@Component
public class ScanReportBuilder {

    public record Report(List<FileInfo> files, Map<String, Integer> extensions) {
    }

    public Report build(File scanTarget) throws Exception {

        List<FileInfo> files = new ArrayList<>();

        Map<String, Integer> extensions = new HashMap<>();

        if (scanTarget.isFile()) {

            addFile(scanTarget.getName(), scanTarget.getAbsolutePath(), files, extensions);

            return new Report(files, extensions);
        }

        try (Stream<Path> paths = Files.walk(scanTarget.toPath())) {

            paths.filter(Files::isRegularFile)
                    .forEach(path -> addFile(
                            path.getFileName().toString(),
                            path.toString(),
                            files,
                            extensions
                    ));
        }

        return new Report(files, extensions);
    }

    private void addFile(
            String fileName,
            String path,
            List<FileInfo> files,
            Map<String, Integer> extensions
    ) {

        String extension = FileExtensions.extractExtension(fileName);

        extensions.merge(extension, 1, Integer::sum);

        files.add(
                FileInfo.builder()
                        .fileName(fileName)
                        .extension(extension)
                        .path(path)
                        .build()
        );
    }
}
