package tuwien.taskinn.basetest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Set;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.Guest;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.Point;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.repo.*;
import tuwien.taskinn.services.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    // Repositories to clean up from
    @Autowired
    protected EmployeeRepository employeeRepository;

    @Autowired
    protected PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    protected RevokedTokenRepository revokedTokenRepository;

    @Autowired
    protected GuestRepository guestRepository;

    @Autowired
    protected BookingRepository bookingRepository;

    @Autowired
    protected RoomTypeRepository roomTypeRepository;

    @Autowired
    protected RoomRepository roomRepository;

    @Autowired
    protected ShiftRepository shiftRepository;

    @Autowired
    protected SchedulingRoleRepository schedulingRoleRepository;

    @Autowired
    protected ShiftScheduleTemplateRepository scheduleTemplateRepository;

    @Autowired
    protected PTORepository ptoRepository;

    @Autowired
    protected CleaningTaskRepository taskRepository;

    @Autowired
    protected MaintenanceTicketRepository maintenanceTicketRepository;

    @Autowired
    protected InventoryItemRepository inventoryItemRepository;

    @Autowired
    protected MinibarConsumptionRepository minibarConsumptionRepository;

    @Autowired
    protected UserSettingRepository settingRepository;

    @Autowired
    protected StaircaseRepository staircaseRepository;

    @Autowired
    protected MobileAuthTokenRepository mobileAuthTokenRepository;

    protected static final String ADMIN_FIRST_NAME = "Admin";
    protected static final String ADMIN_LAST_NAME = "User";
    protected static final String ADMIN_EMAIL = "admin@taskinn.comn";

    /**
     * This method is executed automatically after each test. It cleans all data from the repositories to ensure that
     * tests are isolated and do not interfere with each other. The order of deletion is important to avoid foreign key
     * constraint violations.
     */
    @AfterEach
    @Transactional
    public void cleanup() {
        settingRepository.deleteAll();
        taskRepository.deleteAll();
        scheduleTemplateRepository.deleteAll();
        ptoRepository.deleteAll();
        shiftRepository.deleteAll();
        maintenanceTicketRepository.deleteAll();
        minibarConsumptionRepository.deleteAll();
        mobileAuthTokenRepository.deleteAll();
        revokedTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        employeeRepository.deleteAll();
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
        roomTypeRepository.deleteAll();
        guestRepository.deleteAll();
        schedulingRoleRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        staircaseRepository.deleteAll();
    }

    /**
     * A utility method to create a standard ADMIN user in the database and generate a valid JWT for them.
     *
     * @return A valid JWT access token for an authenticated ADMIN user.
     */
    protected String getAdminAuthToken() {
        Employee admin = Employee.builder()
                .firstName(ADMIN_FIRST_NAME)
                .lastName(ADMIN_LAST_NAME)
                .active(true)
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode("password"))
                .roles(Set.of(Roles.ADMIN))
                .build();

        employeeRepository.save(admin);

        return jwtService.generateAccessToken(admin.getEmail(), admin.getRoles());
    }

    /**
     * A utility method to create a standard EMPLOYEE user in the database and generate a valid JWT for them.
     *
     * @return A valid JWT access token for an authenticated EMPLOYEE user.
     */
    protected String getEmployeeAuthToken() {
        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .active(true)
                .email("employee@taskinn.com")
                .passwordHash(passwordEncoder.encode("password"))
                .roles(Set.of(Roles.EMPLOYEE))
                .build();

        employeeRepository.save(employee);

        return jwtService.generateAccessToken(employee.getEmail(), employee.getRoles());
    }

    protected Employee createAndSaveEmployee(String email, Roles role) {
        Employee employee = Employee.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .passwordHash(passwordEncoder.encode("password"))
                .roles(Set.of(role))
                .active(true)
                .build();
        return employeeRepository.save(employee);
    }

    protected String generateToken(Employee employee) {
        return jwtService.generateAccessToken(employee.getEmail(), employee.getRoles());
    }

    protected List<Long> createValidBooking() {
        RoomType doubleRoomType = RoomType.builder()
                .name("Double Deluxe")
                .description("Deluxe double room with king size bed")
                .capacity(1)
                .basePrice(new BigDecimal("120.00"))
                .singleBed(1)
                .doubleBed(0)
                .build();
        roomTypeRepository.save(doubleRoomType);

        RoomType tripleRoomType = RoomType.builder()
                .name("Triple Deluxe")
                .description("Deluxe triple room with king size bed and single bed")
                .capacity(1)
                .basePrice(new BigDecimal("180.00"))
                .singleBed(1)
                .doubleBed(0)
                .build();
        roomTypeRepository.save(tripleRoomType);

        Room room101 = Room.builder()
                .roomNumber("101")
                .floor(1)
                .status(RoomStatus.OCCUPIED)
                .active(true)
                .roomType(doubleRoomType)
                .svgPoints(List.of(new Point(10.0, 10.0), new Point(60.0, 10.0), new Point(60.0, 40.0),
                        new Point(10.0, 40.0)))
                .build();
        roomRepository.save(room101);

        Room room102 = Room.builder()
                .roomNumber("102")
                .floor(1)
                .status(RoomStatus.OCCUPIED)
                .active(true)
                .roomType(tripleRoomType)
                .svgPoints(List.of(new Point(70.0, 10.0), new Point(120.0, 10.0), new Point(120.0, 40.0),
                        new Point(70.0, 40.0)))
                .build();
        roomRepository.save(room102);

        Guest guest = Guest.builder()
                .firstName("Alice")
                .lastName("Smith")
                .dateOfBirth(LocalDate.of(1988, 4, 15))
                .email("alice.smith@example.com")
                .phone("+1234567890")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        guestRepository.save(guest);

        Booking booking = Booking.builder()
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(10))
                .numAdults(2)
                .numChildren(0)
                .status(BookingStatus.CONFIRMED)
                .createdManually(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .guest(guest)
                .rooms(Set.of(room101, room102))
                .build();
        bookingRepository.save(booking);
        return List.of(booking.getId(), room101.getId(), room102.getId());
    }

    protected Room createAndSaveRoom(String roomNumber, Integer floor, RoomType roomType) {
        Room room = Room.builder()
                .roomNumber(roomNumber)
                .floor(floor)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                        new Point(0.0, 100.0)))
                .roomType(roomType)
                .active(true)
                .build();
        return roomRepository.save(room);
    }

}
