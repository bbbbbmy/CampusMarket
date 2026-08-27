package com.campus.trade.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "t_wallet")
public class Wallet implements Serializable {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "balance_cents", nullable = false)
    private Long balanceCents = 0L;

    @Column(name = "frozen_cents", nullable = false)
    private Long frozenCents = 0L;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBalanceCents() { return balanceCents; }
    public void setBalanceCents(Long v) { this.balanceCents = v; }
    public Long getFrozenCents() { return frozenCents; }
    public void setFrozenCents(Long v) { this.frozenCents = v; }
    public Long getVersion() { return version; }
    public void setVersion(Long v) { this.version = v; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wallet w)) return false;
        return Objects.equals(userId, w.userId);
    }
    @Override public int hashCode() { return Objects.hashCode(userId); }
}
