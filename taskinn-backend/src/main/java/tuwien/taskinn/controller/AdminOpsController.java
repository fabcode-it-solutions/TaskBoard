package tuwien.taskinn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tuwien.taskinn.annotation.RequireRole;
import tuwien.taskinn.datagenerator.MainDataGenerator;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.repo.*;

@RestController
@RequestMapping("/api/v1/admin/ops")
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class AdminOpsController {

    private final MainDataGenerator mainDataGenerator;

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final EmployeeRepository employeeRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final CleaningTaskRepository cleaningTaskRepository;
    private final PaymentRepository paymentRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ShiftRepository shiftRepository;
    private final PTORepository ptoRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final MinibarConsumptionRepository minibarConsumptionRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceInventory invoiceRepository;
    private final MobileAuthTokenRepository mobileAuthTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final SchedulingRoleRepository schedulingRoleRepository;
    private final ShiftScheduleTemplateRepository shiftScheduleTemplateRepository;
    private final StaircaseRepository staircaseRepository;

    @PostMapping("/reset-database")
    @RequireRole(Roles.ADMIN)
    public ResponseEntity<String> resetDatabase() {
        log.warn("ADMIN INITIATED FULL DATABASE RESET");

        try {
            log.info("Deleting all data...");

            mobileAuthTokenRepository.deleteAll();
            maintenanceTicketRepository.deleteAll();
            cleaningTaskRepository.deleteAll();
            paymentRepository.deleteAll();
            bookingRepository.deleteAll();

            shiftRepository.deleteAll();
            ptoRepository.deleteAll();

            guestRepository.deleteAll();
            roomRepository.deleteAll();
            roomTypeRepository.deleteAll();

            employeeRepository.deleteAll();
            inventoryItemRepository.deleteAll();

            minibarConsumptionRepository.deleteAll();
            invoiceLineRepository.deleteAll();
            invoiceRepository.deleteAll();

            passwordResetTokenRepository.deleteAll();
            revokedTokenRepository.deleteAll();
            schedulingRoleRepository.deleteAll();
            shiftScheduleTemplateRepository.deleteAll();
            staircaseRepository.deleteAll();

            log.info("Rerunning Data Generation...");
            mainDataGenerator.generateData();

            log.info("Database reset and re-seeding completed successfully.");
            return ResponseEntity.ok("Database reset and re-seeded successfully.");

        } catch (Exception e) {
            log.error("Database reset failed", e);
            return ResponseEntity.internalServerError().body("Reset failed: " + e.getMessage());
        }
    }
}
