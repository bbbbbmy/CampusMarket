package com.campus.listing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Favorite.Key> {

    /** 只取 listingId，避免装载整个 Favorite 实体造成 JPA 类型不匹配。 */
    @Query("SELECT f.listingId FROM Favorite f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    Page<Long> findListingIdsByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pg);

    boolean existsByUserIdAndListingId(Long userId, Long listingId);

    void deleteByUserIdAndListingId(Long userId, Long listingId);
}
