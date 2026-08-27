package com.campus.trade.web;

import com.campus.common.api.ApiResponse;
import com.campus.common.auth.AuthContext;
import com.campus.trade.service.WalletService;
import com.campus.trade.service.WalletService.WalletView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService wallets;
    public WalletController(WalletService wallets) { this.wallets = wallets; }

    public record TopUpBody(@NotNull @Positive Long amountCents) {}

    @GetMapping
    public ApiResponse<WalletView> get(HttpServletRequest req) {
        return ApiResponse.ok(wallets.getBalance(AuthContext.requireUserId(req)));
    }

    @PostMapping("/top-up")
    public ApiResponse<WalletView> topUp(@org.springframework.web.bind.annotation.RequestBody TopUpBody body, HttpServletRequest req) {
        wallets.topUp(AuthContext.requireUserId(req), body.amountCents());
        return ApiResponse.ok(wallets.getBalance(AuthContext.requireUserId(req)));
    }
}
