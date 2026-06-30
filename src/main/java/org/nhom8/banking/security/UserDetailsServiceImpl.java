package org.nhom8.banking.security;

import lombok.RequiredArgsConstructor;
import org.nhom8.banking.entity.User;
import org.nhom8.banking.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /** Dùng bởi Spring Security khi xác thực email/password */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return CustomUserDetails.from(user);
    }

    /** Dùng bởi JwtAuthenticationFilter sau khi giải mã token */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
        return CustomUserDetails.from(user);
    }
}
