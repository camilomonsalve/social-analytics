package com.socialanalytics.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient syncRestClient(SyncProperties syncProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(syncProperties.csv().connectTimeoutMs());
        factory.setReadTimeout(syncProperties.csv().readTimeoutMs());
        return RestClient.builder().requestFactory(factory).build();
    }
}
