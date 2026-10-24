package com.buctta.api.controller;

import com.buctta.api.service.TeacherAIService;
import com.buctta.api.config.SecurityAuthorize;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

class TeacherAICtrlTest {
    private HttpServer upstream;
    private MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<String> response = new AtomicReference<>();
    private final AtomicReference<String> payload = new AtomicReference<>();
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicInteger status = new AtomicInteger(200);
    private boolean failFirst;
    private boolean delay;
    private static final String INPUT = "{\"subject\":\"数学\",\"grade\":\"八年级\",\"topic\":\"一次函数\"}";

    @BeforeEach void setup() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.createContext("/v1/chat/completions", exchange -> {
            calls.incrementAndGet();
            assertEquals("Bearer test-key", exchange.getRequestHeaders().getFirst("Authorization"));
            payload.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = response.get().getBytes(StandardCharsets.UTF_8);
            if (delay) try { Thread.sleep(1500); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            exchange.sendResponseHeaders(failFirst && calls.get() == 1 ? 503 : status.get(), bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        upstream.start();
        mvc = MockMvcBuilders.standaloneSetup(new TeacherAICtrl(service("test-key"))).build();
    }
    private TeacherAIService service(String key) {
        return new TeacherAIService(mapper, key, "http://127.0.0.1:" + upstream.getAddress().getPort() + "/v1", "test-model", 1000);
    }
    @AfterEach void stop() { upstream.stop(0); org.springframework.security.core.context.SecurityContextHolder.clearContext(); }

    @Test void catalogAndHealthDoNotCallModel() throws Exception {
        mvc.perform(get("/api/teacher-ai/knowledge/subjects")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grades.length()").value(12))
                .andExpect(jsonPath("$.data.subjects[1].name").value("数学"));
        mvc.perform(get("/api/teacher-ai/health")).andExpect(jsonPath("$.hasApiKey").value(true));
        assertEquals(0, calls.get());
    }
    @Test void rejectsMissingAndInvalidFieldsBeforeCallingModel() throws Exception {
        for (String path : new String[]{"knowledge/generate", "lesson-plan/generate", "lesson-plan/generate-stream"}) {
            mvc.perform(post("/api/teacher-ai/" + path).contentType("application/json").content("{}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        }
        mvc.perform(post("/api/teacher-ai/lesson-plan/generate").contentType("application/json")
                .content(INPUT.replace("}", ",\"duration\":-1}"))).andExpect(status().isBadRequest());
        assertEquals(0, calls.get());
    }
    @Test void knowledgePreservesStructuredResultAndJsonMode() throws Exception {
        response.set("{\"choices\":[{\"message\":{\"content\":\"```json\\n{\\\"chapterTitle\\\":\\\"一次函数\\\"}\\n```\"}}]}");
        mvc.perform(post("/api/teacher-ai/knowledge/generate").contentType("application/json").content(INPUT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.chapterTitle").value("一次函数"));
        var body = mapper.readTree(payload.get());
        assertEquals("json_object", body.path("response_format").path("type").asText());
        assertEquals(4096, body.path("max_tokens").asInt());
        assertTrue(body.path("messages").path(0).path("content").asText().contains("数学"));
    }
    @Test void lessonPreservesOptions() throws Exception {
        response.set("{\"choices\":[{\"message\":{\"content\":\"{\\\"title\\\":\\\"教案\\\"}\"}}]}");
        mvc.perform(post("/api/teacher-ai/lesson-plan/generate").contentType("application/json")
                .content(INPUT.replace("}", ",\"duration\":40,\"teachingStyle\":\"探究式\"}")))
                .andExpect(jsonPath("$.data.title").value("教案"));
        var body = mapper.readTree(payload.get());
        assertEquals(8192, body.path("max_tokens").asInt());
        assertTrue(body.path("messages").path(1).path("content").asText().contains("40分钟"));
    }
    @Test void streamsChineseChunksAndOneDoneEvent() throws Exception {
        response.set("data: {\"choices\":[{\"delta\":{\"content\":\"教学目标\"}}]}\n\ndata: [DONE]\n\n");
        var result = mvc.perform(post("/api/teacher-ai/lesson-plan/generate-stream").contentType("application/json").content(INPUT))
                .andExpect(status().isOk()).andReturn();
        String text = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(text.contains("教学目标"));
        assertEquals(1, text.split("\"type\":\"done\"", -1).length - 1);
        assertTrue(mapper.readTree(payload.get()).path("stream").asBoolean());
    }
    @Test void truncatedStreamEmitsErrorInsteadOfDone() throws Exception {
        response.set("data: {\"choices\":[{\"delta\":{\"content\":\"部分内容\"}}]}\n\n");
        var result = mvc.perform(post("/api/teacher-ai/lesson-plan/generate-stream").contentType("application/json").content(INPUT)).andReturn();
        String text = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(text.contains("\"type\":\"error\""));
        assertFalse(text.contains("\"type\":\"done\""));
    }
    @Test void missingKeyAndUpstreamAuthErrorAreSafe() throws Exception {
        var noKey = MockMvcBuilders.standaloneSetup(new TeacherAICtrl(service(""))).build();
        noKey.perform(post("/api/teacher-ai/knowledge/generate").contentType("application/json").content(INPUT))
                .andExpect(status().isServiceUnavailable());
        assertEquals(0, calls.get());
        status.set(401); response.set("secret upstream detail");
        mvc.perform(post("/api/teacher-ai/knowledge/generate").contentType("application/json").content(INPUT))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.type").value("auth"));
        assertEquals(1, calls.get());
    }
    @Test void invalidModelJsonIsReported() throws Exception {
        response.set("{\"choices\":[{\"message\":{\"content\":\"not json\"}}]}");
        mvc.perform(post("/api/teacher-ai/knowledge/generate").contentType("application/json").content(INPUT))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.type").value("response"));
        assertEquals(1, calls.get());
    }

    @Test void temporaryFailureRetriesWithoutLosingRequest() throws Exception {
        failFirst = true;
        response.set("{\"choices\":[{\"message\":{\"content\":\"{\\\"title\\\":\\\"教案\\\"}\"}}]}");
        mvc.perform(post("/api/teacher-ai/lesson-plan/generate").contentType("application/json").content(INPUT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("教案"));
        assertEquals(2, calls.get());
    }
    @Test void timeoutReturnsGatewayTimeout() throws Exception {
        delay = true; response.set("{}");
        mvc.perform(post("/api/teacher-ai/knowledge/generate").contentType("application/json").content(INPUT))
                .andExpect(status().isGatewayTimeout());
        assertEquals(1, calls.get());
    }
    @Test void streamUpstreamFailureIsOneErrorEvent() throws Exception {
        status.set(401); response.set("private supplier detail");
        String body = mvc.perform(post("/api/teacher-ai/lesson-plan/generate-stream").contentType("application/json").content(INPUT))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("\"type\":\"error\""));
        assertFalse(body.contains("private supplier detail"));
        assertFalse(body.contains("\"type\":\"done\""));
        assertEquals(1, calls.get());
    }
    @Configuration @org.springframework.web.servlet.config.annotation.EnableWebMvc @Import(SecurityAuthorize.class)
    static class SecurityConfig {
        @Bean TeacherAIService teacherAIService() { return new TeacherAIService(new ObjectMapper(), "", "https://example.invalid", "test", 1000); }
        @Bean TeacherAICtrl teacherAICtrl(TeacherAIService service) { return new TeacherAICtrl(service); }
    }
    @Test void javaSessionProtectsTeacherAIEndpoints() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(SecurityConfig.class); context.refresh();
            var secured = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            secured.perform(get("/api/teacher-ai/health").servletPath("/api/teacher-ai/health"))
                    .andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/json"));
            secured.perform(get("/api/teacher-ai/health").with(request -> {
                request.setRequestedSessionId("expired"); request.setRequestedSessionIdValid(false); return request;
            })).andExpect(status().isUnauthorized());
            secured.perform(get("/api/teacher-ai/health").with(user("teacher"))).andExpect(status().isOk());
        }
    }
}
