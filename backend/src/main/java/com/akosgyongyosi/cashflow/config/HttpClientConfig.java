package com.akosgyongyosi.cashflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Slf4j
@Configuration
public class HttpClientConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        try {
            // Create a trust manager that accepts all certificates (for development)
            // WARNING: This is insecure and should only be used in development
            // This is necessary because api.frankfurter.app certificate may not be trusted
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    // Empty implementation for development - accepts all client certificates
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // Intentionally empty for development SSL bypass
                    }
                    // Empty implementation for development - accepts all server certificates
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // Intentionally empty for development SSL bypass
                    }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            // Accept all hostnames for development - necessary for Frankfurter API
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(5000);
            requestFactory.setReadTimeout(10000);

            return builder
                    .requestFactory(() -> requestFactory)
                    .build();
        } catch (Exception e) {
            log.error("Failed to configure SSL, falling back to default RestTemplate", e);
            return builder.build();
        }
    }
}
