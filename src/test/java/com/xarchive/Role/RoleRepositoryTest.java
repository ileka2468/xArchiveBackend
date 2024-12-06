package com.xarchive.Role;

import com.xarchive.authentication.entity.Role;
import com.xarchive.authentication.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RoleRepositoryTest {
    @Autowired private RoleRepository roleRepository;

    public Role makeRole () {
        Role role = new Role();
        role.setRoleName("Test");
        return role;
    }

    @Test
    public void testCreateRole() {
        long recordsBefore = roleRepository.count();
        Role role = makeRole();
        roleRepository.save(role);
        long recordsAfter = roleRepository.count();
        assert recordsAfter == recordsBefore + 1;
    }

    @Test
    public void testUpdateRole() {
        Role role = makeRole();
        roleRepository.save(role);

        role.setRoleName("Update Test");
        roleRepository.save(role);

        Role updatedRole = roleRepository.findById(role.getId()).orElse(null);
        assert updatedRole != null;
        assert updatedRole.getRoleName().equals("Update Test");
    }

    @Test
    public void testDeleteRole() {
        Role role = makeRole();
        roleRepository.save(role);
        roleRepository.delete(role);
        assert roleRepository.findById(role.getId()).isEmpty();
    }
}
