package com.codesentinel.scan.infrastructure;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Giải nén file upload (.zip / .rar) đúng MỘT lần, có chống Zip Slip và Zip Bomb.
 * <p>
 * Trước refactor, {@code ScanServiceImpl} gọi liên tiếp 2 class khác nhau
 * (ArchiveUtil rồi ZipUtil) để giải nén cùng 1 file — dư thừa và không nhất
 * quán với .rar. Class này gộp lại thành một điểm trách nhiệm duy nhất (SRP).
 */
@Slf4j
@Component
public class ArchiveExtractor {

    private static final int BUFFER_SIZE = 4096;

    // chống zip bomb
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB / file
    private static final int MAX_ENTRIES = 10_000;

    /**
     * Giải nén {@code archiveFile} vào một thư mục tạm mới và trả về thư mục đó.
     */
    public File extract(File archiveFile) throws IOException {

        String fileName = archiveFile.getName().toLowerCase();

        Path destDir = Paths.get(
                System.getProperty("java.io.tmpdir"),
                "extract_" + UUID.randomUUID()
        );

        Files.createDirectories(destDir);

        if (fileName.endsWith(".zip")) {

            extractZip(archiveFile, destDir);

        } else if (fileName.endsWith(".rar")) {

            extractRar(archiveFile, destDir);

        } else {

            throw new IOException("Unsupported archive format: " + fileName);
        }

        return destDir.toFile();
    }

    // =========================
    // ZIP (an toàn: chống zip-slip + zip-bomb)
    // =========================

    private void extractZip(File zipFile, Path destDir) throws IOException {

        Path destPath = destDir.toAbsolutePath().normalize();

        int entryCount = 0;

        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)))) {

            ZipEntry entry;

            byte[] buffer = new byte[BUFFER_SIZE];

            while ((entry = zis.getNextEntry()) != null) {

                entryCount++;

                if (entryCount > MAX_ENTRIES) {
                    throw new IOException("Too many ZIP entries");
                }

                Path targetPath = destPath.resolve(entry.getName()).normalize();

                if (!targetPath.startsWith(destPath)) {
                    throw new IOException("Zip Slip attack detected: " + entry.getName());
                }

                if (entry.isDirectory()) {

                    Files.createDirectories(targetPath);
                    zis.closeEntry();
                    continue;
                }

                Path parent = targetPath.getParent();

                if (parent != null) {
                    Files.createDirectories(parent);
                }

                long written = 0;

                try (BufferedOutputStream bos = new BufferedOutputStream(
                        new FileOutputStream(targetPath.toFile()))) {

                    int len;

                    while ((len = zis.read(buffer)) != -1) {

                        written += len;

                        if (written > MAX_FILE_SIZE) {
                            throw new IOException("File too large: " + entry.getName());
                        }

                        bos.write(buffer, 0, len);
                    }

                    bos.flush();
                }

                log.info("Extracted: {} ({} bytes)", targetPath, written);

                zis.closeEntry();
            }
        }
    }

    // =========================
    // RAR
    // =========================

    private void extractRar(File rarFile, Path destDir) throws IOException {

        try (Archive archive = new Archive(rarFile)) {

            FileHeader header;

            while ((header = archive.nextFileHeader()) != null) {

                Path targetPath = destDir.resolve(header.getFileName()).normalize();

                if (!targetPath.startsWith(destDir)) {
                    throw new IOException("Zip Slip attack detected: " + header.getFileName());
                }

                if (header.isDirectory()) {
                    Files.createDirectories(targetPath);
                    continue;
                }

                Path parent = targetPath.getParent();

                if (parent != null) {
                    Files.createDirectories(parent);
                }

                try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {

                    archive.extractFile(header, fos);
                }
            }

        } catch (Exception e) {

            throw new IOException("Failed to extract RAR archive", e);
        }
    }
}
