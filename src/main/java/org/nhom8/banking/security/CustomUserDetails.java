package org.nhom8.banking.security;

import lombok.Getter;
import org.nhom8.banking.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final User.UserStatus status;

    private CustomUserDetails(Long id, String email, String password, User.UserStatus status) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.status = status;
    }

    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getStatus()
        );
    }

    @Override public String getUsername()   { return email; }
    @Override public String getPassword()   { return password; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override public boolean isEnabled()               { return status == User.UserStatus.ACTIVE; }
    @Override public boolean isAccountNonLocked()      { return status != User.UserStatus.LOCKED; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
