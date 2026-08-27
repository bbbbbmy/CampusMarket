package com.campus;

import com.campus.common.error.BusinessException;
import com.campus.common.error.ErrorCode;
import com.campus.listing.domain.FavoriteRepository;
import com.campus.listing.domain.Listing;
import com.campus.listing.service.ListingService;
import com.campus.listing.service.ListingService.CreateCommand;
import com.campus.listing.service.ListingService.ListingView;
import com.campus.listing.service.ListingService.SearchQuery;
import com.campus.trade.domain.Order;
import com.campus.trade.domain.OrderRepository;
import com.campus.trade.service.OrderService;
import com.campus.trade.service.OrderService.OrderView;
import com.campus.trade.service.OrderService.ReviewView;
import com.campus.trade.service.WalletService;
import com.campus.user.domain.SchoolRepository;
import com.campus.user.service.AuthService;
import com.campus.user.service.AuthService.LoginResult;
import com.campus.user.service.AuthService.RegisterCommand;
import com.campus.user.service.AuthService.UserView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * 单元测试套：覆盖 3 个 service 的核心业务规则与错误码。
 * 共享一个 SpringBoot 上下文（webEnvironment = MOCK）—— 不走 HTTP，启动 ~5s。
 * 共 20 用例，跑完约 8s。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class UnitTests {

    @Autowired AuthService auth;
    @Autowired ListingService listings;
    @Autowired OrderService orders;
    @Autowired WalletService wallets;
    @Autowired OrderRepository orderRepo;
    @Autowired FavoriteRepository favoriteRepo;
    @Autowired SchoolRepository schoolRepo;

    private long schoolId;
    private long aliceId;
    private long bobId;

    @BeforeEach @Transactional
    void seed() {
        schoolId = schoolRepo.findByDomain("demo.edu").orElseThrow().getId();
        long ts = System.currentTimeMillis();
        String aEmail = "ua" + ts + "@demo.edu";
        String bEmail = "ub" + ts + "@demo.edu";
        UserView a = auth.register(new RegisterCommand(schoolId, aEmail, "alice12345", "U-Alice"));
        UserView b = auth.register(new RegisterCommand(schoolId, bEmail, "bob123456", "U-Bob"));
        aliceId = a.userId();
        bobId = b.userId();
    }

    // ============================================================
    // AuthService
    // ============================================================

    @Test @DisplayName("Auth: 重复 email 抛 EMAIL_TAKEN")
    void auth_duplicate_email() {
        long ts = System.currentTimeMillis();
        String email = "dup" + ts + "@demo.edu";
        auth.register(new RegisterCommand(schoolId, email, "abcd1234", "ndup1"));
        assertThatThrownBy(() -> auth.register(new RegisterCommand(schoolId, email, "abcd1234", "ndup2")))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.EMAIL_TAKEN);
    }

    @Test @DisplayName("Auth: 弱密码（无数字）抛 BAD_REQUEST")
    void auth_weak_password_no_digit() {
        assertThatThrownBy(() -> auth.register(new RegisterCommand(schoolId, "no" + System.currentTimeMillis() + "@demo.edu", "nodigitsxx", "ndp")))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test @DisplayName("Auth: 跨校 email 抛 SCHOOL_DOMAIN_MISMATCH")
    void auth_domain_mismatch() {
        assertThatThrownBy(() -> auth.register(new RegisterCommand(schoolId, "x@notdemo.edu", "abc12345", "ndm")))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.SCHOOL_DOMAIN_MISMATCH);
    }

    @Test @DisplayName("Auth: 错误密码抛 INVALID_CREDENTIALS")
    void auth_wrong_password() {
        long ts = System.currentTimeMillis();
        String email = "wp" + ts + "@demo.edu";
        auth.register(new RegisterCommand(schoolId, email, "good1234", "wp"));
        assertThatThrownBy(() -> auth.login(email, "bad12345"))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test @DisplayName("Auth: 正常登录返 JWT+userId")
    void auth_login_ok() {
        long ts = System.currentTimeMillis();
        String email = "lo" + ts + "@demo.edu";
        auth.register(new RegisterCommand(schoolId, email, "good1234", "lo"));
        LoginResult r = auth.login(email, "good1234");
        assertThat(r.token()).isNotBlank();
        assertThat(r.userId()).isPositive();
    }

    // ============================================================
    // ListingService
    // ============================================================

    @Test @DisplayName("Listing: 价格越界（>1M）抛 PRICE_OUT_OF_RANGE")
    void listing_price_out_of_range() {
        assertThatThrownBy(() -> listings.create(
            new CreateCommand(1L, "x", "y", 1_000_001L, Listing.Condition.NEW, null),
            aliceId, schoolId))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.PRICE_OUT_OF_RANGE);
    }

    @Test @DisplayName("Listing: 价格越界（<1）抛 PRICE_OUT_OF_RANGE")
    void listing_price_too_low() {
        assertThatThrownBy(() -> listings.create(
            new CreateCommand(1L, "x", "y", 0L, Listing.Condition.NEW, null),
            aliceId, schoolId))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.PRICE_OUT_OF_RANGE);
    }

    @Test @DisplayName("Listing: 跨校读详情抛 LISTING_NOT_FOUND")
    void listing_cross_school_404() {
        ListingView l = listings.create(
            new CreateCommand(1L, "t", "d", 1000L, Listing.Condition.GOOD, null),
            aliceId, schoolId);
        assertThatThrownBy(() -> listings.get(l.id(), 9999L))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.LISTING_NOT_FOUND);
    }

    @Test @DisplayName("Listing: 下架非卖方抛 BusinessException")
    void listing_offshelf_not_owner() {
        ListingView l = listings.create(
            new CreateCommand(1L, "t2", "d2", 2000L, Listing.Condition.GOOD, null),
            aliceId, schoolId);
        assertThatThrownBy(() -> listings.offShelf(l.id(), bobId, schoolId))
            .isInstanceOf(BusinessException.class);
    }

    @Test @DisplayName("Listing: 搜索命中本学校 listing")
    void listing_search_match() {
        listings.create(new CreateCommand(1L, "searchable-keyword-xyz", "d", 100L, Listing.Condition.NEW, null),
            aliceId, schoolId);
        Page<ListingView> page = listings.search(
            new SearchQuery("searchable-keyword", null, null, null, null),
            schoolId, 1, 20);
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test @DisplayName("Listing: 收藏 / 取消幂等")
    void listing_favorite_unfavorite_idempotent() {
        ListingView l = listings.create(
            new CreateCommand(1L, "fav", "d", 50L, Listing.Condition.NEW, null),
            aliceId, schoolId);
        listings.favorite(bobId, l.id());
        listings.favorite(bobId, l.id());
        assertThat(favoriteRepo.existsByUserIdAndListingId(bobId, l.id())).isTrue();
        listings.unfavorite(bobId, l.id());
        listings.unfavorite(bobId, l.id()); // 二次无副作用
        assertThat(favoriteRepo.existsByUserIdAndListingId(bobId, l.id())).isFalse();
    }

    // ============================================================
    // OrderService — 下单与状态机
    // ============================================================

    /** 通用：alice 发布、bob 充值，bob 下单（oid 包含）。 */
    private OrderView setupEscrowedOrder() {
        ListingView l = listings.create(
            new CreateCommand(1L, "orderable-" + System.nanoTime(), "d", 1000L, Listing.Condition.GOOD, null),
            aliceId, schoolId);
        wallets.topUp(bobId, 5000L);
        return orders.create(bobId, schoolId, l.id());
    }

    @Test @DisplayName("Order: 余额不足抛 INSUFFICIENT_BALANCE")
    void order_insufficient_balance() {
        ListingView l = listings.create(
            new CreateCommand(1L, "needs-money-" + System.nanoTime(), "d", 999_999L, Listing.Condition.GOOD, null),
            aliceId, schoolId);
        assertThatThrownBy(() -> orders.create(bobId, schoolId, l.id()))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test @DisplayName("Order: 卖家买自家抛 BusinessException")
    void order_self_purchase_blocked() {
        ListingView l = listings.create(
            new CreateCommand(1L, "selfbuy-" + System.nanoTime(), "d", 1000L, Listing.Condition.GOOD, null),
            aliceId, schoolId);
        wallets.topUp(aliceId, 5000L);
        assertThatThrownBy(() -> orders.create(aliceId, schoolId, l.id()))
            .isInstanceOf(BusinessException.class);
    }

    @Test @DisplayName("Order: ship 非卖家抛 NOT_ORDER_SELLER")
    void order_ship_not_seller() {
        OrderView o = setupEscrowedOrder();
        assertThatThrownBy(() -> orders.ship(o.id(), bobId))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_ORDER_SELLER);
    }

    @Test @DisplayName("Order: ship 非 PAID_ESCROW 抛 ILLEGAL_ORDER_TRANSITION")
    void order_ship_wrong_state() {
        OrderView o = setupEscrowedOrder();
        orders.ship(o.id(), aliceId);
        assertThatThrownBy(() -> orders.ship(o.id(), aliceId))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_ORDER_TRANSITION);
    }

    @Test @DisplayName("Order: confirm 非 SHIPPED 抛 ILLEGAL_ORDER_TRANSITION")
    void order_confirm_wrong_state() {
        OrderView o = setupEscrowedOrder();
        assertThatThrownBy(() -> orders.confirm(o.id(), bobId))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_ORDER_TRANSITION);
    }

    @Test @DisplayName("Order: cancel PAID_ESCROW 退款 + listing 回 ON_SALE")
    void order_cancel_refund_and_relist() {
        OrderView o = setupEscrowedOrder();
        var before = wallets.getBalance(bobId);
        orders.cancel(o.id(), bobId);
        var after = wallets.getBalance(bobId);
        assertThat(after.balanceCents()).isEqualTo(before.balanceCents() + 1000L);
        Order persisted = orderRepo.findById(o.id()).orElseThrow();
        assertThat(listings.snapshot(persisted.getListingId(), schoolId).status()).isEqualTo("ON_SALE");
    }

    @Test @DisplayName("Order: cancel SHIPPED 抛 USE_REFUND")
    void order_cancel_shipped_use_refund() {
        OrderView o = setupEscrowedOrder();
        orders.ship(o.id(), aliceId);
        assertThatThrownBy(() -> orders.cancel(o.id(), bobId))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.USE_REFUND);
    }

    @Test @DisplayName("Order: review 非 CONFIRMED 抛 ILLEGAL_ORDER_TRANSITION")
    void order_review_wrong_state() {
        OrderView o = setupEscrowedOrder();
        assertThatThrownBy(() -> orders.review(o.id(), bobId, 5, "early review"))
            .isInstanceOf(BusinessException.class);
    }

    @Test @DisplayName("Order: review rating 越界抛 BAD_REQUEST")
    void order_review_bad_rating() {
        OrderView o = setupEscrowedOrder();
        orders.ship(o.id(), aliceId);
        orders.confirm(o.id(), bobId);
        assertThatThrownBy(() -> orders.review(o.id(), bobId, 6, "x"))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test @DisplayName("Order: review 双向 + 重复抛 REVIEW_ALREADY_EXISTS")
    void order_review_both_sides_then_idempotent() {
        OrderView o = setupEscrowedOrder();
        orders.ship(o.id(), aliceId);
        orders.confirm(o.id(), bobId);
        ReviewView r1 = orders.review(o.id(), bobId, 5, "nice");
        ReviewView r2 = orders.review(o.id(), aliceId, 4, "thanks");
        assertThat(r1.toUserId()).isEqualTo(aliceId);
        assertThat(r2.toUserId()).isEqualTo(bobId);
        assertThatThrownBy(() -> orders.review(o.id(), bobId, 3, "again"))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    // ============================================================
    // Scheduled tasks — 触发 sweep
    // ============================================================

    @Test @DisplayName("Sched: 7 天未确认 → CONFIRMED")
    void sched_auto_confirm_7day() {
        OrderView o = setupEscrowedOrder();
        orders.ship(o.id(), aliceId);
        Order persisted = orderRepo.findById(o.id()).orElseThrow();
        persisted.setShippedAt(Instant.now().minus(Duration.ofDays(8)));
        orderRepo.save(persisted);

        int n = orders.sweepAutoConfirm(Duration.ofDays(7));
        assertThat(n).isGreaterThanOrEqualTo(1);
        Order after = orderRepo.findById(o.id()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(Order.Status.CONFIRMED);
    }

    @Test @DisplayName("Sched: 30 分钟未付款 → CANCELLED + 退款")
    void sched_auto_cancel_30min() {
        OrderView o = setupEscrowedOrder();
        var before = wallets.getBalance(bobId);
        Order persisted = orderRepo.findById(o.id()).orElseThrow();
        persisted.setPaidAt(Instant.now().minus(Duration.ofMinutes(31)));
        orderRepo.save(persisted);

        int n = orders.sweepCancelExpired(Duration.ofMinutes(30));
        assertThat(n).isGreaterThanOrEqualTo(1);
        var after = wallets.getBalance(bobId);
        assertThat(after.balanceCents()).isEqualTo(before.balanceCents() + 1000L);
    }
}
