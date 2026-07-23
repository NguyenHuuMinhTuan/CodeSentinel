package com.codesentinel.scan.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

@Slf4j
public class ZipUtil {

    private static final int BUFFER_SIZE = 4096;

    // limit chống zip bomb
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB
    private static final int MAX_ENTRIES = 10000;

    public static void extractZip(
            String zipFilePath,
            String destDir
    ) throws IOException {

        Path destPath =
                Paths.get(destDir).toAbsolutePath().normalize();

        Files.createDirectories(destPath);

        int entryCount = 0;

        try (
                ZipInputStream zis =
                        new ZipInputStream(
                                new BufferedInputStream(
                                        new FileInputStream(zipFilePath)
                                )
                        )
        ) {

            ZipEntry entry;

            byte[] buffer = new byte[BUFFER_SIZE];

            while ((entry = zis.getNextEntry()) != null) {

                entryCount++;

                // =========================
                // anti zip bomb
                // =========================

                if (entryCount > MAX_ENTRIES) {

                    throw new IOException(
                            "Too many ZIP entries"
                    );
                }

                // =========================
                // normalize path
                // =========================

                Path targetPath =
                        destPath.resolve(entry.getName())
                                .normalize();

                // =========================
                // prevent zip slip
                // =========================

                if (!targetPath.startsWith(destPath)) {

                    throw new IOException(
                            "Zip Slip attack detected: "
                                    + entry.getName()
                    );
                }

                File targetFile =
                        targetPath.toFile();

                // =========================
                // directory
                // =========================

                if (entry.isDirectory()) {

                    Files.createDirectories(targetPath);

                    zis.closeEntry();
                    continue;
                }

                // =========================
                // create parent dirs
                // =========================

                Path parent =
                        targetPath.getParent();

                if (parent != null) {

                    Files.createDirectories(parent);
                }

                // =========================
                // write file
                // =========================

                long written = 0;

                try (
                        BufferedOutputStream bos =
                                new BufferedOutputStream(
                                        new FileOutputStream(targetFile)
                                )
                ) {

                    int len;

                    while ((len = zis.read(buffer)) != -1) {

                        written += len;

                        // =========================
                        // anti huge file
                        // =========================

                        if (written > MAX_FILE_SIZE) {

                            throw new IOException(
                                    "File too large: "
                                            + entry.getName()
                            );
                        }

                        bos.write(buffer, 0, len);
                    }

                    bos.flush();
                }

                log.info(
                        "Extracted: {} ({} bytes)",
                        targetFile.getAbsolutePath(),
                        written
                );

                zis.closeEntry();
            }
        }
    }

    // =========================
    // RAR
    // =========================

    private static void extractRar(
            String rarPath,
            String destDir
    ) throws Exception {

        File rarFile =
                new File(rarPath);

        try (
                Archive archive =
                        new Archive(rarFile)
        ) {

            FileHeader header;

            while ((header = archive.nextFileHeader()) != null) {

                File outFile =
                        new File(
                                destDir,
                                header.getFileName()
                        );

                if (header.isDirectory()) {

                    outFile.mkdirs();

                    continue;
                }

                new File(
                        outFile.getParent()
                ).mkdirs();

                try (
                        FileOutputStream fos =
                                new FileOutputStream(outFile)
                ) {

                    archive.extractFile(
                            header,
                            fos
                    );
                }
            }
        }
    }
}