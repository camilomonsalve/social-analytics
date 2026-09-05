package com.socialanalytics.synchronization.csv;

import com.socialanalytics.configuration.SyncProperties;
import com.socialanalytics.synchronization.exception.SyncException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvDownloaderTest {

    @Mock
    private RestClient syncRestClient;

    @Mock
    private SyncProperties syncProperties;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private CsvDownloader csvDownloader;

    @BeforeEach
    void setUp() {
        SyncProperties.Csv csvProps = new SyncProperties.Csv(
                "https://example.com/datos.csv",
                5000,
                15000
        );
        when(syncProperties.csv()).thenReturn(csvProps);
    }

    @Test
    void downloadReturnsInputStreamWhenSuccessful() {
        byte[] csvData = "nombre,categoria,descripcion\nTest,artistas,Test desc".getBytes();

        when(syncRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(byte[].class)).thenReturn(csvData);

        InputStream result = csvDownloader.download();

        assertNotNull(result);
        assertTrue(result instanceof ByteArrayInputStream);
    }

    @Test
    void downloadThrowsSyncExceptionWhenEmptyResponse() {
        when(syncRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(byte[].class)).thenReturn(null);

        assertThrows(SyncException.class, () -> csvDownloader.download());
    }

    @Test
    void downloadThrowsSyncExceptionWhenNullResponse() {
        when(syncRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(byte[].class)).thenReturn(null);

        assertThrows(SyncException.class, () -> csvDownloader.download());
    }

    @Test
    void downloadThrowsSyncExceptionOnRestClientError() {
        when(syncRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("Connection failed"));

        SyncException exception = assertThrows(SyncException.class, () -> csvDownloader.download());
        assertTrue(exception.getMessage().contains("Failed to download CSV"));
    }
}