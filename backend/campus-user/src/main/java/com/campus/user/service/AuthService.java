package com.campus.user.service;

import com.campus.common.auth.PasswordHasher;
import com.campus.common.error.BusinessException;
import com.campus.common.error.ErrorCode;
import com.campus.common.jwt.JwtSupport;
import com.campus.common.wallet.WalletApi;
import com.campus.user.domain.School;
import com.campus.user.domain.SchoolRepository;
import com.campus.user.domain.User;
import com.campus.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 注册 / 登录 / 拉取自己。 */
@Service
public class AuthService {

    public record RegisterCommand(long schoolId, String email, String password, String nickname) {}
    public record UserView(long userId, long schoolId, String email, String nickname, String avatarUrl) {}
    public record LoginResult(String token, long userId, long schoolId) {}

    private final UserRepository users;
    private final SchoolRepository schools;
    private final JwtSupport jwt;
    private final WalletApi walletApi;

    public AuthService(UserRepository users, SchoolRepository schools, JwtSupport jwt, WalletApi walletApi) {
        this.users = users;
        this.schools = schools;
        this.jwt = jwt;
        this.walletApi = walletApi;
    }

    @Transactional
    public UserView register(RegisterCommand cmd) {
        if (cmd.password() == null || cmd.password().length() < 8
            || !cmd.password().matches(".*[A-Za-z].*") || !cmd.password().matches(".*[0-9].*")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "password must be ≥8 chars with letters and digits");
        }
        if (cmd.nickname() == null || cmd.nickname().length() < 2 || cmd.nickname().length() > 24) {
            throw new BusinessException(ErrorCode.NICKNAME_INVALID);
        }
        School school = schools.findById(cmd.schoolId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "school not found"));
        if (school.getStatus() != School.Status.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "school disabled");
        }
        if (!cmd.email().toLowerCase().endsWith("@" + school.getDomain())) {
            throw new BusinessException(ErrorCode.SCHOOL_DOMAIN_MISMATCH);
        }
        if (users.existsByEmail(cmd.email().toLowerCase())) {
            throw new BusinessException(ErrorCode.EMAIL_TAKEN);
        }
        User u = new User();
        u.setEmail(cmd.email().toLowerCase());
        u.setSchoolId(school.getId());
        u.setPasswordHash(PasswordHasher.hash(cmd.password()));
        u.setNickname(cmd.nickname());
        u.setStatus(User.Status.ACTIVE);
        u = users.save(u);
        // 自动开钱包（v0.1 同进程调用）
        walletApi.openWallet(u.getId());
        return view(u);
    }

    public LoginResult login(String email, String password) {
        User u = users.findByEmail(email == null ? "" : email.toLowerCase())
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (u.getStatus() != User.Status.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_FROZEN);
        }
        if (!PasswordHasher.verify(password, u.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        String token = jwt.issue(u.getId(), u.getSchoolId());
        return new LoginResult(token, u.getId(), u.getSchoolId());
    }

    public UserView fetchMe(long userId) {
        User u = users.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "user not found"));
        return view(u);
    }

    private UserView view(User u) {
        return new UserView(u.getId(), u.getSchoolId(), u.getEmail(), u.getNickname(), u.getAvatarUrl());
    }
}
