package com.campus.listing.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "t_listing", indexes = {
    @Index(name = "ix_listing_school_status", columnList = "school_id,status,created_at")
})
public class Listing {

    public enum Condition { NEW, LIKE_NEW, GOOD, FAIR }
    public enum Status { ON_SALE, RESERVED, SOLD, OFF_SHELF }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false) private Long sellerId;
    @Column(name = "school_id", nullable = false) private Long schoolId;
    @Column(name = "category_id", nullable = false) private Long categoryId;

    @Column(nullable = false, length = 60)  private String title;
    @Column(nullable = false, length = 4000) private String description;
    @Column(name = "price_cents", nullable = false) private Long priceCents;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private Condition condition;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private Status status = Status.ON_SALE;

    @Column(name = "cover_image_url", length = 300) private String coverImageUrl;

    @Column(name = "view_count", nullable = false) private Long viewCount = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    @Column(name = "closed_at") private Instant closedAt;

    /** 并发锁用的版本号（@Version）。 */
    @Version
    @Column(name = "version", nullable = false) private Long version = 0L;

    public Long getId() { return id; }
    public Long getSellerId() { return sellerId; }
    public Long getSchoolId() { return schoolId; }
    public Long getCategoryId() { return categoryId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getPriceCents() { return priceCents; }
    public Condition getCondition() { return condition; }
    public Status getStatus() { return status; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public Long getViewCount() { return viewCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getClosedAt() { return closedAt; }

    public void setSellerId(Long v) { this.sellerId = v; }
    public void setSchoolId(Long v) { this.schoolId = v; }
    public void setCategoryId(Long v) { this.categoryId = v; }
    public void setTitle(String v) { this.title = v; }
    public void setDescription(String v) { this.description = v; }
    public void setPriceCents(Long v) { this.priceCents = v; }
    public void setCondition(Condition v) { this.condition = v; }
    public void setStatus(Status v) { this.status = v; this.updatedAt = Instant.now(); }
    public void setCoverImageUrl(String v) { this.coverImageUrl = v; }
    public void setViewCount(Long v) { this.viewCount = v; }
    public void setClosedAt(Instant v) { this.closedAt = v; }
}
