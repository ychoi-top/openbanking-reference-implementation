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
package com.forgerock.openbanking.jwkms;

import com.forgerock.cert.Psd2CertInfo;
import com.forgerock.cert.psd2.RolesOfPsp;
import com.forgerock.openbanking.common.OBRIInternalCertificates;
import com.forgerock.openbanking.jwkms.service.application.ApplicationService;
import com.forgerock.openbanking.model.OBRIRole;
import dev.openbanking4.spring.security.multiauth.model.granttypes.PSD2GrantType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.forgerock.openbanking.common.CertificateHelper.getCn;
import static com.forgerock.openbanking.common.CertificateHelper.isCertificateIssuedByCA;

/**
 * A specific variation of {@link com.forgerock.openbanking.common.OBRIInternalCertificates} for the jwkms application.
 */
@Slf4j
class JwkMsOBRIInternalCertificates extends OBRIInternalCertificates {

    private final X509Certificate caCertificate;
    private final ApplicationService applicationService;

    JwkMsOBRIInternalCertificates(X509Certificate caCertificate, ApplicationService applicationService) {
        super(caCertificate);
        this.caCertificate = caCertificate;
        this.applicationService = applicationService;
    }

    @Override
    public Set<GrantedAuthority> getAuthorities(X509Certificate[] certificatesChain, Psd2CertInfo psd2CertInfo, RolesOfPsp roles) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (roles != null) {
            authorities.addAll(roles.getRolesOfPsp().stream().map(PSD2GrantType::new).collect(Collectors.toSet()));
        }

        if (isCertificateIssuedByCA(caCertificate, certificatesChain)) {
            authorities.add(OBRIRole.ROLE_FORGEROCK_INTERNAL_APP);
            authorities.add(OBRIRole.ROLE_JWKMS_APP);
        }
        return authorities;
    }

    @Override
    public String getUserName(X509Certificate[] certificatesChain) {
        if (!isCertificateIssuedByCA(caCertificate, certificatesChain)) {
            return null;
        }
        return applicationService.getApplication(getCn(certificatesChain[0])).getIssuerId();
    }

}
