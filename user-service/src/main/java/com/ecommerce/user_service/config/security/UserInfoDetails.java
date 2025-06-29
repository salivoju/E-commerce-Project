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
        // For now, we will return an empty list as we are not using roles yet.
        return List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getName();
    }

    @Override
    public String getUsername() {
        return user.getName();
    }

    // For simplicity, we are returning true for these.
    // In a real application, you might have logic to disable accounts.
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}