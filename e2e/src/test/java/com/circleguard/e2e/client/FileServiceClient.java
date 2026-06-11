package com.circleguard.e2e.client;

import com.fasterxml.jackson.databind.JsonNode;

public class FileServiceClient {

    private final String baseUrl;

    public FileServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public JsonNode uploadFile(byte[] content, String fileName, String jwt) {
        return HttpClientUtil.postMultipart(baseUrl + "/api/v1/files/upload",
                "file", content, fileName, jwt);
    }
}
