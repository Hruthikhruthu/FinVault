package com.finvault.auth;

import com.finvault.common.repository.UserRepository;
import com.finvault.common.security.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class FinVaultUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public FinVaultUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmailIgnoreCase(username)
            .map(UserPrincipal::from)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}

