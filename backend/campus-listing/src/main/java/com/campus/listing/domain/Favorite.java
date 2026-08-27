package com.campus.listing.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** 收藏：联合主键 (user_id, listing_id)；仅记录归属，不再持有 listing 快照。 */
@Entity
@Table(name = "t_favorite")
@IdClass(Favorite.Key.class)
public class Favorite {

    @Id @Column(name = "user_id")     private Long userId;
    @Id @Column(name = "listing_id")  private Long listingId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }
    public Instant getCreatedAt() { return createdAt; }

    public static class Key implements Serializable {
        private Long userId;
        private Long listingId;
        public Key() {}
        public Key(Long userId, Long listingId) { this.userId = userId; this.listingId = listingId; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(userId, k.userId) && Objects.equals(listingId, k.listingId);
        }
        @Override public int hashCode() { return Objects.hash(userId, listingId); }
    }
}
