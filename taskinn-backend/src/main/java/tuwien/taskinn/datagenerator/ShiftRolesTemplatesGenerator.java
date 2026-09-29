package tuwien.taskinn.datagenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.SchedulingRole;
import tuwien.taskinn.entity.ShiftScheduleTemplate;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.SchedulingRoleRepository;
import tuwien.taskinn.repo.ShiftScheduleTemplateRepository;

@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class ShiftRolesTemplatesGenerator {

    private final EmployeeRepository employeeRepository;
    private final SchedulingRoleRepository schedulingRoleRepository;
    private final ShiftScheduleTemplateRepository shiftScheduleTemplateRepository;

    private static final Map<String, Integer> ROLE_RATIOS =
            Map.of("Housekeeping", 40, "Front Desk", 25, "Maintenance", 10, "Kitchen Staff", 25);

    private static final Map<String, Integer> TEMPLATE_EMPLOYEE_COUNTS =
            Map.of("Housekeeping", 5, "Front Desk", 1, "Maintenance", 1, "Kitchen Staff", 2);

    /**
     * Generates shift roles and assigns them to the provided list of employees.
     */
    public void generate(List<Employee> employees) {
        if (employees.isEmpty()) {
            log.warn("Cannot assign scheduling roles as no employees were provided.");
            return;
        }

        List<SchedulingRole> roles = createShiftRoles();
        assignRolesToEmployees(employees, roles);
        createShiftScheduleTemplates(roles);
    }

    private List<SchedulingRole> createShiftRoles() {
        if (schedulingRoleRepository.count() > 0) {
            log.info("Scheduling roles already exist. Skipping creation.");
            return schedulingRoleRepository.findAll();
        }

        log.info("Creating scheduling roles...");
        List<SchedulingRole> rolesToCreate = ROLE_RATIOS.keySet()
                .stream()
                .map(roleName -> SchedulingRole.builder().roleName(roleName).build())
                .collect(Collectors.toList());

        return schedulingRoleRepository.saveAll(rolesToCreate);
    }

    /**
     * Assigns scheduling roles to a list of employees based on predefined percentage ratios.
     *
     * @param employees The list of employees to assign roles to.
     * @param roles The list of available SchedulingRole entities.
     */
    private void assignRolesToEmployees(List<Employee> employees, List<SchedulingRole> roles) {
        List<Employee> mutableEmployees = new ArrayList<>(employees);

        Map<String, SchedulingRole> rolesByName =
                roles.stream().collect(Collectors.toMap(SchedulingRole::getRoleName, Function.identity()));

        Collections.shuffle(mutableEmployees);

        int totalEmployees = mutableEmployees.size();
        int assignedCount = 0;

        for (Map.Entry<String, Integer> entry : ROLE_RATIOS.entrySet()) {
            String roleName = entry.getKey();
            Integer percentage = entry.getValue();

            SchedulingRole role = rolesByName.get(roleName);
            if (role == null) {
                log.warn("Scheduling role '{}' not found in database. Skipping assignment.", roleName);
                continue;
            }

            int targetCount = (int) Math.round(totalEmployees * (percentage / 100.0));

            for (int i = 0; i < targetCount && assignedCount < totalEmployees; i++) {
                Employee employee = mutableEmployees.get(assignedCount);
                employee.setSchedulingRole(role);
                assignedCount++;
            }
        }

        SchedulingRole defaultRole = rolesByName.get("Housekeeping");
        while (assignedCount < totalEmployees) {
            Employee employee = mutableEmployees.get(assignedCount);
            if (employee.getEmail().equals("admin@taskinn.com")) {
                assignedCount++;
                continue;
            }

            if (employee.getSchedulingRole() == null) {
                employee.setSchedulingRole(defaultRole);
            }
            assignedCount++;
        }

        employeeRepository.saveAll(mutableEmployees);
        log.info("Assigned roles to {} employees...", totalEmployees);
    }

    private void createShiftScheduleTemplates(List<SchedulingRole> roles) {
        if (shiftScheduleTemplateRepository.count() > 0) {
            log.info("Shift schedule templates already exist. Skipping creation.");
            return;
        }

        for (SchedulingRole role : roles) {
            String templateName = role.getRoleName() + " Standard Template";
            Integer baseCount = TEMPLATE_EMPLOYEE_COUNTS.getOrDefault(role.getRoleName(), 1);
            float weekendMultiplier = role.getRoleName().equals("Housekeeping") ? 1.4f : 2;
            String content = String.format(
                    "{\"dailyHours\":[8,8,8,8,8,10,10],\"staffingLevels\":[%d,%d,%d,%d,%d,%d,%d],\"associatedRole\":{\"id\":%d,\"name\":\"%s\"}}",
                    baseCount, baseCount, baseCount, baseCount, baseCount, (int) (baseCount * weekendMultiplier),
                    (int) (baseCount * weekendMultiplier), role.getId(), role.getRoleName());

            ShiftScheduleTemplate template =
                    ShiftScheduleTemplate.builder().name(templateName).content(content).build();
            this.shiftScheduleTemplateRepository.save(template);
        }
    }
}
