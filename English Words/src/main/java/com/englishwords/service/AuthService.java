package com.englishwords.service;

import com.englishwords.common.BusinessException;
import com.englishwords.dto.auth.AuthResponse;
import com.englishwords.dto.auth.LoginRequest;
import com.englishwords.dto.auth.RegisterRequest;
import com.englishwords.dto.auth.UserResponse;
import com.englishwords.entity.User;
import com.englishwords.repository.UserRepository;
import com.englishwords.security.CurrentUserProvider;
import com.englishwords.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MapperService mapperService;
    private final CurrentUserProvider currentUserProvider;
    private final PresetWordBookService presetWordBookService;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        MapperService mapperService,
        CurrentUserProvider currentUserProvider,
        PresetWordBookService presetWordBookService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mapperService = mapperService;
        this.currentUserProvider = currentUserProvider;
        this.presetWordBookService = presetWordBookService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalize(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(isBlank(request.nickname()) ? username : request.nickname().trim());
        userRepository.save(user);
        presetWordBookService.ensureForUser(user);
        return new AuthResponse(jwtService.generateToken(user), mapperService.toUserResponse(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String username = normalize(request.username());
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        presetWordBookService.ensureForUser(user);
        return new AuthResponse(jwtService.generateToken(user), mapperService.toUserResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        Long userId = currentUserProvider.userId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        return mapperService.toUserResponse(user);
    }

    private String normalize(String username) {
        return username.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
