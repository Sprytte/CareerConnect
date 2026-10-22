package com.example.careercontactapi.security;

import com.example.careercontactapi.security.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Auth0PasswordServiceTest {
    private HttpServer server;
    private Auth0PasswordService service;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> body = new AtomicReference<>("{\"user_id\":\"auth0|one\"}");
    private final AtomicReference<String> sent = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @BeforeEach void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v2/users/", exchange -> {
            sent.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        var management = mock(Auth0ManagementService.class);
        when(management.getAccessToken()).thenReturn("fixture-access-token");
        service = new Auth0PasswordService("http://127.0.0.1:" + server.getAddress().getPort(), management);
    }
    @AfterEach void stop() { server.stop(0); }
    @Test void realHttpRequestPreservesPasswordAndChecksAccountIdentity() throws Exception {
        String password = "Long\\Quoted\"Password!7";
        service.changePassword("auth0|one", password);
        assertEquals(password, new ObjectMapper().readTree(sent.get()).get("password").asText());
        assertEquals("Bearer fixture-access-token", authorization.get());
    }
    @Test void socialAccountsCannotSendPasswordChanges() {
        var error = assertThrows(ResponseStatusException.class, () -> service.changePassword("google-oauth2|one", "LongFixturePassword!7"));
        assertEquals(409, error.getStatusCode().value()); assertNull(sent.get());
    }
    @Test void shortPasswordsCannotReachAuth0() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.changePassword("auth0|one", "short")).getStatusCode().value());
        assertNull(sent.get());
    }
    @Test void upstreamPolicyFailureIsNotSuccess() {
        status.set(400);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.changePassword("auth0|one", "LongFixturePassword!7")).getStatusCode().value());
    }
    @Test void upstreamPermissionFailureIsNotExposedAsUserPermissionOrSuccess() {
        status.set(403); body.set("private upstream diagnostic");
        var error = assertThrows(ResponseStatusException.class, () -> service.changePassword("auth0|one", "LongFixturePassword!7"));
        assertEquals(502, error.getStatusCode().value()); assertFalse(error.getReason().contains("private"));
    }
    @Test void wrongAccountConfirmationIsRejected() {
        body.set("{\"user_id\":\"auth0|other\"}");
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> service.changePassword("auth0|one", "LongFixturePassword!7")).getStatusCode().value());
    }
}
