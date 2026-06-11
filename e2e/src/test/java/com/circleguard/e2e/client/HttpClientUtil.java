package com.circleguard.e2e.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.*;

import java.io.IOException;

public class HttpClientUtil {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static JsonNode get(String url, String bearerToken) {
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + bearerToken)
                .get()
                .build();
        return execute(request);
    }

    public static JsonNode post(String url, Object body, String bearerToken) {
        try {
            String json = mapper.writeValueAsString(body);
            RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));
            Request.Builder builder = new Request.Builder().url(url).post(requestBody);
            if (bearerToken != null && !bearerToken.isEmpty()) {
                builder.header("Authorization", "Bearer " + bearerToken);
            }
            return execute(builder.build());
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }

    public static JsonNode postMultipart(String url, String fileParamName, byte[] fileBytes, String fileName,
                                          String bearerToken) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(fileParamName, fileName,
                        RequestBody.create(fileBytes, MediaType.parse("application/octet-stream")))
                .build();
        Request.Builder builder = new Request.Builder().url(url).post(requestBody);
        if (bearerToken != null && !bearerToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return execute(builder.build());
    }

    private static JsonNode execute(Request request) {
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            JsonNode node = mapper.readTree(responseBody);
            if (node instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("_status", response.code());
            }
            return node;
        } catch (IOException e) {
            throw new RuntimeException("HTTP request failed: " + request.method() + " " + request.url(), e);
        }
    }

    public static int getStatus(JsonNode response) {
        return response.has("_status") ? response.get("_status").asInt() : -1;
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }
}
