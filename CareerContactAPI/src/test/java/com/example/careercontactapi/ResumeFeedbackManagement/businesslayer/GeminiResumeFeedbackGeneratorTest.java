package com.example.careercontactapi.ResumeFeedbackManagement.businesslayer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uses a local stand-in for the provider, so no key or network access is needed. */
class GeminiResumeFeedbackGeneratorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ResumeDocument PDF =
            ResumeDocument.ofPdf("resume.pdf", "%PDF-1.7 fixture".getBytes(StandardCharsets.US_ASCII));

    private HttpServer provider;
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> seenPath = new AtomicReference<>();
    private final AtomicReference<String> seenKey = new AtomicReference<>();
    private final AtomicReference<String> seenBody = new AtomicReference<>();

    @AfterEach
    void stopProvider() {
        if (provider != null) {
            provider.stop(0);
        }
    }

    private GeminiResumeFeedbackGenerator generatorAnswering(int status, String body) throws IOException {
        provider = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        provider.createContext("/", exchange -> {
            calls.incrementAndGet();
            seenPath.set(exchange.getRequestURI().toString());
            seenKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            seenBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        provider.start();
        String baseUrl = "http://127.0.0.1:" + provider.getAddress().getPort() + "/v1beta/";
        return new GeminiResumeFeedbackGenerator("test-key", "test-model", baseUrl, Duration.ofSeconds(5));
    }

    private static String providerReply(String text) throws IOException {
        var root = MAPPER.createObjectNode();
        root.putArray("candidates").addObject().putObject("content").putArray("parts").addObject().put("text", text);
        return MAPPER.writeValueAsString(root);
    }

    private int statusOf(Runnable call) {
        return assertThrows(ResponseStatusException.class, call::run).getStatusCode().value();
    }

    @Test
    void sendsThePdfInlineWithTheKeyInAHeaderAndReturnsTheParsedFeedback() throws IOException {
        var generator = generatorAnswering(200, providerReply(
                "{\"summary\":\"Clear and focused.\",\"strengths\":[\"Good projects\"],\"improvements\":[\"Add metrics\",\"Shorten summary\"]}"));

        var feedback = generator.generate(PDF);

        assertEquals("resume.pdf", feedback.fileName());
        assertEquals("Clear and focused.", feedback.summary());
        assertEquals(List.of("Good projects"), feedback.strengths());
        assertEquals(List.of("Add metrics", "Shorten summary"), feedback.improvements());

        assertEquals("/v1beta/models/test-model:generateContent", seenPath.get());
        assertEquals("test-key", seenKey.get());
        JsonNode parts = MAPPER.readTree(seenBody.get()).path("contents").path(0).path("parts");
        assertEquals("application/pdf", parts.path(0).path("inline_data").path("mime_type").asText());
        assertArrayEquals(PDF.pdf(), Base64.getDecoder().decode(parts.path(0).path("inline_data").path("data").asText()));
        assertTrue(parts.path(1).path("text").asText().contains("\"improvements\""));
        assertFalse(seenBody.get().contains("test-key"));
    }

    @Test
    void sendsDocxTextAsPlainText() throws IOException {
        var generator = generatorAnswering(200, providerReply("{\"summary\":\"Fine.\",\"strengths\":[],\"improvements\":[]}"));

        generator.generate(ResumeDocument.ofText("resume.docx", "Frontend developer, Montreal"));

        JsonNode parts = MAPPER.readTree(seenBody.get()).path("contents").path(0).path("parts");
        assertEquals(1, parts.size());
        assertTrue(parts.path(0).path("text").asText().contains("Frontend developer, Montreal"));
        assertFalse(parts.path(0).has("inline_data"));
    }

    @Test
    void acceptsAReplyWrappedInACodeFence() {
        var feedback = GeminiResumeFeedbackGenerator.parseFeedback(
                replyOrFail("```json\n{\"summary\":\"Solid.\",\"strengths\":[\"A\",\"  \",7],\"improvements\":[\"B\"]}\n```"),
                "cv.pdf");

        assertEquals("Solid.", feedback.summary());
        assertEquals(List.of("A"), feedback.strengths());
        assertEquals(List.of("B"), feedback.improvements());
    }

    @Test
    void returnsPlainProseAsTheSummaryWhenTheReplyIsNotJson() {
        var feedback = GeminiResumeFeedbackGenerator.parseFeedback(
                replyOrFail("Your resume is clear, but add measurable results."), "cv.pdf");

        assertEquals("Your resume is clear, but add measurable results.", feedback.summary());
        assertTrue(feedback.strengths().isEmpty());
        assertTrue(feedback.improvements().isEmpty());
    }

    @Test
    void limitsHowMuchOfTheReplyIsReturned() {
        String longItem = "\"" + "x".repeat(900) + "\"";
        String manyItems = "\"item\",".repeat(20) + "\"end\"";
        var feedback = GeminiResumeFeedbackGenerator.parseFeedback(
                replyOrFail("{\"summary\":\"" + "s".repeat(4000) + "\",\"strengths\":[" + longItem
                        + "],\"improvements\":[" + manyItems + "]}"), "cv.pdf");

        assertEquals(1500, feedback.summary().length());
        assertEquals(500, feedback.strengths().get(0).length());
        assertEquals(8, feedback.improvements().size());
    }

    @Test
    void anEmptyOrBlockedReplyIsAGatewayError() {
        assertEquals(502, statusOf(() -> GeminiResumeFeedbackGenerator.parseFeedback("{\"candidates\":[]}", "cv.pdf")));
        assertEquals(502, statusOf(() -> GeminiResumeFeedbackGenerator.parseFeedback(
                "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}", "cv.pdf")));
        assertEquals(502, statusOf(() -> GeminiResumeFeedbackGenerator.parseFeedback("not json", "cv.pdf")));
        assertEquals(502, statusOf(() -> GeminiResumeFeedbackGenerator.parseFeedback("", "cv.pdf")));
    }

    private int statusWhenProviderAnswers(int status, String body) throws IOException {
        var generator = generatorAnswering(status, body);
        try {
            return statusOf(() -> generator.generate(PDF));
        } finally {
            provider.stop(0);
            provider = null;
        }
    }

    @Test
    void mapsProviderFailuresToTheDocumentedStatuses() throws IOException {
        assertEquals(429, statusWhenProviderAnswers(429, "{}"));
        assertEquals(502, statusWhenProviderAnswers(500, "{}"));
        assertEquals(503, statusWhenProviderAnswers(403, "{}"));
        assertEquals(503, statusWhenProviderAnswers(400,
                "{\"error\":{\"details\":[{\"reason\":\"API_KEY_INVALID\"}]}}"));
        assertEquals(502, statusWhenProviderAnswers(400, "{\"error\":{\"message\":\"bad request\"}}"));
        assertEquals(502, statusWhenProviderAnswers(200, "{\"candidates\":[]}"));
    }

    @Test
    void withoutAKeyTheProviderIsNeverCalled() throws IOException {
        generatorAnswering(200, providerReply("{}"));
        String baseUrl = "http://127.0.0.1:" + provider.getAddress().getPort() + "/v1beta";

        var noKey = new GeminiResumeFeedbackGenerator(" ", "test-model", baseUrl, Duration.ofSeconds(5));
        var badModel = new GeminiResumeFeedbackGenerator("test-key", "models/../x", baseUrl, Duration.ofSeconds(5));

        assertEquals(503, statusOf(() -> noKey.generate(PDF)));
        assertEquals(503, statusOf(() -> badModel.generate(PDF)));
        assertEquals(0, calls.get());
    }

    @Test
    void anUnreachableProviderIsAGatewayError() throws IOException {
        generatorAnswering(200, "{}");
        int port = provider.getAddress().getPort();
        provider.stop(0);
        provider = null;

        var generator = new GeminiResumeFeedbackGenerator(
                "test-key", "test-model", "http://127.0.0.1:" + port + "/v1beta", Duration.ofSeconds(5));

        assertEquals(502, statusOf(() -> generator.generate(PDF)));
    }

    private static String replyOrFail(String text) {
        try {
            return providerReply(text);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
