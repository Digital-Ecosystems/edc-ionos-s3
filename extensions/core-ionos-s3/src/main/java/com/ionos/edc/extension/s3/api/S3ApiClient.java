/*
 *  Copyright (c) 2024 IONOS
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *      IONOS
 *
 */

package com.ionos.edc.extension.s3.api;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.spi.EdcException;

public class S3ApiClient {
    private static final String BASE_URL = "https://s3.ionos.com";
    private static final String REGIONS_ENDPOINT_URL = BASE_URL + "/regions";
    private static final String ACCESS_KEYS_ENDPOINT_URL = BASE_URL + "/accesskeys";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TOKEN_PREFIX = "Bearer ";
    private static final String JSON_MEDIA_TYPE = "application/json";

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public S3ApiClient() {
        client = new OkHttpClient();
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public S3Regions retrieveRegions(String token) {

        Request request = new Request.Builder().url(REGIONS_ENDPOINT_URL)
                .addHeader(AUTHORIZATION_HEADER, BEARER_TOKEN_PREFIX + token)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new EdcException("Unexpected code [" + response.code() + "] retrieving S3 regions");
            }

            if (response.body() == null)
                throw new IOException("Empty response body retrieving S3 regions");
            else
                return objectMapper.readValue(response.body().string(), new TypeReference<S3Regions>() {});

        } catch (IOException e) {
            throw new EdcException("Error retrieving S3 accesskey", e);
        }
    }

    public boolean verifyToken(String token) {
        if(token == null || token.isEmpty())
            return false;

        String url = "https://api.ionos.com/cloudapi/v6/locations";

        Request request = new Request.Builder().url(url)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return false;

            } else return response.body() != null;
        } catch (IOException e) {
            throw new EdcException("Error access Ionos Cloud", e);

        }
    }

    public S3AccessKey createAccessKey(String token) {

        Request request = new Request.Builder().url(ACCESS_KEYS_ENDPOINT_URL)
                .addHeader(AUTHORIZATION_HEADER, BEARER_TOKEN_PREFIX + token)
                .post(RequestBody.create(MediaType.get(JSON_MEDIA_TYPE), new byte[0]))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new EdcException("Unexpected code [" + response.code() + "] creating S3 accesskey");
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
        String url = ACCESS_KEYS_ENDPOINT_URL + "/" + keyID;

        Request request = new Request.Builder().url(url)
                .addHeader(AUTHORIZATION_HEADER, BEARER_TOKEN_PREFIX + token)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new EdcException("Unexpected code [" + response.code() + "] retrieving S3 accesskey");
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
        String url = ACCESS_KEYS_ENDPOINT_URL + "/" + keyID;

        Request request = new Request.Builder().url(url)
                .addHeader(AUTHORIZATION_HEADER, BEARER_TOKEN_PREFIX + token)
                .delete()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new EdcException("Unexpected code [" + response.code() + "] deleting S3 accesskey");
            }
        } catch (IOException e) {
            throw new EdcException("Error deleting S3 accesskey", e);
        }
    }
}
