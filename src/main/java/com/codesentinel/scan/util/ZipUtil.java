package com.codesentinel.scan.util;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    public static void extractZip(String zipFilePath, String destDir) throws IOException {

        File dir = new File(destDir);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        byte[] buffer = new byte[1024];

        ZipInputStream zis = new ZipInputStream(
                new FileInputStream(zipFilePath)
        );

        ZipEntry zipEntry = zis.getNextEntry();

        while (zipEntry != null) {

            File newFile = new File(destDir, zipEntry.getName());

            if (zipEntry.isDirectory()) {

                newFile.mkdirs();

            } else {

                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;

                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
            }

            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
    }
}