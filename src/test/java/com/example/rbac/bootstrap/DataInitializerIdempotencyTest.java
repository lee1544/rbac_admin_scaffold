package com.example.rbac.bootstrap;

import com.example.rbac.common.constant.SystemConstants;
import com.example.rbac.module.permission.repository.PermissionRepository;
import com.example.rbac.module.role.repository.RoleRepository;
import com.example.rbac.module.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class DataInitializerIdempotencyTest {

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void repeatedInitializationDoesNotDuplicateSeedData() {
        DefaultApplicationArguments args = new DefaultApplicationArguments(new String[0]);

        assertDoesNotThrow(() -> dataInitializer.run(args));
        assertDoesNotThrow(() -> dataInitializer.run(args));

        assertEquals(1, userRepository.count());
        assertEquals(SystemConstants.BASE_PERMISSIONS.size(), permissionRepository.count());
        assertEquals(3, roleRepository.count());
    }
}
