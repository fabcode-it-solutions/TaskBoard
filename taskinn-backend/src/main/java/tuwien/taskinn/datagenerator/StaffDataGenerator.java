package tuwien.taskinn.datagenerator;

import com.github.javafaker.Faker;
import java.math.BigDecimal;
import java.util.*;

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
public class StaffDataGenerator {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    // Initialize a single Faker instance for generating realistic names and emails
    private final Faker faker = new Faker(new Locale("en-US"));

    /**
     * Generates a specified number of staff members with a distributed set of roles. If staff members already exist,
     * this method does nothing. This method is called by the MasterDataGenerator.
     *
     * @param count The total number of staff members to generate.
     * @return A list of the created Employee entities.
     */
    protected List<Employee> generateData(int count) {
        if (employeeRepository.count() > 1) {
            log.info("Staff members already exist. Skipping generation.");
            return employeeRepository.findAll();
        }

        if (count <= 0) {
            return new ArrayList<>();
        }

        log.info("Generating {} new staff members...", count);
        List<Employee> createdStaff = new ArrayList<>();

        int generatedCount = 0;

        while (generatedCount < count) {
            createdStaff.add(createEmployee("general", new HashSet<>(Set.of(Roles.EMPLOYEE))));
            generatedCount++;
        }

        log.info("Staff member data generation complete. Created {} new employees.", createdStaff.size());
        return createdStaff;
    }

    /**
     * A helper method to create and save a single Employee with realistic data.
     *
     * @param rolePrefix A prefix for the email to make it easy to identify (e.g., "cook").
     * @param roles The set of roles for this employee.
     * @return The saved Employee entity.
     */
    private Employee createEmployee(String rolePrefix, Set<Roles> roles) {
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();
        String email = String.format("%s.%s%d@taskinn.com", rolePrefix, lastName.toLowerCase().replaceAll("[^a-z]", ""),
                faker.number().numberBetween(10, 10000));

        boolean isActive = faker.random().nextInt(100) >= 15;

        final BigDecimal normalWage = generateRandomWage();

        Employee employee = Employee.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .passwordHash(passwordEncoder.encode("password"))
                .active(isActive)
                .hourlyWage(normalWage)
                .hourlyOvertimeWage(normalWage.multiply(BigDecimal.TWO))
                .workingHoursPerWeek(BigDecimal.valueOf(40))
                .roles(roles)
                .build();

        return employeeRepository.save(employee);
    }

    /**
     * Generates a random hourly wage for more varied data.
     *
     * @return A BigDecimal representing an hourly wage between 15.00 and 25.00.
     */
    private BigDecimal generateRandomWage() {
        double randomValue = 15.0 + (faker.random().nextDouble() * 10.0);
        return BigDecimal.valueOf(randomValue).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
