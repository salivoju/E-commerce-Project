package com.ecommerce.user_service.config.security;

import com.ecommerce.user_service.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserInfoDetails implements UserDetails {

    private final User user;

    public UserInfoDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    // CHANGED: Now using actual User entity values instead of hardcoded true
    @Override
    public boolean isAccountNonExpired() {
        return user.getAccountNonExpired() != null ? user.getAccountNonExpired() : true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getAccountNonLocked() != null ? user.getAccountNonLocked() : true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return user.getCredentialsNonExpired() != null ? user.getCredentialsNonExpired() : true;
    }

    @Override
    public boolean isEnabled() {
        return user.getEnabled() != null ? user.getEnabled() : true;
    }

    // Helper method to get the actual User entity if needed
    public User getUser() {
        return user;
    }
}