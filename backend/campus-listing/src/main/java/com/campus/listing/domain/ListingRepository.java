package com.campus.listing.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    Page<Listing> findBySchoolIdAndStatus(Long schoolId, Listing.Status status, Pageable pg);

    @Query("""
        SELECT l FROM Listing l
        WHERE l.schoolId = :schoolId AND l.status = :status
          AND (:keyword IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR l.categoryId = :categoryId)
          AND (:minCents IS NULL OR l.priceCents >= :minCents)
          AND (:maxCents IS NULL OR l.priceCents <= :maxCents)
          AND (:condition IS NULL OR l.condition = :condition)
        """)
    Page<Listing> search(@Param("schoolId") Long schoolId,
                         @Param("status") Listing.Status status,
                         @Param("keyword") String keyword,
                         @Param("categoryId") Long categoryId,
                         @Param("minCents") Long minCents,
                         @Param("maxCents") Long maxCents,
                         @Param("condition") Listing.Condition condition,
                         Pageable pg);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Listing l WHERE l.id = :id")
    Optional<Listing> findForUpdate(@Param("id") Long id);
}
