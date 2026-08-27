package com.campus.user.web;

import com.campus.common.api.ApiResponse;
import com.campus.user.domain.School;
import com.campus.user.domain.SchoolRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 公开接口：拉取所有 ACTIVE 学校（供前端注册页下拉）。 */
@RestController
@RequestMapping("/api/v1/schools")
public class SchoolsController {

    private final SchoolRepository schools;

    public SchoolsController(SchoolRepository schools) {
        this.schools = schools;
    }

    public record SchoolView(long id, String name, String domain) {}

    @GetMapping
    public ApiResponse<List<SchoolView>> list() {
        List<SchoolView> out = schools.findAllByStatus(School.Status.ACTIVE).stream()
            .map(s -> new SchoolView(s.getId(), s.getName(), s.getDomain()))
            .toList();
        return ApiResponse.ok(out);
    }
}
