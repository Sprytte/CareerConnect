package com.example.careercontactapi.security.service;

import com.example.careercontactapi.security.usermodels.EmployeeResponseModel;
import com.example.careercontactapi.security.usermodels.UserInfoResponseModel;
import com.example.careercontactapi.security.usermodels.EmployeeRequestModel;
import com.example.careercontactapi.UserManagement.presentationlayer.UserRequestModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Generated
public class Auth0ManagementService {
    //TODO These are environment variables. They need to be added in /resources/application.properties
    @Value("${okta.oauth2.issuer}")
    private String issuer;

    @Value("${okta.oauth2.client-id}")
    private String clientId;

    @Value("${okta.oauth2.client-secret}")
    private String clientSecret;

    private static String getFormattedBody(EmployeeRequestModel userRequest) {
        String email = userRequest.getEmail();
        String fName = userRequest.getFirstName();
        String lName = userRequest.getLastName();
        String password = userRequest.getPassword();
        String fullName = fName +  " " + lName;

        return """
            {
                "connection": "Username-Password-Authentication",
                "email": "%s",
                "given_name": "%s",
                "family_name": "%s",
                "name": "%s",
                "picture": "https://static-00.iconduck.com/assets.00/person-icon-476x512-hr6biidg.png",
                "password": "%s",
                "app_metadata": {}
            }
        """.formatted(email, fName, lName, fullName, password);
    }

    private static String getPatchFormatterBody(EmployeeRequestModel employeeRequestModel){
        String email = employeeRequestModel.getEmail();
        String fName = employeeRequestModel.getFirstName();
        String lName = employeeRequestModel.getLastName();
        String password = employeeRequestModel.getPassword();
        String fullName = fName +  " " + lName;

        if(email != "") {
            log.info("Changing email and name");
            return """
                {
                    "email": "%s",
                    "name": "%s"
                }
            """.formatted(email, fullName);
        }
        else if(email == ""){
            log.info("Changing name");
            return """
                {
                    "name": "%s"
                }
            """.formatted(email, fullName);
        }
        else{
            log.info("Changing password");
            return """
                {
                    "password": "%s"
                }
            """.formatted(email, password);
        }
    }

