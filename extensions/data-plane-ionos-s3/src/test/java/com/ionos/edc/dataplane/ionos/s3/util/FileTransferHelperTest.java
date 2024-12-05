package com.ionos.edc.dataplane.ionos.s3.util;

import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileTransferHelperTest {

    @Test
    public void calculateChunkSize_0Bytes() {
        var chunkSize = FileTransferHelper.calculateChunkSize(0);
        assertEquals(0, chunkSize);
    }

    @Test
    public void calculateChunkSize_1MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(1048576);
        assertEquals(1048576, chunkSize);
    }

    @Test
    public void calculateChunkSize_2MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(2097152);
        assertEquals(1048576, chunkSize);
    }

    @Test
    public void calculateChunkSize_3MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(3145728);
        assertEquals(1048576, chunkSize);
    }

    @Test
    public void calculateChunkSize_4MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(4194304);
        assertEquals(1048576, chunkSize);
    }

    @Test
    public void calculateChunkSize_5MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(5242880);
        assertEquals(1048576, chunkSize);
    }

    @Test
    public void calculateChunkSize_6MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(6291456);
        assertEquals(1048576, chunkSize);
    }

    @Test
    public void calculateChunkSize_7MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(7340032);
        assertEquals(1048576, chunkSize);
    }

    @Test
    public void calculateChunkSize_8MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(8388608);
        assertEquals(1048576, chunkSize);
    }

    @Test
    public void calculateChunkSize_9MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(9437184);
        assertEquals(1048576, chunkSize);
    }

    @Test
    public void calculateChunkSize_10MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(10485760);
        assertEquals(1048576, chunkSize);
    }

    @Test
    public void calculateChunkSize_20MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(20971520);
        assertEquals(1048576, chunkSize);
    }

    @Test
    public void calculateChunkSize_30MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(31457280);
        assertEquals(2097152, chunkSize);
    }

    @Test
    public void calculateChunkSize_40MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(41943040);
        assertEquals(2097152, chunkSize);
    }

    @Test
    public void calculateChunkSize_50MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(52428800);
        assertEquals(3145728, chunkSize);
    }

    @Test
    public void calculateChunkSize_60MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(62914560);
        assertEquals(3145728, chunkSize);
    }

    @Test
    public void calculateChunkSize_70MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(73400320);
        assertEquals(4194304, chunkSize);
    }

    @Test
    public void calculateChunkSize_80MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(83886080);
        assertEquals(4194304, chunkSize);
    }

    @Test
    public void calculateChunkSize_90MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(94371840);
        assertEquals(5242880, chunkSize);
    }

    @Test
    public void calculateChunkSize_100MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(104857600);
        assertEquals(5242880, chunkSize);
    }

    @Test
    public void calculateChunkSize_200MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(209715200);
        assertEquals(10485760, chunkSize);
    }

    @Test
    public void calculateChunkSize_300MB() {
        var chunkSize = FileTransferHelper.calculateChunkSize(209715200);
        assertEquals(10485760, chunkSize);
    }

    @Test
    public void calculateChunkSize_UnsupportedSize() {
        assertThrows(EdcException.class,
            ()-> FileTransferHelper.calculateChunkSize(10737418240L));
    }
}
