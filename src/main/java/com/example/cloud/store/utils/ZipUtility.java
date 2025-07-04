package com.example.cloud.store.utils;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public class ZipUtility {

    public static void zip(Path source, Path destinationZip) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(destinationZip))) {
            if (Files.isDirectory(source)) {
                Files.walk(source).filter(p -> !Files.isDirectory(p)).forEach(p -> {
                    try (InputStream fis = Files.newInputStream(p)) {
                        ZipEntry entry = new ZipEntry(source.relativize(p).toString().replace("\\", "/"));
                        zos.putNextEntry(entry);
                        fis.transferTo(zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } else {
                try (InputStream fis = Files.newInputStream(source)) {
                    ZipEntry entry = new ZipEntry(source.getFileName().toString());
                    zos.putNextEntry(entry);
                    fis.transferTo(zos);
                    zos.closeEntry();
                }
            }
        }
    }

    public static void unzip(Path zipFile, Path outputDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = outputDir.resolve(entry.getName()).normalize();
                if (!newPath.startsWith(outputDir)) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    try (OutputStream fos = Files.newOutputStream(newPath)) {
                        zis.transferTo(fos);
                    }
                }

                zis.closeEntry();
            }
        }
    }
}
