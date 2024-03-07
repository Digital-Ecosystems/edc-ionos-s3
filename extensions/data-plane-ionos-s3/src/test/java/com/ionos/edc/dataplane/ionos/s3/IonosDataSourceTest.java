/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.ionos.edc.dataplane.ionos.s3;

import com.ionos.edc.extension.s3.api.S3ConnectorApi;
import com.ionos.edc.extension.s3.api.S3Object;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

class IonosDataSourceTest {

    private static final String TEST_BUCKET = "bucket1";

    private static final String TEST_FILE_1_NAME = "device1-data.csv";
    private static final int TEST_FILE_1_SIZE = 1024;
    private static final String TEST_FILE_2_NAME = "device2-data.csv";
    private static final int TEST_FILE_2_SIZE = 2048;
    private static final String TEST_FILE_3_NAME = "device3-data.csv";
    private static final int TEST_FILE_3_SIZE = 3072;
    private static final String TEST_FILE_4_NAME = "device4-data.csv";
    private static final int TEST_FILE_4_SIZE = 4096;

    private static final String TEST_FOLDER_NAME = "devices/";
    private static final String TEST_SUB_FOLDER_1_NAME = "device1/";
    private static final String TEST_SUB_FOLDER_2_NAME = "device2/";

    @Mock
    private S3ConnectorApi s3Api;
    @Mock
    private Monitor monitor;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void openPartStream_empty() {

        doReturn(List.of())
                .when(s3Api).listObjects(any(String.class), any(String.class));

        var dataSource = IonosDataSource.Builder.newInstance()
                .client(s3Api)
                .monitor(monitor)
                .bucketName(TEST_BUCKET)
                .blobName(TEST_FILE_1_NAME)
                .build();

        var stream = dataSource.openPartStream();
        assertTrue(stream.failed());
        assertNull(stream.getContent());
        assertEquals(StreamFailure.Reason.NOT_FOUND, stream.getFailure().getReason());
    }

    @Test
    public void openPartStream_singleFile() {

        var s3Objects = List.of(new S3Object(TEST_FILE_1_NAME, TEST_FILE_1_SIZE));
        doReturn(s3Objects)
                .when(s3Api).listObjects(any(String.class), any(String.class));

        var dataSource = IonosDataSource.Builder.newInstance()
                .client(s3Api)
                .monitor(monitor)
                .bucketName(TEST_BUCKET)
                .blobName(TEST_FILE_1_NAME)
                .build();

        var stream = dataSource.openPartStream();
        assertTrue(stream.succeeded());

        var parts = stream.getContent().collect(Collectors.toList());
        assertEquals(1, parts.size());

        var part = parts.get(0);
        assertEquals(TEST_FILE_1_NAME, part.name());
        assertEquals(TEST_FILE_1_SIZE, part.size());
    }

    @Test
    public void openPartStream_folder() {

        var s3Objects = List.of(new S3Object(TEST_FOLDER_NAME, 0),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_1_NAME, TEST_FILE_1_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_2_NAME, TEST_FILE_2_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_3_NAME, TEST_FILE_3_SIZE)
        );
        doReturn(s3Objects)
                .when(s3Api).listObjects(any(String.class), any(String.class));

        var dataSource = IonosDataSource.Builder.newInstance()
                .client(s3Api)
                .monitor(monitor)
                .bucketName(TEST_BUCKET)
                .blobName(TEST_FOLDER_NAME)
                .build();

        var stream = dataSource.openPartStream();
        assertTrue(stream.succeeded());

        var parts = stream.getContent().collect(Collectors.toList());
        assertEquals(4, parts.size());

        var partFolder = parts.get(0);
        assertEquals(TEST_FOLDER_NAME, partFolder.name());
        assertEquals(0, partFolder.size());

        var partFile1 = parts.get(1);
        assertEquals(TEST_FOLDER_NAME + TEST_FILE_1_NAME, partFile1.name());
        assertEquals(TEST_FILE_1_SIZE, partFile1.size());

