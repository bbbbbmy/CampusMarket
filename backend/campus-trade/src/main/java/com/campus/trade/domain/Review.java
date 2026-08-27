package com.campus.trade.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "t_review", uniqueConstraints = {
    @UniqueConstraint(name = "uk_review_order_from", columnNames = {"order_id", "from_user_id"})
})
public class Review {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "from_user_id", nullable = false) private Long fromUserId;
    @Column(name = "to_user_id", nullable = false) private Long toUserId;

    @Column(nullable = false) private Integer rating; // 1..5
    @Column(nullable = false, length = 500) private String content;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getFromUserId() { return fromUserId; }
    public Long getToUserId() { return toUserId; }
    public Integer getRating() { return rating; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }

    public void setOrderId(Long v) { this.orderId = v; }
    public void setFromUserId(Long v) { this.fromUserId = v; }
    public void setToUserId(Long v) { this.toUserId = v; }
    public void setRating(Integer v) { this.rating = v; }
    public void setContent(String v) { this.content = v; }
}
