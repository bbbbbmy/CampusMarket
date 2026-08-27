package com.campus.trade.schedule;

import com.campus.trade.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * §6.6/§6.7 定时器：
 * - 每分钟扫一次 PAID_ESCROW > 30min → 自动取消并退款（CAREFUL：v0.1 是下单即付款，
 *   因此这个 sweep 在 v0.1 主要是结构占位；接真实支付时可启用）。
 * - 每 10 分钟扫一次 SHIPPED > 7day → 自动确认放款。
 *
 * 生产环境固定频率；测试时直接调用 sweepCancelExpired / sweepAutoConfirm。
 */
@Component
public class TradeScheduler {

    private static final Logger log = LoggerFactory.getLogger(TradeScheduler.class);

    /** 30 分钟超时取消。 */
    public static final Duration UNPAID_GRACE = Duration.ofMinutes(30);

    /** 7 天超时自动确认。 */
    public static final Duration AUTO_CONFIRM_AGE = Duration.ofDays(7);

    private final OrderService orders;

    public TradeScheduler(OrderService orders) {
        this.orders = orders;
    }

    @Scheduled(fixedDelayString = "${campus.trade.sweep-unpaid-millis:60000}")
    public void sweepUnpaid() {
        int n = orders.sweepCancelExpired(UNPAID_GRACE);
        if (n > 0) log.info("[sched] auto-cancelled {} orders older than {}", n, UNPAID_GRACE);
    }

    @Scheduled(fixedDelayString = "${campus.trade.sweep-auto-confirm-millis:600000}")
    public void sweepAutoConfirm() {
        int n = orders.sweepAutoConfirm(AUTO_CONFIRM_AGE);
        if (n > 0) log.info("[sched] auto-confirmed {} orders older than {}", n, AUTO_CONFIRM_AGE);
    }

    // 测试用 getter
    public OrderService getOrders() { return orders; }
}