        var partFile2 = parts.get(2);
        assertEquals(TEST_FOLDER_NAME + TEST_FILE_2_NAME, partFile2.name());
        assertEquals(TEST_FILE_2_SIZE, partFile2.size());

        var partFile3 = parts.get(3);
        assertEquals(TEST_FOLDER_NAME + TEST_FILE_3_NAME, partFile3.name());
        assertEquals(TEST_FILE_3_SIZE, partFile3.size());
    }

    @Test
    public void openPartStream_folder_includeFiles() {

        var s3Objects = List.of(new S3Object(TEST_FOLDER_NAME, 0),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_1_NAME, TEST_FILE_1_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_2_NAME, TEST_FILE_2_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_3_NAME, TEST_FILE_3_SIZE)
        );
        doReturn(s3Objects)
                .when(s3Api).listObjects(any(String.class), any(String.class));

        var dataSource = IonosDataSource.Builder.newInstance()
                .client(s3Api)
                .monitor(monitor)
                .bucketName(TEST_BUCKET)
                .blobName(TEST_FOLDER_NAME)
                .filterIncludes("device[1-2]-data.csv")
                .build();

        var stream = dataSource.openPartStream();
        assertTrue(stream.succeeded());

        var parts = stream.getContent().collect(Collectors.toList());
        assertEquals(3, parts.size());

        var partFolder = parts.get(0);
        assertEquals(TEST_FOLDER_NAME, partFolder.name());
        assertEquals(0, partFolder.size());

        var partFile1 = parts.get(1);
        assertEquals(TEST_FOLDER_NAME + TEST_FILE_1_NAME, partFile1.name());
        assertEquals(TEST_FILE_1_SIZE, partFile1.size());

        var partFile2 = parts.get(2);
        assertEquals(TEST_FOLDER_NAME + TEST_FILE_2_NAME, partFile2.name());
        assertEquals(TEST_FILE_2_SIZE, partFile2.size());
    }

    @Test
    public void openPartStream_folder_excludeFiles() {

        var s3Objects = List.of(new S3Object(TEST_FOLDER_NAME, 0),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_1_NAME, TEST_FILE_1_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_2_NAME, TEST_FILE_2_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_3_NAME, TEST_FILE_3_SIZE)
        );
        doReturn(s3Objects)
                .when(s3Api).listObjects(any(String.class), any(String.class));

        var dataSource = IonosDataSource.Builder.newInstance()
                .client(s3Api)
                .monitor(monitor)
                .bucketName(TEST_BUCKET)
                .blobName(TEST_FOLDER_NAME)
                .filterExcludes("device[1-2]-data.csv")
                .build();

        var stream = dataSource.openPartStream();
        assertTrue(stream.succeeded());

        var parts = stream.getContent().collect(Collectors.toList());
        assertEquals(2, parts.size());

        var partFolder = parts.get(0);
        assertEquals(TEST_FOLDER_NAME, partFolder.name());
        assertEquals(0, partFolder.size());

        var partFile1 = parts.get(1);
        assertEquals(TEST_FOLDER_NAME + TEST_FILE_3_NAME, partFile1.name());
        assertEquals(TEST_FILE_3_SIZE, partFile1.size());
    }

    @Test
    public void openPartStream_subFolder() {

        var s3Objects = List.of(new S3Object(TEST_FOLDER_NAME, 0),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_1_NAME, TEST_FILE_1_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_2_NAME, TEST_FILE_2_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME, 0),
                new S3Object(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME + TEST_FILE_3_NAME, TEST_FILE_3_SIZE)
        );
        doReturn(s3Objects)
                .when(s3Api).listObjects(any(String.class), any(String.class));

        var dataSource = IonosDataSource.Builder.newInstance()
                .client(s3Api)
                .monitor(monitor)
                .bucketName(TEST_BUCKET)
                .blobName(TEST_FOLDER_NAME)
                .build();

        var stream = dataSource.openPartStream();
        assertTrue(stream.succeeded());

        var parts = stream.getContent().collect(Collectors.toList());
        assertEquals(5, parts.size());

        var partFolder = parts.get(0);
        assertEquals(TEST_FOLDER_NAME, partFolder.name());
        assertEquals(0, partFolder.size());

        var partFile1 = parts.get(1);
        assertEquals(TEST_FOLDER_NAME + TEST_FILE_1_NAME, partFile1.name());
        assertEquals(TEST_FILE_1_SIZE, partFile1.size());

        var partFile2 = parts.get(2);
        assertEquals(TEST_FOLDER_NAME + TEST_FILE_2_NAME, partFile2.name());
        assertEquals(TEST_FILE_2_SIZE, partFile2.size());

        var parSubFolder1 = parts.get(3);
        assertEquals(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME, parSubFolder1.name());
        assertEquals(0, parSubFolder1.size());

        var partFile3 = parts.get(4);
        assertEquals(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME + TEST_FILE_3_NAME, partFile3.name());
        assertEquals(TEST_FILE_3_SIZE, partFile3.size());
    }

    @Test
    public void openPartStream_subFolder_includeFilesInFolder() {

        var s3Objects = List.of(new S3Object(TEST_FOLDER_NAME, 0),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_1_NAME, TEST_FILE_1_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_2_NAME, TEST_FILE_2_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME, 0),
                new S3Object(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME + TEST_FILE_3_NAME, TEST_FILE_3_SIZE)
        );
        doReturn(s3Objects)
                .when(s3Api).listObjects(any(String.class), any(String.class));

        var dataSource = IonosDataSource.Builder.newInstance()
                .client(s3Api)
                .monitor(monitor)
                .bucketName(TEST_BUCKET)
                .blobName(TEST_FOLDER_NAME)
                .filterIncludes("device[1-2]-data.csv")
                .build();

        var stream = dataSource.openPartStream();
        assertTrue(stream.succeeded());

        var parts = stream.getContent().collect(Collectors.toList());
        assertEquals(3, parts.size());

        var partFolder = parts.get(0);
        assertEquals(TEST_FOLDER_NAME, partFolder.name());
        assertEquals(0, partFolder.size());

        var partFile1 = parts.get(1);
        assertEquals(TEST_FOLDER_NAME + TEST_FILE_1_NAME, partFile1.name());
        assertEquals(TEST_FILE_1_SIZE, partFile1.size());

        var partFile2 = parts.get(2);
        assertEquals(TEST_FOLDER_NAME + TEST_FILE_2_NAME, partFile2.name());
        assertEquals(TEST_FILE_2_SIZE, partFile2.size());
    }

    @Test
    public void openPartStream_subFolder_excludeFilesInFolder() {

        var s3Objects = List.of(new S3Object(TEST_FOLDER_NAME, 0),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_1_NAME, TEST_FILE_1_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_2_NAME, TEST_FILE_2_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME, 0),
                new S3Object(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME + TEST_FILE_3_NAME, TEST_FILE_3_SIZE)
        );
        doReturn(s3Objects)
                .when(s3Api).listObjects(any(String.class), any(String.class));

        var dataSource = IonosDataSource.Builder.newInstance()
                .client(s3Api)
                .monitor(monitor)
                .bucketName(TEST_BUCKET)
                .blobName(TEST_FOLDER_NAME)
                .filterExcludes("device1-data.csv")
                .build();

        var stream = dataSource.openPartStream();
        assertTrue(stream.succeeded());

        var parts = stream.getContent().collect(Collectors.toList());
        assertEquals(4, parts.size());

        var partFolder = parts.get(0);
        assertEquals(TEST_FOLDER_NAME, partFolder.name());
        assertEquals(0, partFolder.size());

        var partFile2 = parts.get(1);
        assertEquals(TEST_FOLDER_NAME + TEST_FILE_2_NAME, partFile2.name());
        assertEquals(TEST_FILE_2_SIZE, partFile2.size());

        var partSubFolder1 = parts.get(2);
        assertEquals(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME, partSubFolder1.name());
        assertEquals(0, partSubFolder1.size());

        var partFile3 = parts.get(3);
        assertEquals(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME + TEST_FILE_3_NAME, partFile3.name());
        assertEquals(TEST_FILE_3_SIZE, partFile3.size());
    }

    @Test
    public void openPartStream_subFolder_includeFilesInSubFolder() {

        var s3Objects = List.of(new S3Object(TEST_FOLDER_NAME, 0),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_1_NAME, TEST_FILE_1_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_2_NAME, TEST_FILE_2_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME, 0),
                new S3Object(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME + TEST_FILE_3_NAME, TEST_FILE_3_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME + TEST_FILE_4_NAME, TEST_FILE_4_SIZE)
        );
        doReturn(s3Objects)
                .when(s3Api).listObjects(any(String.class), any(String.class));

        var dataSource = IonosDataSource.Builder.newInstance()
                .client(s3Api)
                .monitor(monitor)
                .bucketName(TEST_BUCKET)
                .blobName(TEST_FOLDER_NAME)
                .filterIncludes("device1/device[3-4]-data.csv")
                .build();

        var stream = dataSource.openPartStream();
        assertTrue(stream.succeeded());

        var parts = stream.getContent().collect(Collectors.toList());
        assertEquals(3, parts.size());

        var partFolder = parts.get(0);
        assertEquals(TEST_FOLDER_NAME, partFolder.name());
        assertEquals(0, partFolder.size());

        var partFile3 = parts.get(1);
        assertEquals(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME + TEST_FILE_3_NAME, partFile3.name());
        assertEquals(TEST_FILE_3_SIZE, partFile3.size());

        var partFile4 = parts.get(2);
        assertEquals(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME + TEST_FILE_4_NAME, partFile4.name());
        assertEquals(TEST_FILE_4_SIZE, partFile4.size());
    }

    @Test
    public void openPartStream_subFolder_excludeFilesInSubFolder() {

        var s3Objects = List.of(new S3Object(TEST_FOLDER_NAME, 0),
                new S3Object(TEST_FOLDER_NAME + TEST_FILE_1_NAME, TEST_FILE_1_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME, 0),
                new S3Object(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME + TEST_FILE_3_NAME, TEST_FILE_3_SIZE),
                new S3Object(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME + TEST_FILE_4_NAME, TEST_FILE_4_SIZE)
        );
        doReturn(s3Objects)
                .when(s3Api).listObjects(any(String.class), any(String.class));

        var dataSource = IonosDataSource.Builder.newInstance()
                .client(s3Api)
                .monitor(monitor)
                .bucketName(TEST_BUCKET)
                .blobName(TEST_FOLDER_NAME)
                .filterExcludes("device1/device4-data.csv")
                .build();

        var stream = dataSource.openPartStream();
        assertTrue(stream.succeeded());

        var parts = stream.getContent().collect(Collectors.toList());
        assertEquals(4, parts.size());

        var partFolder = parts.get(0);
        assertEquals(TEST_FOLDER_NAME, partFolder.name());
        assertEquals(0, partFolder.size());

        var partFile1 = parts.get(1);
        assertEquals(TEST_FOLDER_NAME + TEST_FILE_1_NAME, partFile1.name());
        assertEquals(TEST_FILE_1_SIZE, partFile1.size());

        var parSubFolder1 = parts.get(2);
        assertEquals(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME, parSubFolder1.name());
        assertEquals(0, parSubFolder1.size());

        var partFile3 = parts.get(3);
        assertEquals(TEST_FOLDER_NAME + TEST_SUB_FOLDER_1_NAME + TEST_FILE_3_NAME, partFile3.name());
        assertEquals(TEST_FILE_3_SIZE, partFile3.size());
    }

}