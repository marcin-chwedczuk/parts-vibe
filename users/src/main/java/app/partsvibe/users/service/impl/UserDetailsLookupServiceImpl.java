package app.partsvibe.users.service.impl;

import app.partsvibe.users.repo.UserAccountRepository;
import app.partsvibe.users.security.UserPrincipal;
import app.partsvibe.users.service.UserDetailsLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsLookupServiceImpl implements UserDetailsLookupService {
    private static final Logger log = LoggerFactory.getLogger(UserDetailsLookupServiceImpl.class);
    private final UserAccountRepository userAccountRepository;

    public UserDetailsLookupServiceImpl(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        log.info("Loading user details for username={}", username);
        return userAccountRepository
                .findByUsername(username)
                .map(UserPrincipal::new)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
    }
}
