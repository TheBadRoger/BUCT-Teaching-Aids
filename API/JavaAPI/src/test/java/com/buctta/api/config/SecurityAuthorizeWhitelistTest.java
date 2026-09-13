package com.buctta.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 校验 {@link SecurityAuthorize} 的白名单规则。
 * <p>
 * 覆盖两类回归：
 * <ul>
 *   <li>AI 背诵手册页面（/recitation/**）必须以 200 返回，否则功能不可达；</li>
 *   <li>其页面内调用的 /api/handbook/** 以及未列入白名单的页面必须要求登录。</li>
 * </ul>
 * 只组装 Security 过滤链，不启动完整 Spring 上下文（无需数据库 / Redis）。
 */
@SpringJUnitWebConfig(classes = SecurityAuthorizeWhitelistTest.TestWebConfig.class)
class SecurityAuthorizeWhitelistTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private MockMvc mockMvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(context)
                    .apply(springSecurity())
                    .build();
        }
        return mockMvc;
    }

    @Test
    void recitationIndex_isPublic() throws Exception {
        int status = mockMvc().perform(get("/recitation/index.html"))
                .andReturn().getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status,
                "/recitation/index.html 应为 200（白名单），实际被拦截");
    }

    @Test
    void recitationAssets_arePublic() throws Exception {
        int status = mockMvc().perform(get("/recitation/css/main.css"))
                .andReturn().getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status,
                "/recitation/css/main.css 应为 200（白名单），实际被拦截");
    }

    @Test
    void handbookApi_stillRequiresAuthentication() throws Exception {
        int status = mockMvc().perform(post("/api/handbook/generate"))
                .andReturn().getResponse().getStatus();
        assertEquals(HttpStatus.FOUND.value(), status,
                "/api/handbook/generate 未登录时应重定向到登录页（302）");
        assertEquals("/enter.html",
                mockMvc().perform(post("/api/handbook/generate"))
                        .andReturn().getResponse().getRedirectedUrl(),
                "/api/handbook/generate 未登录时应重定向到 /enter.html");
    }

    @Test
    void unlistedPage_stillRequiresAuthentication() throws Exception {
        int status = mockMvc().perform(get("/admin-enter.html"))
                .andReturn().getResponse().getStatus();
        assertEquals(HttpStatus.FOUND.value(), status,
                "/admin-enter.html 未在白名单内，未登录时应重定向到登录页");
    }

    @TestConfiguration
    @Import(SecurityAuthorize.class)
    static class TestWebConfig {

        @Bean
        StubEndpoints stubEndpoints() {
            return new StubEndpoints();
        }
    }

    @RestController
    static class StubEndpoints {

        @GetMapping("/recitation/index.html")
        String recitationIndex() {
            return "<html>recitation</html>";
        }

        @GetMapping("/recitation/css/main.css")
        String recitationCss() {
            return "body{}";
        }

        @GetMapping("/admin-enter.html")
        String adminEnter() {
            return "<html>admin</html>";
        }

        @PostMapping("/api/handbook/generate")
        String generateHandbook() {
            return "{}";
        }
    }
}
