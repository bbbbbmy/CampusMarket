package com.campus.trade.web;

import com.campus.common.api.ApiResponse;
import com.campus.common.auth.AuthContext;
import com.campus.trade.service.OrderService;
import com.campus.trade.service.OrderService.OrderView;
import com.campus.trade.service.OrderService.ReviewView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderService orders;
    public OrderController(OrderService orders) { this.orders = orders; }

    public record CreateOrderBody(@NotNull Long listingId) {}

    public record ReviewBody(
        @NotNull Long orderId,
        @Min(1) @Max(5) int rating,
        @NotNull @Size(min = 1, max = 500) String content
    ) {}

    @PostMapping("/orders")
    public ApiResponse<OrderView> create(@Valid @RequestBody CreateOrderBody body, HttpServletRequest req) {
        return ApiResponse.ok(orders.create(
            AuthContext.requireUserId(req),
            AuthContext.requireSchoolId(req),
            body.listingId()));
    }

    @GetMapping("/orders/mine")
    public ApiResponse<org.springframework.data.domain.Page<OrderView>> listMine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        return ApiResponse.ok(orders.listMine(AuthContext.requireUserId(req), page, size));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderView> get(@PathVariable long id, HttpServletRequest req) {
        return ApiResponse.ok(orders.get(id, AuthContext.requireUserId(req)));
    }

    @PostMapping("/orders/{id}/ship")
    public ApiResponse<Void> ship(@PathVariable long id, HttpServletRequest req) {
        orders.ship(id, AuthContext.requireUserId(req));
        return ApiResponse.ok(null);
    }

    @PostMapping("/orders/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable long id, HttpServletRequest req) {
        orders.confirm(id, AuthContext.requireUserId(req));
        return ApiResponse.ok(null);
    }

    @PostMapping("/orders/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable long id, HttpServletRequest req) {
        orders.cancel(id, AuthContext.requireUserId(req));
        return ApiResponse.ok(null);
    }

    @PostMapping("/reviews")
    public ApiResponse<ReviewView> review(@Valid @RequestBody ReviewBody body, HttpServletRequest req) {
        return ApiResponse.ok(orders.review(body.orderId(),
            AuthContext.requireUserId(req), body.rating(), body.content()));
    }
}
