package com.apiDataProcessor.producer.service.api;

import com.apiDataProcessor.models.apiResponse.reddit.RedditApiResponse;
import com.apiDataProcessor.producer.service.ApiDataHandlerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Base64Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.Map;

import static com.apiDataProcessor.utils.utils.isEmpty;

@Slf4j
@Service
public class RedditService extends ApiService {
    private final String STATE = "checkIt";
//    private final String STATE = hashString(UUID.randomUUID().toString()); // randomizing state string

    @Value(value =  "${reddit.api.clientId}")
    private String clientId;
    @Value(value = "${reddit.api.clientSecret}")
    private String clientSecret;
    @Value(value = "${reddit.api.redirectUri}")
    private String redirectUri;

    private final String accessTokenUri = "https://www.reddit.com/api/v1/access_token";
    private final String dataUri = "https://oauth.reddit.com/new";

    private String refreshToken = null;
    private String accessToken = null;
    private Timestamp accessTokenExpiryTimeStamp;

    private final ApiDataHandlerService apiDataHandlerService;

    public RedditService(ApiDataHandlerService apiDataHandlerService) {
        this.apiDataHandlerService = apiDataHandlerService;
    }

    @Override
    public void fetchData() {

        if (!isExecutable()) {
            log.warn("Reddit service not yet Authenticated. Please provide Client ID, Client Secret and Redirect URI.");
            return;
        }
        else if (!isAuthorized()) {
            log.warn("Reddit service not yet Authenticated. Please authenticate via: {}", getAuthUrl());
            return;
        }
        else if (tokenExpired()) {
            try {
                this.accessToken = getAccessToken();
            } catch (IOException | InterruptedException eX) {
                log.error("Error occurred while fetching access token: {}", eX.getMessage());
                return;
            }
        }

        apiDataHandlerService.fetchData(
                this.dataUri,
                RedditApiResponse.class,
                httpHeaders -> {
                    httpHeaders.setBearerAuth(this.accessToken);
                }
        );
    }

    @Override
    public boolean isExecutable() {
        return !isEmpty(clientId) && !isEmpty(clientSecret) & !isEmpty(redirectUri);
    }

    @Override
    public boolean isAuthorized() {
        return !isEmpty(accessToken) && !isEmpty(refreshToken);
    }

    public boolean checkState(String state) {
        return state.equals(STATE);
    }

    public String getAuthUrl() {
        return "https://www.reddit.com/api/v1/authorize" +
                "?client_id=" + this.clientId +
                "&response_type=code" +
                "&state=" + this.STATE +
                "&redirect_uri=" + this.redirectUri +
                "&duration=permanent" +
                "&scope=read,identity";
    }

    @SuppressWarnings("unchecked")
    public String getAccessToken(String code) throws IOException, InterruptedException {
        String authenticationHeader = "Basic " + Base64Util.encode(clientId + ":" + clientSecret);
        String requestParam = "grant_type=authorization_code" + "&" + "code=" + code + "&" + "redirect_uri=" + this.redirectUri;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(accessTokenUri))
                .header("Authorization", authenticationHeader)
                .POST(HttpRequest.BodyPublishers.ofString(requestParam))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> responseBody = new ObjectMapper().readValue(response.body(), Map.class);

        if (responseBody == null || responseBody.getOrDefault("error", null) != null) {
            return null;
        }
        if (responseBody.getOrDefault("refresh_token", null) != null) {
            this.refreshToken = (String) responseBody.get("refresh_token");
        }
        this.accessToken = (String) responseBody.get("access_token");
        this.accessTokenExpiryTimeStamp = new Timestamp(System.currentTimeMillis() + ((Integer) responseBody.get("expires_in")) * 1000);
        return this.accessToken;
    }

    @SuppressWarnings("unchecked")
    public String getAccessToken() throws IOException, InterruptedException {
        if (this.refreshToken == null) {
            return null;
        }
        if (this.accessToken != null && !tokenExpired()) {
            return this.accessToken;
        }
        String authenticationHeader = "Basic " + Base64Util.encode(clientId + ":" + clientSecret);
        String requestParam = "grant_type=refresh_token" + "&" + "refresh_token=" + this.refreshToken;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(accessTokenUri))
                .header("Authorization", authenticationHeader)
                .POST(HttpRequest.BodyPublishers.ofString(requestParam))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> responseBody = new ObjectMapper().readValue(response.body(), Map.class);

        if (responseBody == null || responseBody.getOrDefault("error", null) != null) {
            return null;
        }
        this.accessToken = (String) responseBody.get("access_token");
        return this.accessToken;
    }

    public Map<String, String> getConfigs() {
        Map<String, String> configs = Maps.newHashMap();

        configs.put("clientId", this.clientId);
        configs.put("clientSecret", this.clientSecret);
        configs.put("redirectUri", this.redirectUri);
        configs.put("state", this.STATE);
        configs.put("refreshToken", this.refreshToken);
        configs.put("accessToken", this.accessToken);
        configs.put("accessTokenExpiryTimeStamp", this.accessTokenExpiryTimeStamp.toString());

        return configs;
    }

    private boolean tokenExpired() {
        // null expiry timestamp or a timestamp before current-time means token is expired
        return this.accessTokenExpiryTimeStamp == null || this.accessTokenExpiryTimeStamp.before(new Timestamp(System.currentTimeMillis()));
    }
}
