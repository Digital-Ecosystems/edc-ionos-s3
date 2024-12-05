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

public class S3Region {

    private String id;
    private Properties properties;

	public String getId() {
		return id;
	}
	public Properties getProperties() {
		return properties;
	}

	public static class Properties {
		private String endpoint;

		public String getEndpoint() {
			return endpoint;
		}
	}
}