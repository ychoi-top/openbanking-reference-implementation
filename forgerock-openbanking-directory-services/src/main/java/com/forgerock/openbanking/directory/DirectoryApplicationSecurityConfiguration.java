/**
 * Copyright 2019 ForgeRock AS.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.forgerock.openbanking.directory;

import com.forgerock.openbanking.common.OBRIInternalCertificates;
import com.forgerock.openbanking.directory.service.DirectoryUtilsService;
import com.forgerock.openbanking.jwt.services.CryptoApiClient;
import com.forgerock.openbanking.ssl.services.keystore.KeyStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import java.security.cert.X509Certificate;

import static com.forgerock.openbanking.common.CookieHttpSecurityConfiguration.configureHttpSecurity;

@Configuration
class DirectoryApplicationSecurityConfiguration {

    @Configuration
    static class AuthWebSecurity extends WebSecurityConfigurerAdapter {

        @Value("${matls.forgerock-internal-ca-alias}")
        private String internalCaAlias;
        @Value("${matls.forgerock-external-ca-alias}")
        private String externalCaAlias;

        private final CryptoApiClient cryptoApiClient;
        private final DirectoryUtilsService directoryUtilsService;
        private final KeyStoreService keyStoreService;

        @Autowired
        AuthWebSecurity(CryptoApiClient cryptoApiClient, DirectoryUtilsService directoryUtilsService, KeyStoreService keyStoreService) {
            this.cryptoApiClient = cryptoApiClient;
            this.directoryUtilsService = directoryUtilsService;
            this.keyStoreService = keyStoreService;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            X509Certificate internalCACertificate = (X509Certificate) keyStoreService.getKeyStore().getCertificate(internalCaAlias);
            X509Certificate externalCACertificate = (X509Certificate) keyStoreService.getKeyStore().getCertificate(externalCaAlias);

            OBRIInternalCertificates obriInternalCertificates = new OBRIInternalCertificates(internalCACertificate);
            DirectoryORBIExternalCertificates obriExternalCertificates = new DirectoryORBIExternalCertificates(externalCACertificate, directoryUtilsService);

            configureHttpSecurity(http, obriInternalCertificates, obriExternalCertificates, cryptoApiClient);
        }
    }
}
