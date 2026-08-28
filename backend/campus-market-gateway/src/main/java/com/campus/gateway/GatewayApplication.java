package com.campus.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * v0.2.1 R1 网关启动器：
 * - 默认（profile 不激活）：作为 standalone Spring Boot 启动，启动路由表占位但不下游
 * - profile=nacos 时启用服务发现 + Sentinel 限流（v0.2.1 docker-compose 验证）
 *
 * 单进程 demo 走 campus-app（v0.1.2） —— 此模块在用户切换到 v0.2.x 才生效。
 */
@SpringBootApplication(scanBasePackages = "com.campus")
@ConditionalOnProperty(name = "campus.gateway.enabled", havingValue = "true", matchIfMissing = false)
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
