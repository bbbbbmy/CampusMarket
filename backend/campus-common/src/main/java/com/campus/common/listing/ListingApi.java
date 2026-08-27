package com.campus.common.listing;

/** trade-service 用到的 listing 能力入口；由 listing-service 实现 bean。 */
public interface ListingApi {

    /** 内部只读快照：跨校或不存在一律抛 LISTING_NOT_FOUND。 */
    Snapshot snapshot(long id, long callerSchoolId);

    /** 内部：订单创建后将商品置 RESERVED（仅允许 ON_SALE → RESERVED）。 */
    void markReserved(long listingId, long buyerUserId);

    /** 内部：买家确认后将商品置 SOLD（仅允许 RESERVED → SOLD）。 */
    void markSold(long listingId);

    /** 内部：订单取消后将商品从 RESERVED 退回 ON_SALE。 */
    void markOnSale(long listingId);

    record Snapshot(
        long id, long sellerId, long schoolId,
        long priceCents, String title, String status
    ) {}
}
