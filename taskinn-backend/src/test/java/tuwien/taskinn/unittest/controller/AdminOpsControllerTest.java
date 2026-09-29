package tuwien.taskinn.unittest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tuwien.taskinn.controller.AdminOpsController;
import tuwien.taskinn.datagenerator.MainDataGenerator;
import tuwien.taskinn.repo.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOpsController Unit Tests")
class AdminOpsControllerTest {

    @Mock
    private MainDataGenerator mainDataGenerator;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private MaintenanceTicketRepository maintenanceTicketRepository;

    @Mock
    private CleaningTaskRepository cleaningTaskRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private PTORepository ptoRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private MobileAuthTokenRepository mobileAuthTokenRepository;

    @Mock
    private MinibarConsumptionRepository minibarConsumptionRepository;

    @Mock
    private InvoiceLineRepository invoiceLineRepository;

    @Mock
    private InvoiceInventory invoiceRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @Mock
    private SchedulingRoleRepository schedulingRoleRepository;

    @Mock
    private ShiftScheduleTemplateRepository shiftScheduleTemplateRepository;

    @Mock
    private StaircaseRepository staircaseRepository;

    @InjectMocks
    private AdminOpsController adminOpsController;

    @Test
    @DisplayName("resetDatabase should return OK response when all operations succeed")
    void resetDatabase_whenSuccessful_returnsOkResponse() {
        ResponseEntity<String> response = adminOpsController.resetDatabase();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Database reset and re-seeded successfully.");

        verify(mobileAuthTokenRepository).deleteAll();
        verify(maintenanceTicketRepository).deleteAll();
        verify(cleaningTaskRepository).deleteAll();
        verify(paymentRepository).deleteAll();
        verify(bookingRepository).deleteAll();
        verify(shiftRepository).deleteAll();
        verify(ptoRepository).deleteAll();
        verify(guestRepository).deleteAll();
        verify(roomRepository).deleteAll();
        verify(roomTypeRepository).deleteAll();
        verify(employeeRepository).deleteAll();
        verify(inventoryItemRepository).deleteAll();
        verify(minibarConsumptionRepository).deleteAll();
        verify(invoiceLineRepository).deleteAll();
        verify(invoiceRepository).deleteAll();
        verify(passwordResetTokenRepository).deleteAll();
        verify(revokedTokenRepository).deleteAll();
        verify(schedulingRoleRepository).deleteAll();
        verify(shiftScheduleTemplateRepository).deleteAll();
        verify(staircaseRepository).deleteAll();
        verify(mainDataGenerator).generateData();
    }

    @Test
    @DisplayName("resetDatabase should return internal server error when exception is thrown")
    void resetDatabase_whenExceptionThrown_returnsInternalServerError() {
        String errorMessage = "Database connection failed";
        doThrow(new RuntimeException(errorMessage)).when(mobileAuthTokenRepository).deleteAll();

        ResponseEntity<String> response = adminOpsController.resetDatabase();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("Reset failed:");
        assertThat(response.getBody()).contains(errorMessage);

        verify(mainDataGenerator, never()).generateData();
    }
}
