package com.campus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端 happy-path 集成测试：注册 → 充值 → 发布 → 检索 → 下单 →
 * 发货 → 确认 → 评价。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HappyPathTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper json;

    private String baseUrl() { return "http://localhost:" + port; }

    private HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.setBearerAuth(token);
        return h;
    }

    private long jsonLong(JsonNode n, String field) { return n.path(field).asLong(); }
    private String jsonStr(JsonNode n, String field) { return n.path(field).asText(); }

    @Test
    void happyPath_register_to_review() throws Exception {
        // 0) 拿到 Demo 学校 ID
        ResponseEntity<String> schoolsResp = rest.getForEntity(baseUrl() + "/api/v1/schools", String.class);
        assertThat(schoolsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.err.println("[DBG] /schools body: " + schoolsResp.getBody());
        JsonNode schools = json.readTree(schoolsResp.getBody()).get("data");
        assertThat(schools).as("data is not null").isNotNull();
        assertThat(schools.isArray()).as("data is array").isTrue();
        assertThat(schools.size()).as("schools seeded").isGreaterThan(0);
        long schoolId = jsonLong(schools.get(0), "id");
        assertThat(jsonStr(schools.get(0), "domain")).isEqualTo("demo.edu");

        // 1) 注册买家 (A) — 注：邮箱必须含字母数字
        String aEmail = "alice" + System.currentTimeMillis() + "@demo.edu";
        String registerBody = json.writeValueAsString(Map.of(
            "schoolId", schoolId, "email", aEmail,
            "password", "alice12345", "nickname", "Alice"));
        ResponseEntity<String> regA = rest.postForEntity(baseUrl() + "/api/v1/auth/register",
            new HttpEntity<>(registerBody, bearer(null)), String.class);
        assertThat(regA.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode regAData = json.readTree(regA.getBody()).get("data");
        long aUserId = jsonLong(regAData, "userId");

        // 2) 登录拿 token
        String loginBody = json.writeValueAsString(Map.of("email", aEmail, "password", "alice12345"));
        ResponseEntity<String> loginA = rest.postForEntity(baseUrl() + "/api/v1/auth/login",
            new HttpEntity<>(loginBody, bearer(null)), String.class);
        assertThat(loginA.getStatusCode()).isEqualTo(HttpStatus.OK);
        String tokenA = json.readTree(loginA.getBody()).get("data").get("token").asText();

        // 3) 注册卖家 (B)
        String bEmail = "bob" + System.currentTimeMillis() + "@demo.edu";
        String registerB = json.writeValueAsString(Map.of(
            "schoolId", schoolId, "email", bEmail,
            "password", "bobpass1", "nickname", "Bob"));
        rest.postForEntity(baseUrl() + "/api/v1/auth/register",
            new HttpEntity<>(registerB, bearer(null)), String.class);
        ResponseEntity<String> loginB = rest.postForEntity(baseUrl() + "/api/v1/auth/login",
            new HttpEntity<>(json.writeValueAsString(Map.of("email", bEmail, "password", "bobpass1")), bearer(null)),
            String.class);
        String tokenB = json.readTree(loginB.getBody()).get("data").get("token").asText();

        // 4) 买家 A 充值 5000 cents
        rest.exchange(baseUrl() + "/api/v1/wallet/top-up", HttpMethod.POST,
            new HttpEntity<>(json.writeValueAsString(Map.of("amountCents", 5000)), bearer(tokenA)), String.class);

        // 5) 拿一个分类 ID
        ResponseEntity<String> catsResp = rest.getForEntity(baseUrl() + "/api/v1/categories", String.class);
        System.err.println("[DBG] /categories body: " + catsResp.getBody());
        JsonNode cats = json.readTree(catsResp.getBody()).get("data");
        assertThat(cats).as("cats data not null").isNotNull();
        assertThat(cats.isArray()).as("cats is array").isTrue();
        assertThat(cats.size()).as("cats seeded").isGreaterThan(0);
        long categoryId = jsonLong(cats.get(0), "id");

        // 6) 卖家 B 发布商品
        String listingBody = json.writeValueAsString(Map.of(
            "categoryId", categoryId,
            "title", "calculus-textbook-9-used",
            "description", "used for graduate exam 2026",
            "priceCents", 4500,
            "condition", "LIKE_NEW",
            "coverImageUrl", "https://example.com/cover.jpg"));
        ResponseEntity<String> listingResp = rest.postForEntity(baseUrl() + "/api/v1/listings",
            new HttpEntity<>(listingBody, bearer(tokenB)), String.class);
        System.err.println("[DBG] /listings POST body: " + listingResp.getBody());
        assertThat(listingResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        long listingId = jsonLong(json.readTree(listingResp.getBody()).get("data"), "id");

        // 7) 买家 A 搜索「高数」
        ResponseEntity<String> searchResp = rest.exchange(baseUrl() + "/api/v1/listings?keyword=calculus&page=1&size=20",
            HttpMethod.GET, new HttpEntity<>(bearer(tokenA)), String.class);
        System.err.println("[DBG] /listings GET body: " + searchResp.getBody());
        ResponseEntity<String> allResp = rest.exchange(baseUrl() + "/api/v1/listings?page=1&size=20",
            HttpMethod.GET, new HttpEntity<>(bearer(tokenA)), String.class);
        System.err.println("[DBG] /listings GET ALL body: " + allResp.getBody());
        assertThat(searchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode searchBody = json.readTree(searchResp.getBody()).get("data").get("content");
        assertThat(searchBody.size()).as("search results").isGreaterThan(0);
        long foundId = searchBody.get(0).get("id").asLong();
        assertThat(foundId).isEqualTo(listingId);

        // 8) 买家 A 下单
        String orderBody = json.writeValueAsString(Map.of("listingId", listingId));
        ResponseEntity<String> orderResp = rest.postForEntity(baseUrl() + "/api/v1/orders",
            new HttpEntity<>(orderBody, bearer(tokenA)), String.class);
        assertThat(orderResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        long orderId = jsonLong(json.readTree(orderResp.getBody()).get("data"), "id");
        String orderStatus = jsonStr(json.readTree(orderResp.getBody()).get("data"), "status");
        assertThat(orderStatus).isEqualTo("PAID_ESCROW");

        // 9) 卖家 B 发货
        rest.exchange(baseUrl() + "/api/v1/orders/" + orderId + "/ship", HttpMethod.POST,
            new HttpEntity<>("", bearer(tokenB)), String.class);

        // 10) 买家 A 确认收货
        rest.exchange(baseUrl() + "/api/v1/orders/" + orderId + "/confirm", HttpMethod.POST,
            new HttpEntity<>("", bearer(tokenA)), String.class);

        // 11) 买家 A 评价
        String reviewBody = json.writeValueAsString(Map.of(
            "orderId", orderId, "rating", 5,
            "content", "货物与描述一致，卖家发货快"));
        ResponseEntity<String> reviewResp = rest.postForEntity(baseUrl() + "/api/v1/reviews",
            new HttpEntity<>(reviewBody, bearer(tokenA)), String.class);
        assertThat(reviewResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 12) 最终断言：订单状态 = CONFIRMED、wallet 里 B 余额增加
        JsonNode orderFinal = json.readTree(rest.exchange(baseUrl() + "/api/v1/orders/" + orderId,
            HttpMethod.GET, new HttpEntity<>(bearer(tokenA)), String.class).getBody()).get("data");
        assertThat(orderFinal.get("status").asText()).isEqualTo("CONFIRMED");

        JsonNode walletB = json.readTree(rest.exchange(baseUrl() + "/api/v1/wallet",
            HttpMethod.GET, new HttpEntity<>(bearer(tokenB)), String.class).getBody()).get("data");
        assertThat(walletB.get("balanceCents").asLong()).isEqualTo(4500L);

        JsonNode walletA = json.readTree(rest.exchange(baseUrl() + "/api/v1/wallet",
            HttpMethod.GET, new HttpEntity<>(bearer(tokenA)), String.class).getBody()).get("data");
        assertThat(walletA.get("frozenCents").asLong()).isEqualTo(0L);
        assertThat(walletA.get("balanceCents").asLong()).isEqualTo(500L); // 5000 - 4500
    }
}
