package com.example.careercontactapi.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Service
public class Auth0PasswordService {
    private final String issuer;
    private final Auth0ManagementService management;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public Auth0PasswordService(@Value("${okta.oauth2.issuer}") String issuer, Auth0ManagementService management) {
        this.issuer = issuer.endsWith("/") ? issuer : issuer + "/";
        this.management = management;
    }

    public void changePassword(String userId, String password) {
        if (userId == null || !userId.startsWith("auth0|")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Change your password through your sign-in provider");
        }
        if (password == null || password.length() < 15) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use at least 15 characters");
        }
        try {
            String token = management.getAccessToken();
            // Serialize JSON so quotes, backslashes, and Unicode passwords are preserved.
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(issuer + "api/v2/users/" + URLEncoder.encode(userId, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("password", password))))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 400) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password does not meet the sign-in provider's policy");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The password service could not complete the change");
            }
            var saved = mapper.readTree(response.body());
            if (saved == null || !userId.equals(saved.path("user_id").asText())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The password service returned an invalid confirmation");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The password service is unavailable");
        } catch (IOException | IllegalArgumentException e) {
            // Never expose the password, token, or upstream response body.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The password service is unavailable");
        }
    }
}
