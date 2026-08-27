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
 *
 * v0.1.1 起本测试只验证闭环，不打印中间响应（debug 已收敛到 UnitTests 中的边界用例）。
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
        // 0) 拉取 Demo 学校
        JsonNode schools = json.readTree(
            rest.getForEntity(baseUrl() + "/api/v1/schools", String.class).getBody()).get("data");
        long schoolId = jsonLong(schools.get(0), "id");
        assertThat(jsonStr(schools.get(0), "domain")).isEqualTo("demo.edu");

        // 1) 注册买家 A + 卖家 B（每个都注册 + 登录拿到 token）
        long ts = System.currentTimeMillis();
        String aEmail = "alice" + ts + "@demo.edu";
        String bEmail = "bob" + ts + "@demo.edu";

        JsonNode regA = json.readTree(rest.postForEntity(baseUrl() + "/api/v1/auth/register",
            new HttpEntity<>(json.writeValueAsString(Map.of(
                "schoolId", schoolId, "email", aEmail, "password", "alice12345", "nickname", "Alice")),
                bearer(null)), String.class).getBody()).get("data");
        long aUserId = jsonLong(regA, "userId");
        assertThat(aUserId).isPositive();

        rest.postForEntity(baseUrl() + "/api/v1/auth/register",
            new HttpEntity<>(json.writeValueAsString(Map.of(
                "schoolId", schoolId, "email", bEmail, "password", "bobpass1234", "nickname", "Bob")),
                bearer(null)), String.class);

        String tokenA = json.readTree(rest.postForEntity(baseUrl() + "/api/v1/auth/login",
            new HttpEntity<>(json.writeValueAsString(Map.of("email", aEmail, "password", "alice12345")), bearer(null)),
            String.class).getBody()).get("data").get("token").asText();
        String tokenB = json.readTree(rest.postForEntity(baseUrl() + "/api/v1/auth/login",
            new HttpEntity<>(json.writeValueAsString(Map.of("email", bEmail, "password", "bobpass1234")), bearer(null)),
            String.class).getBody()).get("data").get("token").asText();

        // 2) 买家 A 充值
        rest.exchange(baseUrl() + "/api/v1/wallet/top-up", HttpMethod.POST,
            new HttpEntity<>(json.writeValueAsString(Map.of("amountCents", 5000)), bearer(tokenA)), String.class);

        // 3) 卖家 B 发布商品
        JsonNode cats = json.readTree(
            rest.getForEntity(baseUrl() + "/api/v1/categories", String.class).getBody()).get("data");
        long categoryId = jsonLong(cats.get(0), "id");

        long listingId = jsonLong(json.readTree(rest.postForEntity(baseUrl() + "/api/v1/listings",
            new HttpEntity<>(json.writeValueAsString(Map.of(
                "categoryId", categoryId,
                "title", "calculus-textbook-9-used",
                "description", "used for graduate exam 2026",
                "priceCents", 4500,
                "condition", "LIKE_NEW",
                "coverImageUrl", "https://example.com/cover.jpg")),
                bearer(tokenB)), String.class).getBody()).get("data"), "id");

        // 4) 买家 A 检索命中
        JsonNode searchHits = json.readTree(rest.exchange(
            baseUrl() + "/api/v1/listings?keyword=calculus&page=1&size=20", HttpMethod.GET,
            new HttpEntity<>(bearer(tokenA)), String.class).getBody()).get("data").get("content");
        assertThat(searchHits.size()).isGreaterThan(0);
        assertThat(jsonLong(searchHits.get(0), "id")).isEqualTo(listingId);

        // 5) 买家 A 加收藏 + 二次幂等
        rest.exchange(baseUrl() + "/api/v1/favorites/" + listingId, HttpMethod.POST,
            new HttpEntity<>("", bearer(tokenA)), String.class);
        rest.exchange(baseUrl() + "/api/v1/favorites/" + listingId, HttpMethod.POST,
            new HttpEntity<>("", bearer(tokenA)), String.class);
        String favMineBody = rest.exchange(baseUrl() + "/api/v1/favorites/mine?page=1&size=20",
            HttpMethod.GET, new HttpEntity<>(bearer(tokenA)), String.class).getBody();
        JsonNode myFavsData = json.readTree(favMineBody).get("data");
        assertThat(myFavsData).as("data not null").isNotNull();
        assertThat(myFavsData.has("content")).as("paged content").isTrue();
        JsonNode myFavs = myFavsData.get("content");
        assertThat(myFavs.size()).as("user A has favorite").isPositive();

        // 6) 下单（PAID_ESCROW）
        JsonNode orderData = json.readTree(rest.postForEntity(baseUrl() + "/api/v1/orders",
            new HttpEntity<>(json.writeValueAsString(Map.of("listingId", listingId)), bearer(tokenA)),
            String.class).getBody()).get("data");
        long orderId = jsonLong(orderData, "id");
        assertThat(jsonStr(orderData, "status")).isEqualTo("PAID_ESCROW");

        // 7) 发货 + 确认收货 + 评价（双向）
        rest.exchange(baseUrl() + "/api/v1/orders/" + orderId + "/ship", HttpMethod.POST,
            new HttpEntity<>("", bearer(tokenB)), String.class);
        rest.exchange(baseUrl() + "/api/v1/orders/" + orderId + "/confirm", HttpMethod.POST,
            new HttpEntity<>("", bearer(tokenA)), String.class);

        rest.postForEntity(baseUrl() + "/api/v1/reviews",
            new HttpEntity<>(json.writeValueAsString(Map.of(
                "orderId", orderId, "rating", 5, "content", "great")), bearer(tokenA)), String.class);
        rest.postForEntity(baseUrl() + "/api/v1/reviews",
            new HttpEntity<>(json.writeValueAsString(Map.of(
                "orderId", orderId, "rating", 4, "content", "thanks")), bearer(tokenB)), String.class);

        // 8) 最终断言：订单 CONFIRMED、卖家钱包到账、买家 frozen 清零
        JsonNode orderFinal = json.readTree(rest.exchange(baseUrl() + "/api/v1/orders/" + orderId,
            HttpMethod.GET, new HttpEntity<>(bearer(tokenA)), String.class).getBody()).get("data");
        assertThat(jsonStr(orderFinal, "status")).isEqualTo("CONFIRMED");

        JsonNode walletB = json.readTree(rest.exchange(baseUrl() + "/api/v1/wallet",
            HttpMethod.GET, new HttpEntity<>(bearer(tokenB)), String.class).getBody()).get("data");
        assertThat(jsonLong(walletB, "balanceCents")).isEqualTo(4500L);

        JsonNode walletA = json.readTree(rest.exchange(baseUrl() + "/api/v1/wallet",
            HttpMethod.GET, new HttpEntity<>(bearer(tokenA)), String.class).getBody()).get("data");
        assertThat(jsonLong(walletA, "frozenCents")).isEqualTo(0L);
        assertThat(jsonLong(walletA, "balanceCents")).isEqualTo(500L); // 5000 - 4500
    }
}
