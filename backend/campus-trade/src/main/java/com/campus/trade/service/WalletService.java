package com.campus.trade.service;

import com.campus.common.error.BusinessException;
import com.campus.common.error.ErrorCode;
import com.campus.common.wallet.WalletApi;
import com.campus.trade.domain.Wallet;
import com.campus.trade.domain.WalletRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 钱包服务：开户、担保划拨、释放提现。implements WalletApi 让 user/order 侧依赖接口。 */
@Service
public class WalletService implements WalletApi {

    @PersistenceContext private EntityManager em;

    private final WalletRepository wallets;

    public WalletService(WalletRepository wallets) {
        this.wallets = wallets;
    }

    public record WalletView(long balanceCents, long frozenCents) {}

    @Transactional
    @Override
    public void openWallet(long userId) {
        if (wallets.existsById(userId)) return;
        Wallet w = new Wallet();
        w.setUserId(userId);
        w.setBalanceCents(0L);
        w.setFrozenCents(0L);
        w.setVersion(0L);
        wallets.save(w);
    }

    @Transactional(readOnly = true)
    public WalletView getBalance(long userId) {
        Wallet w = wallets.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        return new WalletView(w.getBalanceCents(), w.getFrozenCents());
    }

    /** 仅 dev：直接加余额。 */
    @Transactional
    public void topUp(long userId, long amountCents) {
        if (amountCents <= 0 || amountCents > 1_000_000_000L) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "amount out of range");
        }
        Wallet w = wallets.findForUpdate(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        w.setBalanceCents(w.getBalanceCents() + amountCents);
    }

    @Transactional
    @Override
    public void escrow(long buyerUserId, long sellerUserId, long amountCents) {
        if (amountCents <= 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "amount > 0");
        Wallet buyer = wallets.findForUpdate(buyerUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        if (buyer.getBalanceCents() < amountCents) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        Wallet seller = wallets.findForUpdate(sellerUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        buyer.setBalanceCents(buyer.getBalanceCents() - amountCents);
        buyer.setFrozenCents(buyer.getFrozenCents() + amountCents);
        seller.setFrozenCents(seller.getFrozenCents() + amountCents);
        // 事务提交时 JPA 自动落库 + @Version 乐观锁防丢失更新
        // 显式 flush 让 SQL 即时落地（PESSIMISTIC_WRITE 锁依赖数据库事务，flush 后 Hibernate 才发 UPDATE）
        em.flush();
    }

    @Transactional
    @Override
    public void release(long sellerUserId, long buyerUserId, long amountCents) {
        Wallet seller = wallets.findForUpdate(sellerUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        Wallet buyer = wallets.findForUpdate(buyerUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        if (seller.getFrozenCents() < amountCents || buyer.getFrozenCents() < amountCents) {
            throw new BusinessException(ErrorCode.WALLET_VERSION_CONFLICT, "frozen inconsistency");
        }
        seller.setFrozenCents(seller.getFrozenCents() - amountCents);
        seller.setBalanceCents(seller.getBalanceCents() + amountCents);
        buyer.setFrozenCents(buyer.getFrozenCents() - amountCents);
        em.flush();
    }

    /** 取消场景：原路返回 buyer（买家拿回余额、卖家 frozen 减）。 */
    @Transactional
    public void refundFromEscrow(long buyerUserId, long sellerUserId, long amountCents) {
        Wallet seller = wallets.findForUpdate(sellerUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        Wallet buyer = wallets.findForUpdate(buyerUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
        seller.setFrozenCents(Math.max(0, seller.getFrozenCents() - amountCents));
        buyer.setBalanceCents(buyer.getBalanceCents() + amountCents);
        buyer.setFrozenCents(Math.max(0, buyer.getFrozenCents() - amountCents));
        em.flush();
    }
}
