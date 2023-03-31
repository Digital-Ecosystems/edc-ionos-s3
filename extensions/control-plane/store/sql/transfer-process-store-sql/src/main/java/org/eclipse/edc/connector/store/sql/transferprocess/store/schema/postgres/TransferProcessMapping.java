/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.store.sql.transferprocess.store.schema.postgres;

import org.eclipse.edc.connector.store.sql.transferprocess.store.schema.TransferProcessStoreStatements;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.sql.translation.JsonFieldMapping;
import org.eclipse.edc.sql.translation.TranslationMapping;

/**
 * Maps fields of a {@link TransferProcess} onto the
 * corresponding SQL schema (= column names) enabling access through Postgres JSON operators where applicable
 */
public class TransferProcessMapping extends TranslationMapping {

    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_CREATED_TIMESTAMP = "createdAt";
    private static final String FIELD_TRACECONTEXT = "traceContext";
    private static final String FIELD_ERRORDETAIL = "errorDetail";
    private static final String FIELD_DATAREQUEST = "dataRequest";
    private static final String FIELD_DATAADDRESS = "dataAddress";
    // this actually an alias for "dataAddress":
    private static final String FIELD_CONTENTDATAADDRESS = "contentDataAddress";
    private static final String FIELD_RESOURCE_MANIFEST = "resourceManifest";
    private static final String FIELD_PROVISIONED_RESOURCE_SET = "provisionedResourceSet";
    private static final String FIELD_DEPROVISIONED_RESOURCES = "deprovisionedResources";

    private static final String FIELD_PROPERTIES = "properties";


    public TransferProcessMapping(TransferProcessStoreStatements statements) {
        add(FIELD_ID, statements.getIdColumn());
        add(FIELD_TYPE, statements.getTypeColumn());
        add(FIELD_CREATED_TIMESTAMP, statements.getCreatedAtColumn());
        add(FIELD_TRACECONTEXT, new JsonFieldMapping(statements.getTraceContextColumn()));
        add(FIELD_ERRORDETAIL, statements.getErrorDetailColumn());
        add(FIELD_DATAREQUEST, new DataRequestMapping(statements));
        add(FIELD_DATAADDRESS, new JsonFieldMapping(statements.getContentDataAddressColumn()));
        add(FIELD_CONTENTDATAADDRESS, new JsonFieldMapping(statements.getContentDataAddressColumn()));
        add(FIELD_RESOURCE_MANIFEST, new ResourceManifestMapping());
        add(FIELD_PROPERTIES, new JsonFieldMapping(statements.getPropertiesColumn()));
        add(FIELD_PROVISIONED_RESOURCE_SET, new ProvisionedResourceSetMapping());
        // using the alias instead of the actual column name to avoid name clashes.
        add(FIELD_DEPROVISIONED_RESOURCES, new JsonFieldMapping(PostgresDialectStatements.DEPROVISIONED_RESOURCES_ALIAS));
    }
}
