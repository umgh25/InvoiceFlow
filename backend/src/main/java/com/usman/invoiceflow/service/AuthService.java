package com.usman.invoiceflow.service;

import com.usman.invoiceflow.config.jwt.JwtService;
import com.usman.invoiceflow.domain.AppUser;
import com.usman.invoiceflow.dto.AuthRequest;
import com.usman.invoiceflow.dto.AuthResponse;
import com.usman.invoiceflow.dto.RegisterRequest;
import com.usman.invoiceflow.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        var user = new AppUser();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER"); // Default role
        userRepository.save(user);
        var jwtToken = jwtService.generateToken(new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), java.util.Collections.emptyList()));
        return new AuthResponse(jwtToken);
    }

    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), java.util.Collections.emptyList()));
        return new AuthResponse(jwtToken);
    }
}
