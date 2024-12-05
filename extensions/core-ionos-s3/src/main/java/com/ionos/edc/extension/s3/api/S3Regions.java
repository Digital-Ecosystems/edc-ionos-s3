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

import java.util.List;

public class S3Regions {

	private List<S3Region> items;

	public List<S3Region> getItems() {
		return items;
	}

	public void setItems(List<S3Region> items) {
		this.items = items;
	}
}