    public UserInfoResponseModel addReviewEmployee(EmployeeRequestModel employeeRequestModel) throws IOException, InterruptedException {
        String accessToken = getAccessToken();

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        //Creating user
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), getFormattedBody(employeeRequestModel));
        Request request = new Request.Builder()
                .url("https://canadawidecarparts.us.auth0.com/api/v2/users")
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try {
            Response response = client.newCall(request).execute();

            //Get response body of newly created employee
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.body().byteStream());

            //Assign role
            String id = jsonNode.path("user_id").asText().replace("|", "%7C");
            body = RequestBody.create(MediaType.parse("application/json"), "{\"roles\":[\"rol_8Aq3QUKB4NIzp6YY\"]}");
            request = new Request.Builder()
                    .url(issuer + "api/v2/users/" + id + "/roles")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            response = client.newCall(request).execute();
            log.info(response.toString());
            response.close();

            return UserInfoResponseModel.builder()
                    .email(jsonNode.path("email").asText())
                    .name(jsonNode.path("name").asText())
                    .userId(jsonNode.path("user_id").asText())
                    .picture(jsonNode.path("picture").asText())
                    .user_metadata(getUserMetadata(jsonNode))
                    .build();
        } catch( Exception e){
            log.info("To no one's surprise, it died");
            new Exception(e.getMessage());
            return null;
        }
    }

    public UserInfoResponseModel updateReviewEmployee(EmployeeRequestModel employeeRequestModel, String userId) throws IOException, InterruptedException {
        String accessToken = getAccessToken();

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        log.info("About to patch employee");
        //Updating user
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), getPatchFormatterBody(employeeRequestModel));
        //String id = jsonNode.path("user_id").asText().replace("|", "%7C");
        Request request = new Request.Builder()
                .url("https://canadawidecarparts.us.auth0.com/api/v2/users/" + userId)
                .method("PATCH", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try {
            Response response = client.newCall(request).execute();

            //Get response body of newly created employee
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.body().byteStream());

            response.close();

            return UserInfoResponseModel.builder()
                    .email(jsonNode.path("email").asText())
                    .name(jsonNode.path("name").asText())
                    .userId(jsonNode.path("user_id").asText())
                    .picture(jsonNode.path("picture").asText())
                    .user_metadata(getUserMetadata(jsonNode))
                    .build();
        } catch( Exception e){
            log.info("To no one's surprise, it died");
            new Exception(e.getMessage());
            return null;
        }
    }

    public List<EmployeeResponseModel> getReviewEmployees() throws IOException, InterruptedException {
        String accessToken = getAccessToken();
        List<EmployeeResponseModel> employees = new ArrayList<>();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(issuer + "api/v2/roles/rol_8Aq3QUKB4NIzp6YY/users"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP error: " + response.statusCode());
            }

            ObjectMapper mapper = new ObjectMapper();
            employees = mapper.readValue(response.body(), new TypeReference<List<EmployeeResponseModel>>() {});
            return employees;
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public String getAccessToken() throws IOException, InterruptedException {
        log.info("Getting Access Token");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(issuer + "oauth/token"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"client_id\":\"" + clientId + "\",\"client_secret\":\"" + clientSecret + "\",\"audience\":\"" + issuer + "api/v2/\",\"grant_type\":\"client_credentials\"}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("Access Token: " + extractToken(response.body()));
        return extractToken(response.body());
    }


    public UserInfoResponseModel getUserInfo(String id) throws  IOException, InterruptedException {
        String accessToken = getAccessToken();

        id = id.replace("|", "%7C");


        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(issuer + "api/v2/users/" + id))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Response: " + response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP error: " + response.statusCode());
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.body());
            log.info("This is the json Node: " + jsonNode.toString());
            return UserInfoResponseModel.builder()
                    .email(jsonNode.path("email").asText())
                    .name(jsonNode.path("name").asText())
                    .userId(jsonNode.path("user_id").asText())
                    .picture(jsonNode.path("picture").asText())
                    .user_metadata(getUserMetadata(jsonNode))
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }

    }
    public void deleteUser(String userId) throws IOException, InterruptedException {
        String accessToken = getAccessToken();

        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder()
                .url("https://canadawidecarparts.us.auth0.com/api/v2/users/" + userId)
                .method("DELETE", null)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
        }
    }
    private String getPatchFormatterBodyUser(UserRequestModel userRequestModel) {
        String name = userRequestModel.getName();
        return """
        {
            "name": "%s"
        }
    """.formatted(name);
    }
    public UserInfoResponseModel updateUserInfo(UserRequestModel userRequestModel, String userId) throws IOException, InterruptedException {
        String accessToken = getAccessToken();

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), getPatchFormatterBodyUser(userRequestModel));
        //String id = jsonNode.path("user_id").asText().replace("|", "%7C");
        Request request = new Request.Builder()
                .url("https://canadawidecarparts.us.auth0.com/api/v2/users/" + userId)
                .method("PATCH", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try {
            Response response = client.newCall(request).execute();

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.body().byteStream());
            log.info("This is the json Node: " + jsonNode.toString());

            response.close();

            return UserInfoResponseModel.builder()
                    .email(jsonNode.path("email").asText())
                    .name(jsonNode.path("name").asText())
                    .userId(jsonNode.path("user_id").asText())
                    .picture(jsonNode.path("picture").asText())
                    .user_metadata(getUserMetadata(jsonNode))
                    .build();
        } catch( Exception e){
            new Exception(e.getMessage());
            return null;
        }
    }


    private HashMap<String, String> getUserMetadata(JsonNode jsonNode) {
        HashMap<String, String> userMetadata = new HashMap<>();
        JsonNode metadataNode = jsonNode.path("user_metadata");

        if (metadataNode.isObject()) {
            metadataNode.fields().forEachRemaining(entry -> userMetadata.put(entry.getKey(), entry.getValue().asText()));
        }

        return userMetadata;
    }

    private String extractToken(String response) {
        return response.split(",")[0].split(":")[1].replace("\"", "");
    }

    //make a method to get the roles (build request) and then maybe add them as a cookie? for convenience? debatable
}