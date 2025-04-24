/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 *
 */

plugins {
    alias(libs.plugins.tck.generator)
}

dependencies {
    implementation(libs.tck.common.api)
    implementation(project(":core"))
    implementation(project(":dsp:dsp-api"))
    testImplementation(project(":dsp:dsp-system"))
}

tasks.test {
    systemProperty("dataspacetck.launcher", "org.eclipse.dataspacetck.dsp.system.DspSystemLauncher")
    systemProperty("dataspacetck.dsp.local.connector", "false")
    systemProperty("dataspacetck.dsp.connector.http.url", "http://localhost:8282/api/dsp/one/2025-1/")
    systemProperty(
        "dataspacetck.dsp.connector.negotiation.initiate.url",
        "http://localhost:8687/tck/negotiations/requests"
    )
    systemProperty("CN_01_01_DATASETID", "ACN0101")
    systemProperty("CN_01_01_OFFERID", "CD123:ACN0101:456")
    systemProperty("CN_01_02_DATASETID", "ACN0102")
    systemProperty("CN_01_02_OFFERID", "CD123:ACN0102:456")
    systemProperty("CN_01_03_DATASETID", "ACN0103")
    systemProperty("CN_01_03_OFFERID", "CD123:ACN0103:456")
    systemProperty("CN_01_04_DATASETID", "ACN0104")
    systemProperty("CN_01_04_OFFERID", "CD123:ACN0104:456")
    systemProperty("dataspacetck.dsp.connector.agent.id", "CONNECTOR_UNDER_TEST")
    systemProperty("dataspacetck.debug", "true")
}
