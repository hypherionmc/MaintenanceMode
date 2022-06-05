package me.hypherionmc.mmode.util;

import me.hypherionmc.mmode.ModConstants;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;

public class BackupUtil {

    static final DateTimeFormatter FORMATTER = (new DateTimeFormatterBuilder()).appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD).appendLiteral('-').appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral('_').appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral('-').appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral('-').appendValue(ChronoField.SECOND_OF_MINUTE, 2).toFormatter();

    public static void createBackup() throws IOException {
        File worldDir = new File("world");
        File backupDir = new File("mbackups");

        if (!worldDir.exists()) {
            return;
        }

        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        String s = LocalDateTime.now().format(FORMATTER) + ".zip";
        Thread backupThread = new Thread() {

            {
                this.setName("Maintenance Mode Backup Thread");
            }

            @Override
            public void run() {
                createZipFile(backupDir + File.separator + s, System.getProperty("user.dir"));
            }
        };
        backupThread.start();
    }


    public static void createZipFile(String zipFileName, String fileOrDirectoryToZip) {
        BufferedOutputStream bufferedOutputStream = null;
        ZipArchiveOutputStream zipArchiveOutputStream = null;
        OutputStream outputStream = null;
        try {
            Path zipFilePath = Paths.get(zipFileName);
            outputStream = Files.newOutputStream(zipFilePath);
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            zipArchiveOutputStream = new ZipArchiveOutputStream(bufferedOutputStream);
            File fileToZip = new File(fileOrDirectoryToZip);

            addFileToZipStream(zipArchiveOutputStream, fileToZip, "");

            zipArchiveOutputStream.close();
            bufferedOutputStream.close();
            outputStream.close();
            IOUtils.closeQuietly(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ModConstants.LOG.info("Created backup at {}", zipFileName);
    }

    private static void addFileToZipStream(ZipArchiveOutputStream zipArchiveOutputStream, File fileToZip, String base) throws IOException {
        String entryName = base + fileToZip.getName();
        ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(fileToZip, entryName);
        zipArchiveOutputStream.putArchiveEntry(zipArchiveEntry);
        if(fileToZip.isFile() && !fileToZip.getName().contains(".lock")) {
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(fileToZip);
                IOUtils.copy(fileInputStream, zipArchiveOutputStream);
                zipArchiveOutputStream.closeArchiveEntry();
            } catch (Exception e) {
                ModConstants.LOG.error("Failed to add file {}, because: {}", fileToZip.getAbsolutePath(), e.getMessage());
            } finally {
                IOUtils.closeQuietly(fileInputStream);
            }
        } else {
            zipArchiveOutputStream.closeArchiveEntry();
            File[] files = fileToZip.listFiles();
            if(files != null) {
                for (File file: files) {
                    if (!file.getName().contains("mbackups")) {
                        addFileToZipStream(zipArchiveOutputStream, file, entryName + "/");
                    }
                }
            }
        }
    }

}
