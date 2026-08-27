package com.campus.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {
    List<School> findAllByStatus(School.Status status);
    Optional<School> findByDomain(String domain);
}
