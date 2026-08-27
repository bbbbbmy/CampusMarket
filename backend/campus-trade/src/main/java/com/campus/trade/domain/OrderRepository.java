package com.campus.trade.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNo(String orderNo);

    /** §6.6 PAID_ESCROW 超时（30 分钟未付款）：虽然下单即付款，仍保留兜底，便于后续接入真实支付。 */
    List<Order> findByStatusAndPaidAtBefore(Order.Status status, Instant before);

    /** §6.7 SHIPPED 超时（7 天未确认）：定时器扫描用。 */
    List<Order> findByStatusAndShippedAtBefore(Order.Status status, Instant before);

    /** 「我的订单」—— 买家或卖家任一为我。Spring Data 推导按 createdAt desc 排序。 */
    Page<Order> findByBuyerIdOrSellerIdOrderByCreatedAtDesc(Long buyerId, Long sellerId, Pageable pg);
}
