/*
 *  Copyright (c) 2024 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - added dependencies
 *       ZF Friedrichshafen AG - add dependency
 */

plugins {
    `java-library`
    id("application")
}

val edcGroup: String by project
val edcVersion: String by project

dependencies {
    implementation(project(":launchers:base:connector"))

    implementation("${edcGroup}:configuration-filesystem:${edcVersion}")
    implementation("${edcGroup}:vault-hashicorp:${edcVersion}")
    implementation("${edcGroup}:iam-mock:${edcVersion}")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
