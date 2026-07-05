package com.asopoketool.config;

import com.asopoketool.mapper.AdminUserMapper;
import com.asopoketool.model.AdminUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserInitializer.class);

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Checking admin user password configuration...");
        AdminUser admin = adminUserMapper.findByUsername("admin");
        if (admin != null) {
            String currentHash = admin.getPasswordHash();
            if (currentHash == null || currentHash.trim().isEmpty()) {
                logger.info("Admin password hash is empty. Generating BCrypt hash for 'admin123'...");
                String newHash = passwordEncoder.encode("admin123");
                admin.setPasswordHash(newHash);
                adminUserMapper.updatePasswordHash(admin);
                logger.info("Admin password hash successfully initialized with BCrypt.");
            } else {
                logger.info("Admin password hash is already configured. Skipping initialization.");
            }
        } else {
            logger.warn("Admin user 'admin' not found in database.");
        }
    }
}
