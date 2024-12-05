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

public class S3AccessKey {
	public static final String AVAILABLE_STATUS = "AVAILABLE";

    private String id;
    private Metadata metadata;
    private Properties properties;

	public String getId() {
		return id;
	}
	public Metadata getMetadata() {
		return metadata;
	}
	public Properties getProperties() {
		return properties;
	}

	public static class Metadata {
		private String status;

		public String getStatus() {
			return status;
		}
	}

	public static class Properties {
		private String accessKey;
		private String secretKey;

		public String getAccessKey() {
			return accessKey;
		}
		public String getSecretKey() {
			return secretKey;
		}
	}
}