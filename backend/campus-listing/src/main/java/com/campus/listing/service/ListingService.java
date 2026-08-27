package com.campus.listing.service;

import com.campus.common.error.BusinessException;
import com.campus.common.error.ErrorCode;
import com.campus.common.listing.ListingApi;
import com.campus.listing.domain.Category;
import com.campus.listing.domain.CategoryRepository;
import com.campus.listing.domain.Favorite;
import com.campus.listing.domain.FavoriteRepository;
import com.campus.listing.domain.Listing;
import com.campus.listing.domain.ListingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.time.Instant;
import java.util.List;

@Service
public class ListingService implements ListingApi {

    public record CreateCommand(
        long categoryId, String title, String description,
        long priceCents, Listing.Condition condition, String coverImageUrl) {}

    public record ListingView(
        long id, long sellerId, long schoolId, long categoryId,
        String title, String description, long priceCents,
        Listing.Condition condition, Listing.Status status,
        String coverImageUrl, long viewCount, Instant createdAt) {}

    public record SearchQuery(
        String keyword, Long categoryId,
        Long minPriceCents, Long maxPriceCents, Listing.Condition condition) {}

    private final ListingRepository listings;
    private final CategoryRepository categories;
    private final FavoriteRepository favorites;

    public ListingService(ListingRepository listings, CategoryRepository categories, FavoriteRepository favorites) {
        this.listings = listings;
        this.categories = categories;
        this.favorites = favorites;
    }

