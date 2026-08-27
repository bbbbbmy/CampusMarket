package com.campus.listing.seed;

import com.campus.listing.domain.Category;
import com.campus.listing.domain.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ListingDataSeed implements CommandLineRunner {
    private final CategoryRepository cats;
    public ListingDataSeed(CategoryRepository cats) { this.cats = cats; }

    @Override
    public void run(String... args) {
        seed("教材", 1);
        seed("课外书", 2);
        seed("电子数码", 3);
        seed("生活用品", 4);
    }

    private void seed(String name, int order) {
        boolean exists = cats.findAllByOrderBySortOrderAsc().stream()
            .anyMatch(c -> c.getName().equals(name));
        if (!exists) {
            Category c = new Category();
            c.setName(name);
            c.setSortOrder(order);
            cats.save(c);
        }
    }
}
