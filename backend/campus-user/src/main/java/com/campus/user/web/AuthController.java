package com.campus.user.web;

import com.campus.common.api.ApiResponse;
import com.campus.common.auth.AuthContext;
import com.campus.user.service.AuthService;
import com.campus.user.service.AuthService.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    public record RegisterBody(
        long schoolId,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(min = 2, max = 24) String nickname
    ) {}

    public record LoginBody(
        @NotBlank @Email String email,
        @NotBlank String password
    ) {}

    @PostMapping("/register")
    public ApiResponse<UserView> register(@Valid @RequestBody RegisterBody body) {
        return ApiResponse.ok(auth.register(new RegisterCommand(body.schoolId(), body.email(), body.password(), body.nickname())));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginBody body) {
        return ApiResponse.ok(auth.login(body.email(), body.password()));
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me(HttpServletRequest req) {
        long uid = AuthContext.requireUserId(req);
        return ApiResponse.ok(auth.fetchMe(uid));
    }
}
