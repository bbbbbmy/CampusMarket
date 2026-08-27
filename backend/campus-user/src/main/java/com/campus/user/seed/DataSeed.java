package com.campus.user.seed;

import com.campus.user.domain.School;
import com.campus.user.domain.SchoolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时种入 Demo 学校，便于本地演示注册流程。
 * Idempotent：重复启动不会重复插入。
 */
@Component
@Order(0)
public class DataSeed implements CommandLineRunner {

    private final SchoolRepository schools;

    public DataSeed(SchoolRepository schools) {
        this.schools = schools;
    }

    @Override
    public void run(String... args) {
        if (schools.findByDomain("demo.edu").isEmpty()) {
            School s = new School();
            s.setName("Demo University");
            s.setDomain("demo.edu");
            s.setStatus(School.Status.ACTIVE);
            schools.save(s);
        }
    }
}
