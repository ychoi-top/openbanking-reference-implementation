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
package com.forgerock.openbanking.gateway;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertNotEquals;


@TestPropertySource(properties = {"forgerock.whitelist = 127.0.0.1/32"})
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ForgerockOpenbankingGatewayApplicationAsApiIT {
    @LocalServerPort
    private int port;
    private WebTestClient.Builder webClient;
    @Rule
    public WireMockClassRule asApiMock = new WireMockClassRule(WireMockSpring.options().httpsPort(8066).keystorePath("src/test/resources/matls-as.jks").keystorePassword("changeit"));

    @Before
    public void setup() throws SSLException {

        SslContext sslContext = SslContextBuilder
                .forClient()
                .sslProvider(SslProvider.JDK)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(
                HttpClient.create().secure(t -> t.sslContext(sslContext)

                ));


        this.webClient = WebTestClient.bindToServer(connector).responseTimeout(Duration.ofSeconds(10));
    }

    @Test
    public void matlsAsAuthorizeShouldForwardToAsApi() {
        String baseUri = "https://matls.as.aspsp.dev-ob.forgerock.financial:" + port;
        // Given
        asApiMock.stubFor(get(urlEqualTo("/oauth2/authorize"))
                .willReturn(aResponse()
                        .withStatus(200)));

        // When
        webClient.baseUrl(baseUri).build().get()
                .uri("/oauth2/authorize")
                .exchange()

                // Then
                .expectStatus()
                .isOk();
    }

    @Test
    public void matlsAsAuthorizeRootRealmShouldForwardToAsApi() {
        String baseUri = "https://matls.as.aspsp.dev-ob.forgerock.financial:" + port;
        // Given
        asApiMock.stubFor(get(urlEqualTo("/oauth2/authorize"))
                .willReturn(aResponse()
                        .withStatus(200)));

        // When
        webClient.baseUrl(baseUri).build().get()
                .uri("/oauth2/realms/root/realms/openbanking/authorize")
                .exchange()

                // Then
                .expectStatus()
                .isOk();
    }

    @Test
    public void matlsAsAuthorizeForJwkmsShouldNotForwardToAsApi() {
        // Given
        String baseUri = "https://jwkms.dev-ob.forgerock.financial:" + port;
        asApiMock.stubFor(get(urlEqualTo("/oauth2/authorize"))
                .willReturn(aResponse()
                        .withStatus(200)));

        // When
        FluxExchangeResult<String> result = webClient.baseUrl(baseUri).build().get()
                .uri("/oauth2/realms/root/realms/openbanking/authorize")
                .exchange()

                // Then
                .returnResult(String.class);
        assertNotEquals(HttpStatus.OK, result.getStatus());
    }

    @Test
    public void matlsAsAuthorizeRootRealmForJwkmsShouldNotForwardToAsApi() {
        // Given
        String baseUri = "https://jwkms.dev-ob.forgerock.financial:" + port;
        asApiMock.stubFor(get(urlEqualTo("/oauth2/authorize"))
                .willReturn(aResponse()
                        .withStatus(200)));

        // When
        FluxExchangeResult<String> result = webClient.baseUrl(baseUri).build().get()
                .uri("/oauth2/authorize")
                .exchange()

                // Then
                .returnResult(String.class);
        assertNotEquals(HttpStatus.OK, result.getStatus());
    }

    @Test
    public void asAuthorizeShouldForwardToAsApi() {
        // Given
        String baseUri = "https://as.aspsp.dev-ob.forgerock.financial:" + port;
        asApiMock.stubFor(get(urlEqualTo("/oauth2/authorize"))
                .willReturn(aResponse()
                        .withStatus(200)));

        // When
        webClient.baseUrl(baseUri).build().get()
                .uri("/oauth2/authorize")
                .exchange()

                // Then
                .expectStatus()
                .isOk();
    }

    @Test
    public void asAuthorizeRootRealmShouldForwardToAsApi() {
        // Given
        String baseUri = "https://as.aspsp.dev-ob.forgerock.financial:" + port;
        asApiMock.stubFor(get(urlEqualTo("/oauth2/authorize"))
                .willReturn(aResponse()
                        .withStatus(200)));

        // When
        webClient.baseUrl(baseUri).build().get()
                .uri("/oauth2/realms/root/realms/openbanking/authorize")
                .exchange()

                // Then
                .expectStatus()
                .isOk();
    }

    @Test
    public void allowRegisterForWhitelistedIP() {
        // Given
        String baseUri = "https://matls.as.aspsp.dev-ob.forgerock.financial:" + port;
        asApiMock.stubFor(post(urlEqualTo("/open-banking/register"))
                .willReturn(aResponse()
                        .withStatus(200)));

        // When
        webClient.baseUrl(baseUri).build().post()
                .uri("/open-banking/register")
                .body(BodyInserters.fromObject(""))
                .header("X-Forwarded-For", "127.0.0.1")
                .exchange()

                // Then
                .expectStatus()
                .isOk();
    }

}
