package com.campus.trade.service;

import com.campus.common.error.BusinessException;
import com.campus.common.error.ErrorCode;
import com.campus.common.listing.ListingApi;
import com.campus.trade.domain.Order;
import com.campus.trade.domain.OrderRepository;
import com.campus.trade.domain.Review;
import com.campus.trade.domain.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    public record OrderView(
        long id, String orderNo,
        long buyerId, long sellerId, long schoolId, long listingId,
        long priceCents, Order.Status status,
        String snapshotTitle,
        Instant createdAt, Instant paidAt, Instant shippedAt, Instant confirmedAt, Instant cancelledAt
    ) {}

    public record ReviewView(long id, long orderId, long fromUserId, long toUserId, int rating, String content, Instant createdAt) {}

    private final OrderRepository orders;
    private final ReviewRepository reviews;
    private final WalletService wallet;
    private final ListingApi listings;

    public OrderService(OrderRepository orders, ReviewRepository reviews, WalletService wallet, ListingApi listings) {
        this.orders = orders;
        this.reviews = reviews;
        this.wallet = wallet;
        this.listings = listings;
    }

    @Transactional
    public OrderView create(long buyerUserId, long schoolId, long listingId) {
        // 1) 同校 / 价格 / 标题快照（跨校一律 404）
        ListingApi.Snapshot snap = listings.snapshot(listingId, schoolId);
        // 2) 锁定 listing 状态为 RESERVED（卖家不能买自家、只能 ON_SALE→RESERVED）
        listings.markReserved(listingId, buyerUserId);
        // 3) 担保划拨：买家 balance→frozen，卖家 frozen 增加
        wallet.escrow(buyerUserId, snap.sellerId(), snap.priceCents());
        // 4) 落订单（PAID_ESCROW + paidAt）
        Order o = new Order();
        o.setOrderNo("O" + Long.toString(System.currentTimeMillis(), 36) + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        o.setBuyerId(buyerUserId);
        o.setSellerId(snap.sellerId());
        o.setSchoolId(schoolId);
        o.setListingId(listingId);
        o.setPriceCents(snap.priceCents());
        o.setSnapshotTitle(snap.title());
        o.setStatus(Order.Status.PAID_ESCROW);
        o.setPaidAt(Instant.now());
        return view(orders.save(o));
    }

    @Transactional(readOnly = true)
    public OrderView get(long id, long callerUserId) {
        Order o = orders.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "order"));
        if (!o.getBuyerId().equals(callerUserId) && !o.getSellerId().equals(callerUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "not participant");
        }
        return view(o);
    }

    @Transactional
    public void ship(long id, long callerUserId) {
        Order o = orders.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "order"));
        if (!o.getSellerId().equals(callerUserId)) {
            throw new BusinessException(ErrorCode.NOT_ORDER_SELLER);
        }
        if (o.getStatus() != Order.Status.PAID_ESCROW) {
            throw new BusinessException(ErrorCode.ILLEGAL_ORDER_TRANSITION);
        }
        o.setStatus(Order.Status.SHIPPED);
        o.setShippedAt(Instant.now());
    }

    @Transactional
    public void confirm(long id, long callerUserId) {
        Order o = orders.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "order"));
        if (!o.getBuyerId().equals(callerUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "not buyer");
        }
        if (o.getStatus() != Order.Status.SHIPPED) {
            throw new BusinessException(ErrorCode.ILLEGAL_ORDER_TRANSITION);
        }
        wallet.release(o.getSellerId(), o.getBuyerId(), o.getPriceCents());
        listings.markSold(o.getListingId());
        o.setStatus(Order.Status.CONFIRMED);
        o.setConfirmedAt(Instant.now());
    }

    @Transactional
    public void cancel(long id, long callerUserId) {
        Order o = orders.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "order"));
        if (!o.getBuyerId().equals(callerUserId) && !o.getSellerId().equals(callerUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "not participant");
        }
        if (o.getStatus() == Order.Status.CREATED || o.getStatus() == Order.Status.PAID_ESCROW) {
            if (o.getStatus() == Order.Status.PAID_ESCROW) {
                wallet.refundFromEscrow(o.getBuyerId(), o.getSellerId(), o.getPriceCents());
            }
            // 任何 CREATED/PAID_ESCROW 取消，listing 都应从 RESERVED 回到 ON_SALE
            listings.markOnSale(o.getListingId());
            o.setStatus(Order.Status.CANCELLED);
            o.setCancelledAt(Instant.now());
        } else if (o.getStatus() == Order.Status.SHIPPED) {
            throw new BusinessException(ErrorCode.USE_REFUND);
        } else {
            throw new BusinessException(ErrorCode.ILLEGAL_ORDER_TRANSITION);
        }
    }

    @Transactional
    public ReviewView review(long orderId, long fromUserId, int rating, String content) {
        Order o = orders.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "order"));
        if (o.getStatus() != Order.Status.CONFIRMED) {
            throw new BusinessException(ErrorCode.ILLEGAL_ORDER_TRANSITION, "only confirmed orders can review");
        }
        if (!o.getBuyerId().equals(fromUserId) && !o.getSellerId().equals(fromUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "not participant");
        }
        if (rating < 1 || rating > 5) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "rating 1..5");
        }
        if (content == null || content.isBlank() || content.length() > 500) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "content 1..500 chars");
        }
        if (reviews.findByOrderIdAndFromUserId(orderId, fromUserId).isPresent()) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
        long to = o.getBuyerId().equals(fromUserId) ? o.getSellerId() : o.getBuyerId();
        Review r = new Review();
        r.setOrderId(orderId);
        r.setFromUserId(fromUserId);
        r.setToUserId(to);
        r.setRating(rating);
        r.setContent(content);
        r = reviews.save(r);
        return new ReviewView(r.getId(), r.getOrderId(), r.getFromUserId(), r.getToUserId(),
            r.getRating(), r.getContent(), r.getCreatedAt());
    }

    private OrderView view(Order o) {
        return new OrderView(
            o.getId(), o.getOrderNo(),
            o.getBuyerId(), o.getSellerId(), o.getSchoolId(), o.getListingId(),
            o.getPriceCents(), o.getStatus(),
            o.getSnapshotTitle(),
            o.getCreatedAt(), o.getPaidAt(), o.getShippedAt(), o.getConfirmedAt(), o.getCancelledAt()
        );
    }
}
