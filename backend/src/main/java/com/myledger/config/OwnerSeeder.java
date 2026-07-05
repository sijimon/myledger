package com.myledger.config;

import com.myledger.entity.Role;
import com.myledger.entity.User;
import com.myledger.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds the single owner account on first boot so login works end-to-end. Idempotent:
 * if any owner already exists, it does nothing. Change the seed password after first login.
 */
@Configuration
@EnableConfigurationProperties(BootstrapProperties.class)
public class OwnerSeeder {

    private static final Logger log = LoggerFactory.getLogger(OwnerSeeder.class);

    @Bean
    public ApplicationRunner seedOwner(UserRepository users,
                                       PasswordEncoder passwordEncoder,
                                       BootstrapProperties props) {
        return args -> {
            if (users.existsByEmailIgnoreCase(props.ownerEmail())) {
                return;
            }
            User owner = new User(
                    props.ownerEmail(),
                    passwordEncoder.encode(props.ownerPassword()),
                    Role.ROLE_OWNER);
            users.save(owner);
            log.info("Seeded owner account '{}'. Change this password after first login.", props.ownerEmail());
        };
    }
}
