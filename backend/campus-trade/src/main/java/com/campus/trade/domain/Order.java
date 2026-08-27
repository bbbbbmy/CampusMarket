package com.campus.trade.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "t_order", indexes = {
    @Index(name = "ix_order_buyer", columnList = "buyer_id,created_at"),
    @Index(name = "ix_order_seller", columnList = "seller_id,created_at")
})
public class Order {

    public enum Status { CREATED, PAID_ESCROW, SHIPPED, CONFIRMED, CANCELLED, REFUNDING, REFUNDED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 32, unique = true)
    private String orderNo;

    @Column(name = "buyer_id", nullable = false)  private Long buyerId;
    @Column(name = "seller_id", nullable = false) private Long sellerId;
    @Column(name = "school_id", nullable = false) private Long schoolId;
    @Column(name = "listing_id", nullable = false) private Long listingId;
    @Column(name = "price_cents", nullable = false) private Long priceCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16) private Status status = Status.CREATED;

    @Column(name = "snapshot_title", nullable = false, length = 60)
    private String snapshotTitle;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "paid_at")   private Instant paidAt;
    @Column(name = "shipped_at") private Instant shippedAt;
    @Column(name = "confirmed_at") private Instant confirmedAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public Long getBuyerId() { return buyerId; }
    public Long getSellerId() { return sellerId; }
    public Long getSchoolId() { return schoolId; }
    public Long getListingId() { return listingId; }
    public Long getPriceCents() { return priceCents; }
    public Status getStatus() { return status; }
    public String getSnapshotTitle() { return snapshotTitle; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getShippedAt() { return shippedAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getCancelledAt() { return cancelledAt; }

    public void setOrderNo(String v) { this.orderNo = v; }
    public void setBuyerId(Long v) { this.buyerId = v; }
    public void setSellerId(Long v) { this.sellerId = v; }
    public void setSchoolId(Long v) { this.schoolId = v; }
    public void setListingId(Long v) { this.listingId = v; }
    public void setPriceCents(Long v) { this.priceCents = v; }
    public void setStatus(Status v) { this.status = v; }
    public void setSnapshotTitle(String v) { this.snapshotTitle = v; }
    public void setPaidAt(Instant v) { this.paidAt = v; }
    public void setShippedAt(Instant v) { this.shippedAt = v; }
    public void setConfirmedAt(Instant v) { this.confirmedAt = v; }
    public void setCancelledAt(Instant v) { this.cancelledAt = v; }
}
