package com.qrfileshare.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class AppConfig {
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public RestTemplate restTemplate() throws Exception {
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(TrustAllStrategy.INSTANCE)  // Trust all certs
                .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(
                                        SSLConnectionSocketFactoryBuilder.create()
                                                .setSslContext(sslContext)
                                                .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                                .build()
                                )
                                .build()
                )
                .build();
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }
}
