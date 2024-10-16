package com.hypherionmc.mmode.util;

import com.hypherionmc.mmode.ModConstants;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BackupUtil {

    static final DateTimeFormatter FORMATTER = (new DateTimeFormatterBuilder())
            .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('_')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .toFormatter();

    public static void createBackup() throws IOException {
        File worldDir = new File("world");
        File backupDir = new File("mbackups");

        if (!worldDir.exists())
            return;

        if (!backupDir.exists())
            backupDir.mkdirs();

        String fileName = String.format("%s.zip", LocalDateTime.now().format(FORMATTER));

        Thread backupThread = new Thread() {
            {
                this.setName("Maintenance Mode Backup Thread");
            }

            @Override
            public void run() {
                ModConstants.LOG.info("Starting Maintenance Mode Backup");
                createZipFile(new File(backupDir + File.separator + fileName), new File(System.getProperty("user.dir")));
            }
        };

        backupThread.start();
    }

    public static void createZipFile(File zipFileName, File fileOrDirectoryToZip) {
        try (ZipOutputStream stream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipFileName.toPath())))) {
            addFileToZipStream(stream, fileOrDirectoryToZip, null);
        } catch (IOException e) {
            ModConstants.LOG.error("Failed to create backup at {}", zipFileName.getAbsolutePath(), e);
        }

        ModConstants.LOG.info("Created backup at {}", zipFileName);
    }

    private static void addFileToZipStream(ZipOutputStream zipArchiveOutputStream, File fileToZip, @Nullable String base) throws IOException {
        String entryName = base == null ? fileToZip.getName() : String.format("%s/%s", base, fileToZip.getName());

        if(fileToZip.isFile() && !fileToZip.getName().contains(".lock")) {
            ZipEntry zipArchiveEntry = new ZipEntry(entryName);
            zipArchiveOutputStream.putNextEntry(zipArchiveEntry);

            try(FileInputStream stream = new FileInputStream(fileToZip)) {
                IOUtils.copy(stream, zipArchiveOutputStream);
            } catch (Exception e) {
                ModConstants.LOG.error("Failed to add file {}, because: {}", fileToZip.getAbsolutePath(), e.getMessage());
            } finally {
                zipArchiveOutputStream.closeEntry();
            }
        } else {
            File[] files = fileToZip.listFiles();
            if (files == null)
                return;

            for (File file: files) {
                if (!file.getName().contains("mbackups")) {
                    addFileToZipStream(zipArchiveOutputStream, file, entryName);
                }
            }
        }
    }

}
