package com.ionos.edc.extension.s3.connector.ionosapi;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.*;
import org.eclipse.edc.spi.EdcException;

public class S3ApiConnector {
    private static final String BASE_URL = "https://s3.ionos.com";

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public S3ApiConnector() {
        client = new OkHttpClient();
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public S3AccessKey createAccessKey(String token) {
        String url = BASE_URL + "/accesskeys";

        Request request = new Request.Builder().url(url)
                .addHeader("Authorization", "Bearer " + token)
                .post(RequestBody.create(MediaType.get("application/json"), new byte[0]))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code [" + response + "] creating S3 accesskey");
            }

            if (response.body() == null)
                throw new IOException("Empty response body creating S3 accesskey");
            else
                return objectMapper.readValue(response.body().string(), S3AccessKey.class);

        } catch (IOException e) {
            throw new EdcException("Error creating S3 accesskey", e);
        }
    }

    public S3AccessKey retrieveAccessKey(String token, String keyID) {
        String url = BASE_URL + "/accesskeys/" + keyID;

        Request request = new Request.Builder().url(url)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code [" + response + "] retrieving S3 accesskey");
            }

            if (response.body() == null)
                throw new IOException("Empty response body retrieving S3 accesskey");
            else
                return objectMapper.readValue(response.body().string(), S3AccessKey.class);

        } catch (IOException e) {
            throw new EdcException("Error retrieving S3 accesskey", e);
        }
    }

    public void deleteAccessKey(String token, String keyID) {
        String url = BASE_URL + "/accesskeys/" + keyID;

        Request request = new Request.Builder().url(url)
                //This adds the token to the header.
                .addHeader("Authorization", "Bearer " + token)
                .delete()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code [" + response + "] deleting S3 accesskey");
            }
        } catch (IOException e) {
            throw new EdcException("Error deleting S3 accesskey", e);
        }
    }
}
