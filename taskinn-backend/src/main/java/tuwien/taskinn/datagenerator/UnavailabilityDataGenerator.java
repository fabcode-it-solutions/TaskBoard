package tuwien.taskinn.datagenerator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.PTO;
import tuwien.taskinn.repo.PTORepository;

/**
 * Generates realistic, spread-out sample employee unavailability (PTO) data. This generator is designed to be called by
 * a master data generator.
 */
@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class UnavailabilityDataGenerator {

    private final PTORepository ptoRepository;
    private final Random random = new Random();

    /**
     * Generates a realistic distribution of PTO records for a subset of employees over the next 6 months.
     *
     * @param employees A list of pre-existing employees to assign PTO to.
     */
    public void generateData(List<Employee> employees) {
        if (ptoRepository.count() > 0) {
            log.info("PTO records already exist. Skipping generation.");
            return;
        }
        if (employees.isEmpty()) {
            log.warn("Cannot generate unavailability data because no employees were found. Skipping.");
            return;
        }

        log.info("Generating realistic sample PTO (unavailability) data...");
        List<PTO> ptosToCreate = new ArrayList<>();
        LocalDate today = LocalDate.now();

        int timeHorizonInDays = 180;

        int employeesWithPtoCount = (int) (employees.size() * 0.7);

        for (int i = 0; i < employeesWithPtoCount; i++) {
            Employee employee = employees.get(i);

            // A chance for a longer vacation (5-10 days)
            if (random.nextDouble() < 0.4) {
                LocalDate start = today.plusDays(random.nextInt(15, timeHorizonInDays - 14));
                LocalDate end = start.plusDays(random.nextInt(7, 14));
                ptosToCreate.add(createPto(employee, start, end));
            }

            // A chance for a short break
            if (random.nextDouble() < 0.6) {
                LocalDate start = today.plusDays(random.nextInt(7, timeHorizonInDays - 5));
                LocalDate end = start.plusDays(random.nextInt(2, 4));
                ptosToCreate.add(createPto(employee, start, end));
            }

            // A chance for a single day off
            if (random.nextDouble() < 0.8) {
                LocalDate dayOff = today.plusDays(random.nextInt(3, timeHorizonInDays));
                ptosToCreate.add(createPto(employee, dayOff, dayOff));
            }
        }

        ptoRepository.saveAll(ptosToCreate);
        log.info("Unavailability data generation complete. Created {} PTO records.", ptosToCreate.size());
    }

    /**
     * A private helper method to build a PTO object.
     */
    private PTO createPto(Employee employee, LocalDate startDate, LocalDate endDate) {
        return PTO.builder().employee(employee).startDate(startDate).endDate(endDate).build();
    }
}
