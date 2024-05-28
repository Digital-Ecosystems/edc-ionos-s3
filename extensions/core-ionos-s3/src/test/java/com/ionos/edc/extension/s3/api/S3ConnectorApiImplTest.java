package com.ionos.edc.extension.s3.api;

import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class S3ConnectorApiImplTest {

    @Test
    public void getRegion_Frankfurt() {
        var endpoint = "https://s3-eu-central-1.ionoscloud.com";
        var region = S3ConnectorApiImpl.getRegion(endpoint);

        assertEquals("de", region);
    }

    @Test
    public void getRegion_Berlin() {
        var endpoint = "https://s3-eu-central-2.ionoscloud.com";
        var region = S3ConnectorApiImpl.getRegion(endpoint);

        assertEquals("eu-central-2", region);
    }

    @Test
    public void getRegion_Logrono() {
        var endpoint = "https://s3-eu-south-2.ionoscloud.com";
        var region = S3ConnectorApiImpl.getRegion(endpoint);

        assertEquals("eu-south-2", region);
    }

    @Test
    public void getRegion_Invalid() {
        var endpoint = "https://s3-de-central.profitbricks.com";

        Exception exception = assertThrows(EdcException.class, () -> {
            S3ConnectorApiImpl.getRegion(endpoint);
        });

        assertNotNull(exception);
    }
}
