package com.codesentinel.scan.util;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArchiveUtil {

    // =========================
    // AUTO EXTRACT
    // =========================

    public static String extract(
            String archivePath
    ) throws Exception {

        String tempDir =
                System.getProperty("java.io.tmpdir")
                        + File.separator
                        + "extract_"
                        + UUID.randomUUID();

        File destDir =
                new File(tempDir);

        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        // =========================
        // ZIP
        // =========================

        if (archivePath.toLowerCase().endsWith(".zip")) {

            extractZip(
                    archivePath,
                    tempDir
            );

            return tempDir;
        }

        // =========================
        // RAR
        // =========================

        if (archivePath.toLowerCase().endsWith(".rar")) {

            extractRar(
                    archivePath,
                    tempDir
            );

            return tempDir;
        }

        throw new RuntimeException(
                "Unsupported archive format"
        );
    }

    // =========================
    // ZIP
    // =========================

    private static void extractZip(
            String zipFilePath,
            String destDir
    ) throws Exception {

        byte[] buffer =
                new byte[4096];

        try (
                ZipInputStream zis =
                        new ZipInputStream(
                                new FileInputStream(zipFilePath)
                        )
        ) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                File newFile =
                        new File(
                                destDir,
                                entry.getName()
                        );

                if (entry.isDirectory()) {

                    newFile.mkdirs();

                } else {

                    new File(
                            newFile.getParent()
                    ).mkdirs();

                    try (
                            FileOutputStream fos =
                                    new FileOutputStream(newFile)
                    ) {

                        int len;

                        while ((len = zis.read(buffer)) > 0) {

                            fos.write(
                                    buffer,
                                    0,
                                    len
                            );
                        }
                    }
                }

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