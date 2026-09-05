package com.socialanalytics.synchronization.csv;

import com.socialanalytics.configuration.SyncProperties;
import com.socialanalytics.synchronization.exception.SyncException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class CsvDownloader {

    private final RestClient syncRestClient;
    private final SyncProperties syncProperties;

    public InputStream download() {
        try {
            byte[] body = syncRestClient.get()
                    .uri(syncProperties.csv().sourceUrl())
                    .retrieve()
                    .body(byte[].class);
            if (body == null || body.length == 0) {
                throw new SyncException("Downloaded CSV is empty", null);
            }
            return new ByteArrayInputStream(body);
        } catch (RestClientException e) {
            throw new SyncException("Failed to download CSV from " + syncProperties.csv().sourceUrl(), e);
        }
    }
}
