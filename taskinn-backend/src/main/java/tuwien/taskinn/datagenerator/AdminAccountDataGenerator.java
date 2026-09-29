package tuwien.taskinn.datagenerator;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.repo.EmployeeRepository;

@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class AdminAccountDataGenerator {

    private final EmployeeRepository employeeRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * Generates the default admin account if it does not already exist.
     */
    protected Employee generateData() {
        if (!employeeRepository.existsByEmail("admin@taskinn.com")) {
            log.info("Initial admin account not found. Creating default admin account.");
            Employee adminUser = Employee.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("admin@taskinn.com")
                    .passwordHash(passwordEncoder.encode("password"))
                    .active(true)
                    .roles(Set.of(Roles.ADMIN))
                    .build();

            employeeRepository.save(adminUser);
            return adminUser;
        }
        return null;
    }
}
