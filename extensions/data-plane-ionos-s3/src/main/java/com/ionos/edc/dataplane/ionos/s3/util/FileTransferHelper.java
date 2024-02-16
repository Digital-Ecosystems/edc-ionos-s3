package com.ionos.edc.dataplane.ionos.s3.util;

import org.eclipse.edc.spi.EdcException;

public class FileTransferHelper {

    private static final long MEGABYTE = 1048576;

    public static long calculateChunkSize(long fileSize) {
        if (fileSize == 0)
            return 0;

        if (fileSize <= MEGABYTE) {
            return MEGABYTE;
        }

        var fileSizeMB = fileSize / MEGABYTE;

        if ((fileSizeMB >= 1) && (fileSizeMB <= 20)) {
            return MEGABYTE;
        } else if ((fileSizeMB > 20) && (fileSizeMB <= 40)) {
            return 2 * MEGABYTE;
        } else if ((fileSizeMB > 40) && (fileSizeMB <= 60)) {
            return 3 * MEGABYTE;
        } else if ((fileSizeMB > 60) && (fileSizeMB <= 80)) {
            return 4 * MEGABYTE;
        } else if ((fileSizeMB > 80) && (fileSizeMB <= 100)) {
            return 5 * MEGABYTE;
        } else if ((fileSizeMB > 100) && (fileSizeMB <= 200)) {
            return 10 * MEGABYTE;
        } else if ((fileSizeMB > 200) && (fileSizeMB <= 300)) {
            return 15 * MEGABYTE;
        } else {
            throw new EdcException("Unsupported file size: " + fileSize + " bytes");
        }
    }
}