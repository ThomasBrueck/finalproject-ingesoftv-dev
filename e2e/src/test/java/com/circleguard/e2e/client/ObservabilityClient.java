package com.circleguard.e2e.client;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class ObservabilityClient {

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    /** GET {baseUrl}/actuator/prometheus — returns raw Prometheus text format. */
    public String fetchActuatorPrometheus(String baseUrl) {
        String url = baseUrl.replaceAll("/$", "") + "/actuator/prometheus";
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = http.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new RuntimeException("GET " + url + " returned HTTP " + response.code());
            }
            return body;
        } catch (IOException e) {
            throw new RuntimeException("Cannot reach " + url + ": " + e.getMessage(), e);
        }
    }

    /** Returns true if GET {baseUrl}/actuator/health returns HTTP 200. */
    public boolean isHealthy(String baseUrl) {
        String url = baseUrl.replaceAll("/$", "") + "/actuator/health";
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = http.newCall(request).execute()) {
            return response.code() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    /** Executes a PromQL instant query against the Prometheus HTTP API. Returns raw JSON response. */
    public String queryPrometheus(String prometheusUrl, String promql) {
        String encoded = URLEncoder.encode(promql, StandardCharsets.UTF_8);
        String url = prometheusUrl.replaceAll("/$", "") + "/api/v1/query?query=" + encoded;
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = http.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                throw new RuntimeException("Prometheus query failed HTTP " + response.code() + ": " + body);
            }
            return body;
        } catch (IOException e) {
            throw new RuntimeException("Cannot reach Prometheus at " + url + ": " + e.getMessage(), e);
        }
    }
}
