package com.xarchive.UserRole;

import com.xarchive.authentication.entity.Role;
import com.xarchive.authentication.entity.User;
import com.xarchive.authentication.entity.UserRole;
import com.xarchive.authentication.entity.UserRoleId;
import com.xarchive.authentication.repository.RoleRepository;
import com.xarchive.authentication.repository.UserRepository;
import com.xarchive.authentication.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRoleRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    @Transactional
    public void testCreateUserRole() {

        User user = new User();
        user.setUsername("Test");
        user.setPassword("Test");
        user.setFirstName("Test");
        user.setLastName("Test");
        userRepository.save(user);


        Role role = new Role();
        role.setRoleName("ROLE_TEST");
        roleRepository.save(role);

        UserRoleId userRoleId = new UserRoleId();
        userRoleId.setUserId(user.getId());
        userRoleId.setRoleId(role.getId());


        UserRole userRole = new UserRole();
        userRole.setId(userRoleId);
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.save(userRole);

        // Fetch the UserRole and assert it exists
        UserRole foundUserRole = userRoleRepository.findById(userRoleId).orElse(null);
        assertNotNull(foundUserRole);
        assertEquals(userRoleId, foundUserRole.getId());
        assertEquals("Test", foundUserRole.getUser().getUsername());
        assertEquals("ROLE_TEST", foundUserRole.getRole().getRoleName());
    }
}