    @Transactional
    public ListingView create(CreateCommand cmd, long userId, long schoolId) {
        if (cmd.priceCents() < 1 || cmd.priceCents() > 1_000_000L) {
            throw new BusinessException(ErrorCode.PRICE_OUT_OF_RANGE);
        }
        if (cmd.title() == null || cmd.title().isBlank() || cmd.title().length() > 60) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "title 1-60 chars");
        }
        if (cmd.description() != null && cmd.description().length() > 4000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "description ≤ 4000 chars");
        }
        if (categories.findById(cmd.categoryId()).isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "category not found");
        }
        Listing l = new Listing();
        l.setSellerId(userId);
        l.setSchoolId(schoolId);
        l.setCategoryId(cmd.categoryId());
        l.setTitle(cmd.title());
        l.setDescription(cmd.description());
        l.setPriceCents(cmd.priceCents());
        l.setCondition(cmd.condition());
        l.setCoverImageUrl(cmd.coverImageUrl());
        l.setStatus(Listing.Status.ON_SALE);
        return view(listings.save(l));
    }

    @Transactional(readOnly = true)
    public ListingView get(long id, long currentSchoolId) {
        Listing l = listings.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.LISTING_NOT_FOUND));
        if (!l.getSchoolId().equals(currentSchoolId)
            || (l.getStatus() != Listing.Status.ON_SALE && l.getStatus() != Listing.Status.RESERVED)) {
            throw new BusinessException(ErrorCode.LISTING_NOT_FOUND);
        }
        l.setViewCount(l.getViewCount() + 1);
        return view(l);
    }

    @Transactional(readOnly = true)
    public Page<ListingView> search(SearchQuery q, long currentSchoolId, int page, int size) {
        Pageable pg = PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(1, size), 50));
        return listings.search(
            currentSchoolId, Listing.Status.ON_SALE,
            q.keyword(), q.categoryId(), q.minPriceCents(), q.maxPriceCents(), q.condition(),
            pg
        ).map(this::view);
    }

    @Transactional
    public void offShelf(long id, long userId, long currentSchoolId) {
        Listing l = listings.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.LISTING_NOT_FOUND));
        if (!l.getSchoolId().equals(currentSchoolId)) {
            throw new BusinessException(ErrorCode.LISTING_NOT_FOUND);
        }
        if (!l.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "not owner");
        }
        if (l.getStatus() != Listing.Status.ON_SALE) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATUS_TRANSITION);
        }
        l.setStatus(Listing.Status.OFF_SHELF);
        l.setClosedAt(Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Snapshot snapshot(long id, long callerSchoolId) {
        Listing l = listings.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.LISTING_NOT_FOUND));
        if (!l.getSchoolId().equals(callerSchoolId)) {
            throw new BusinessException(ErrorCode.LISTING_NOT_FOUND);
        }
        return new Snapshot(l.getId(), l.getSellerId(), l.getSchoolId(),
            l.getPriceCents(), l.getTitle(), l.getStatus().name());
    }

    @Override
    @Transactional
    public void markReserved(long listingId, long buyerUserId) {
        Listing l = listings.findForUpdate(listingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.LISTING_NOT_FOUND));
        if (l.getStatus() != Listing.Status.ON_SALE) {
            throw new BusinessException(ErrorCode.LISTING_NOT_AVAILABLE);
        }
        if (l.getSellerId().equals(buyerUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "seller cannot buy own listing");
        }
        l.setStatus(Listing.Status.RESERVED);
    }

    @Override
    @Transactional
    public void markSold(long listingId) {
        Listing l = listings.findForUpdate(listingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.LISTING_NOT_FOUND));
        if (l.getStatus() != Listing.Status.RESERVED) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATUS_TRANSITION);
        }
        l.setStatus(Listing.Status.SOLD);
        l.setClosedAt(Instant.now());
    }

    @Override
    @Transactional
    public void markOnSale(long listingId) {
        Listing l = listings.findForUpdate(listingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.LISTING_NOT_FOUND));
        if (l.getStatus() != Listing.Status.RESERVED) {
            // 幂等：已经是 ON_SALE 直接返回
            if (l.getStatus() == Listing.Status.ON_SALE) return;
            throw new BusinessException(ErrorCode.ILLEGAL_STATUS_TRANSITION);
        }
        l.setStatus(Listing.Status.ON_SALE);
        l.setClosedAt(null);
    }

    public List<Category> categories() {
        return categories.findAllByOrderBySortOrderAsc();
    }

    /** §5.6 收藏 — 幂等。 */
    @Transactional
    public void favorite(long userId, long listingId) {
        Favorite.Key key = new Favorite.Key(userId, listingId);
        if (favorites.existsById(key)) return;
        // 仅当 listing 属于当前用户学校（防止枚举）
        Listing l = listings.findById(listingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.LISTING_NOT_FOUND));
        if (l.getSchoolId() == null) {
            throw new BusinessException(ErrorCode.LISTING_NOT_FOUND);
        }
        Favorite f = new Favorite();
        f.setUserId(userId);
        f.setListingId(listingId);
        favorites.save(f);
    }

    /** §5.6 取消收藏 — 幂等。 */
    @Transactional
    public void unfavorite(long userId, long listingId) {
        favorites.deleteByUserIdAndListingId(userId, listingId);
    }

    /** §5.6 我的收藏列表 — 按收藏时间倒序，未返回的 listing 已被删时跳过。 */
    @Transactional(readOnly = true)
    public Page<ListingView> myFavorites(long userId, long currentSchoolId, int page, int size) {
        Pageable pg = PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(1, size), 50));
        Page<Long> ids = favorites.findListingIdsByUserIdOrderByCreatedAtDesc(userId, pg);
        if (ids.isEmpty()) return Page.empty();
        // 直接查 db，按 currentSchoolId 强制过滤
        List<Listing> rows = listings.findAllById(ids.getContent()).stream()
            .filter(l -> l.getSchoolId().equals(currentSchoolId))
            .toList();
        return new PageImpl<>(rows.stream().map(this::view).toList(), pg, ids.getTotalElements());
    }

    private ListingView view(Listing l) {
        return new ListingView(
            l.getId(), l.getSellerId(), l.getSchoolId(), l.getCategoryId(),
            l.getTitle(), l.getDescription(), l.getPriceCents(),
            l.getCondition(), l.getStatus(),
            l.getCoverImageUrl(), l.getViewCount(), l.getCreatedAt());
    }
}
