package com.codesentinel.scan.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Dọn dẹp file/thư mục tạm sinh ra trong quá trình quét (upload gốc + thư mục giải nén).
 */
@Slf4j
@Component
public class TempWorkspaceCleaner {

    public void cleanup(File file) {

        try {

            if (file == null || !file.exists()) {
                return;
            }

            if (file.isDirectory()) {

                try (var paths = Files.walk(file.toPath())) {

                    paths.map(Path::toFile)
                            .sorted(Comparator.reverseOrder())
                            .forEach(File::delete);
                }

            } else {

                file.delete();
            }

        } catch (Exception e) {

            log.error("Cleanup failed: {}", file.getAbsolutePath(), e);
        }
    }
}
