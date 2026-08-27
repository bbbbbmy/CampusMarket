package com.campus.listing.web;

import com.campus.common.api.ApiResponse;
import com.campus.common.auth.AuthContext;
import com.campus.listing.domain.Category;
import com.campus.listing.domain.Listing;
import com.campus.listing.service.ListingService;
import com.campus.listing.service.ListingService.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ListingController {

    private final ListingService listings;

    public ListingController(ListingService listings) { this.listings = listings; }

    public record CreateBody(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 60) String title,
        @Size(max = 4000) String description,
        @NotNull @Min(1) @Max(1_000_000) Long priceCents,
        @NotNull Listing.Condition condition,
        String coverImageUrl
    ) {}

    public record CategoryView(Long id, String name, Integer sortOrder) {}

    @GetMapping("/categories")
    public ApiResponse<List<CategoryView>> categories() {
        return ApiResponse.ok(listings.categories().stream()
            .map(c -> new CategoryView(c.getId(), c.getName(), c.getSortOrder())).toList());
    }

    @PostMapping("/listings")
    public ApiResponse<ListingView> create(@Valid @RequestBody CreateBody body, HttpServletRequest req) {
        return ApiResponse.ok(listings.create(
            new CreateCommand(body.categoryId(), body.title(), body.description(),
                body.priceCents(), body.condition(), body.coverImageUrl()),
            AuthContext.requireUserId(req), AuthContext.requireSchoolId(req)));
    }

    @GetMapping("/listings")
    public ApiResponse<Page<ListingView>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Listing.Condition condition,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        long schoolId = AuthContext.requireSchoolId(req);
        return ApiResponse.ok(listings.search(
            new SearchQuery(keyword, categoryId, minPrice, maxPrice, condition), schoolId, page, size));
    }

    @GetMapping("/listings/{id}")
    public ApiResponse<ListingView> get(@PathVariable long id, HttpServletRequest req) {
        return ApiResponse.ok(listings.get(id, AuthContext.requireSchoolId(req)));
    }

    @PostMapping("/listings/{id}/off-shelf")
    public ApiResponse<Void> offShelf(@PathVariable long id, HttpServletRequest req) {
        listings.offShelf(id, AuthContext.requireUserId(req), AuthContext.requireSchoolId(req));
        return ApiResponse.ok(null);
    }

    @PostMapping("/favorites/{id}")
    public ApiResponse<Void> favorite(@PathVariable long id, HttpServletRequest req) {
        listings.favorite(AuthContext.requireUserId(req), id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/favorites/{id}")
    public ApiResponse<Void> unfavorite(@PathVariable long id, HttpServletRequest req) {
        listings.unfavorite(AuthContext.requireUserId(req), id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/favorites/mine")
    public ApiResponse<Page<ListingView>> myFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        long uid = AuthContext.requireUserId(req);
        long sid = AuthContext.requireSchoolId(req);
        return ApiResponse.ok(listings.myFavorites(uid, sid, page, size));
    }
}
