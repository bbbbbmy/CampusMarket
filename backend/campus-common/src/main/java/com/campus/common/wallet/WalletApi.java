package com.campus.common.wallet;

/**
 * Wallet 内部 API（其他模块调用入口）。
 * v0.1 同进程调用；v0.2 改造为 OpenFeign 接口。
 */
public interface WalletApi {

    /** 给指定用户开户（user-service 注册成功后调用）。 */
    void openWallet(long userId);

    /** 内部转账：扣 buyer.frozen、加 seller.frozen；由 order-service 在事务内调用。 */
    void escrow(long buyerUserId, long sellerUserId, long amountCents);

    /** 确认收货：减 seller.frozen、加 seller.balance、减 buyer.frozen。 */
    void release(long sellerUserId, long buyerUserId, long amountCents);
}
