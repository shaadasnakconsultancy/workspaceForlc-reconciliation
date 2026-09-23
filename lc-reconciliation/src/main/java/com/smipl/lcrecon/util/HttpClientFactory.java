package com.smipl.lcrecon.util;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Builds HTTP clients that always carry timeouts.
 *
 * {@code HttpClients.createDefault()} has NO connect or socket timeout: if a remote endpoint
 * accepts the TCP connection and then goes quiet, the calling thread blocks forever. In a job
 * worker that permanently consumes a thread from the pool, and the job can never be aborted
 * because a thread blocked in a socket read does not respond to interrupts. Every outbound call
 * must therefore be built here rather than with createDefault().
 */
public final class HttpClientFactory {

    /** TCP connect / connection-pool lease. A blocked port should fail fast. */
    public static final int CONNECT_TIMEOUT_MS = 15_000;

    /** Wait for response data on ordinary calls (auth, metadata, uploads). */
    public static final int DEFAULT_SOCKET_TIMEOUT_MS = 60_000;

    /**
     * Wait for response data on calls that are legitimately slow - Document Intelligence OCR and
     * GPT completions on a large document routinely take minutes. Generous, but still finite.
     */
    public static final int LONG_SOCKET_TIMEOUT_MS = 300_000;

    private HttpClientFactory() {}

    public static CloseableHttpClient create(int socketTimeoutMs) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT_MS)
                .setSocketTimeout(socketTimeoutMs)
                .build();
        return HttpClients.custom().setDefaultRequestConfig(config).build();
    }

    /** For short calls: token requests, metadata lookups, connection tests. */
    public static CloseableHttpClient createDefault() {
        return create(DEFAULT_SOCKET_TIMEOUT_MS);
    }

    /** For OCR and GPT calls, which are slow by nature. */
    public static CloseableHttpClient createLongRunning() {
        return create(LONG_SOCKET_TIMEOUT_MS);
    }
}
