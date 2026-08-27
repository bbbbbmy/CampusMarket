package com.campus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * v0.1 单进程启动器：把 4 个业务模块（user / listing / trade / common）
 * 装进同一个 Spring 上下文；同进程内部调用通过 Spring Bean。
 * v0.2 改造为各模块独立 jar + Nacos + OpenFeign。
 */
@SpringBootApplication(scanBasePackages = {
    "com.campus",
    "com.campus.user",
    "com.campus.listing",
    "com.campus.trade"
})
@EntityScan(basePackages = {
    "com.campus.user.domain",
    "com.campus.listing.domain",
    "com.campus.trade.domain"
})
@EnableJpaRepositories(basePackages = {
    "com.campus.user.domain",
    "com.campus.listing.domain",
    "com.campus.trade.domain"
})
@EnableScheduling
public class CampusMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusMarketApplication.class, args);
    }
}
