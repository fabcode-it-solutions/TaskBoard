package tuwien.taskinn.unittest.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tuwien.taskinn.dto.response.BookingStatisticsResponseDto;
import tuwien.taskinn.dto.response.CleaningStatisticsResponseDto;
import tuwien.taskinn.dto.response.EmployeeStatisticsResponseDto;
import tuwien.taskinn.dto.response.MinibarStatisticsResponseDto;
import tuwien.taskinn.enums.TaskType;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.SchedulingRole;
import tuwien.taskinn.entity.InventoryItem;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.CleaningTaskRepository;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.MinibarConsumptionRepository;
import tuwien.taskinn.repo.InventoryItemRepository;
import tuwien.taskinn.repo.SchedulingRoleRepository;
import tuwien.taskinn.services.impl.StatisticsServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("Statistics Service Unit Tests")
public class StatisticsServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CleaningTaskRepository cleaningTaskRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private MinibarConsumptionRepository minibarConsumptionRepository;
    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private SchedulingRoleRepository schedulingRoleRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @Test
    @DisplayName("getMinibarStatistics throws exception when only periodFrom is provided")
    void getMinibarStatistics_throwsWhenOnlyPeriodFromProvided() {
        // Given
        LocalDate from = LocalDate.now();

        // When
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> statisticsService.getMinibarStatistics(from, null, null));

        // Then
        assertAll(() -> assertNotNull(ex),
                () -> assertTrue(ex.getMessage().contains("Both periodFrom and periodTo must be provided together")));
    }

    @Test
    @DisplayName("getMinibarStatistics throws exception when only periodTo is provided")
    void getMinibarStatistics_throwsWhenOnlyPeriodToProvided() {
        // Given
        LocalDate to = LocalDate.now();

        // When
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> statisticsService.getMinibarStatistics(null, to, null));

        // Then
        assertAll(() -> assertNotNull(ex),
                () -> assertTrue(ex.getMessage().contains("Both periodFrom and periodTo must be provided together")));
    }

    @Test
    @DisplayName("getMinibarStatistics throws exception when periodFrom is after periodTo")
    void getMinibarStatistics_throwsWhenPeriodFromAfterPeriodTo() {
        // Given
        LocalDate from = LocalDate.now();
        LocalDate to = from.minusDays(1);

        // When
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> statisticsService.getMinibarStatistics(from, to, null));

        // Then
        assertAll(() -> assertNotNull(ex),
                () -> assertTrue(ex.getMessage().contains("periodFrom must be on or before periodTo")));
    }

    @Test
    @DisplayName("getMinibarStatistics correctly calculates aggregated stats (Pie Charts)")
    void getMinibarStatistics_calculatesAggregatedStatsCorrectly() {
        // Given
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        // Mock Aggregated Data: [ItemName, TotalQty, TotalRevenue]
        List<Object[]> aggregatedMock = new ArrayList<>();
        aggregatedMock.add(new Object[] {"Cola", 10L, new BigDecimal("35.50")});
        aggregatedMock.add(new Object[] {"Chips", 5L, new BigDecimal("12.00")});

        when(minibarConsumptionRepository.findAggregatedStats(any(Instant.class), any(Instant.class)))
                .thenReturn(aggregatedMock);

        // Mock Default Item to avoid NPE in second part of method
        when(inventoryItemRepository.findTopByOrderByNameAsc()).thenReturn(Optional.empty());

        // When
        MinibarStatisticsResponseDto result = statisticsService.getMinibarStatistics(from, to, null);

        // Then
        assertAll(() -> assertNotNull(result),
                // Verify Sales By Quantity
                () -> assertEquals(10L, result.getSalesByQuantity().get("Cola")),
                () -> assertEquals(5L, result.getSalesByQuantity().get("Chips")),
                // Verify Sales By Earnings
                () -> assertEquals(35.50, result.getSalesByEarnings().get("Cola")),
                () -> assertEquals(12.00, result.getSalesByEarnings().get("Chips")),
                // Verify Date Conversion (Start of day / End of day+1)
                () -> verify(minibarConsumptionRepository).findAggregatedStats(any(Instant.class), any(Instant.class)));
    }

    @Test
    @DisplayName("getMinibarStatistics uses default item when no IDs provided")
    void getMinibarStatistics_usesDefaultItemWhenNoIdsProvided() {
        // Given
        InventoryItem mockItem = new InventoryItem();
        mockItem.setId(99L);
        mockItem.setName("Apple Juice");

        when(minibarConsumptionRepository.findAggregatedStats(any(), any())).thenReturn(Collections.emptyList());
        when(inventoryItemRepository.findTopByOrderByNameAsc()).thenReturn(Optional.of(mockItem));
        when(minibarConsumptionRepository.findConsumptionsForItems(eq(List.of(99L)), any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        MinibarStatisticsResponseDto result = statisticsService.getMinibarStatistics(null, null, null);

        // Then
        assertAll(() -> assertTrue(result.getDailyItemSales().containsKey(99L)),
                () -> verify(inventoryItemRepository).findTopByOrderByNameAsc());
    }

    @Test
    @DisplayName("getMinibarStatistics uses provided IDs and fills missing data correctly")
    void getMinibarStatistics_usesProvidedIdsAndFillsMissingData() {
        // Given
        List<Long> requestedIds = List.of(10L, 20L); // 10L has data, 20L has no data
        LocalDate today = LocalDate.of(2025, 1, 15);
        Instant todayInstant = today.atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Mock Aggregated (ignored for this test focus)
        when(minibarConsumptionRepository.findAggregatedStats(any(), any())).thenReturn(Collections.emptyList());

        // Mock Daily Data: [ItemId, Instant, Quantity]
        List<Object[]> dailyMock = new ArrayList<>();
        // Item 10 sold 2 units today
        dailyMock.add(new Object[] {10L, todayInstant, 2});
        // Item 10 sold 3 units today (same day aggregation test)
        dailyMock.add(new Object[] {10L, todayInstant, 3});

        when(minibarConsumptionRepository.findConsumptionsForItems(eq(requestedIds), any(), any()))
                .thenReturn(dailyMock);

        // When
        MinibarStatisticsResponseDto result = statisticsService.getMinibarStatistics(null, null, requestedIds);
        Map<Long, Map<String, Long>> dailySales = result.getDailyItemSales();

        // Then
        assertAll(
                // Check Item 10 (Should sum 2+3 = 5)
                () -> assertTrue(dailySales.containsKey(10L)),
                () -> assertEquals(5L, dailySales.get(10L).get(today.toString())),
                // Check Item 20 (Should exist but be empty/null safely)
                () -> assertTrue(dailySales.containsKey(20L)), () -> assertNotNull(dailySales.get(20L)));
    }

    @Test
    @DisplayName("getMinibarStatistics handles empty database gracefully")
    void getMinibarStatistics_handlesEmptyDatabase() {
        // Given
        when(minibarConsumptionRepository.findAggregatedStats(any(), any())).thenReturn(Collections.emptyList());
        when(inventoryItemRepository.findTopByOrderByNameAsc()).thenReturn(Optional.empty());

        // When
        MinibarStatisticsResponseDto result = statisticsService.getMinibarStatistics(null, null, null);

        // Then
        assertAll(() -> assertNotNull(result), () -> assertTrue(result.getSalesByQuantity().isEmpty()),
                () -> assertTrue(result.getSalesByEarnings().isEmpty()),
                () -> assertTrue(result.getDailyItemSales().isEmpty()));
    }

    @Test
    @DisplayName("getEmployeeStatistics should throw BusinessValidationException when only periodFrom is provided")
    void getEmployeeStatistics_whenOnlyFromProvided_throws() {
        LocalDate from = LocalDate.of(2025, 1, 1);

        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> statisticsService.getEmployeeStatistics(from, null, null));

        assertAll(() -> assertNotNull(ex), () -> assertTrue(ex.getMessage().contains("provided together")));
    }

    @Test
    @DisplayName("getEmployeeStatistics should throw BusinessValidationException when only periodTo is provided")
    void getEmployeeStatistics_whenOnlyToProvided_throws() {
        LocalDate to = LocalDate.of(2025, 12, 31);

        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> statisticsService.getEmployeeStatistics(null, to, null));

        assertAll(() -> assertNotNull(ex), () -> assertTrue(ex.getMessage().contains("provided together")));
    }

    @Test
    @DisplayName("getEmployeeStatistics should throw BusinessValidationException when periodFrom is after periodTo")
    void getEmployeeStatistics_whenFromAfterTo_throws() {
        LocalDate from = LocalDate.of(2025, 12, 31);
        LocalDate to = LocalDate.of(2025, 1, 1);

        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> statisticsService.getEmployeeStatistics(from, to, null));

        assertAll(() -> assertNotNull(ex), () -> assertTrue(ex.getMessage().toLowerCase().contains("on or before")));
    }

    @Test
    @DisplayName("getEmployeeStatistics defaults to first alphabetical employee when no IDs provided")
    void getEmployeeStatistics_defaultsToFirstEmployee() {
        // Given
        // 1. Setup Data
        SchedulingRole role = SchedulingRole.builder().id(99L).roleName("Housekeeping").build();
        Employee emp1 = Employee.builder().id(101L).lastName("Alpha").firstName("John").build(); // First alphabetically
        Employee emp2 = Employee.builder().id(102L).lastName("Beta").firstName("Jane").build();

        // 2. Mock Repositories
        when(schedulingRoleRepository.findByRoleName("Housekeeping")).thenReturn(Optional.of(role));
        when(employeeRepository.findAllEmployeesWithSchedulingRole(99L)).thenReturn(List.of(emp1, emp2));

        // 3. Mock cleaning tasks for emp1 (the expected default target)
        LocalDateTime now = LocalDateTime.now();
        // Object[] row structure: {EmployeeID, TaskType, RoomType, Start, End}
        Object[] row = {101L, TaskType.CHECKOUT_CLEAN, "SINGLE", now.minusMinutes(30), now};
        when(cleaningTaskRepository.findCompletedTasksForEmployees(List.of(101L))).thenReturn(List.<Object[]>of(row));

        // When
        List<EmployeeStatisticsResponseDto> result = statisticsService.getEmployeeStatistics(null, null, null);

        // Then
        assertAll(() -> assertNotNull(result), () -> assertEquals(1, result.size()),
                () -> assertEquals(101L, result.get(0).getEmployeeId()),
                () -> assertEquals(30.0,
                        result.get(0).getAvgDurationByTaskAndRoomType().get("CHECKOUT_CLEAN").get("SINGLE")),
                () -> verify(schedulingRoleRepository).findByRoleName("Housekeeping"),
                () -> verify(employeeRepository).findAllEmployeesWithSchedulingRole(99L));
    }

    @Test
    @DisplayName("getEmployeeStatistics returns empty list if Housekeeping role exists but has no employees")
    void getEmployeeStatistics_returnsEmptyIfNoEmployees() {
        // Given
        // 1. Mock the role lookup to return a valid role
        SchedulingRole role = SchedulingRole.builder().id(1L).roleName("Housekeeping").build();
        when(schedulingRoleRepository.findByRoleName("Housekeeping")).thenReturn(Optional.of(role));

        // 2. Mock the employee lookup to return an empty list
        when(employeeRepository.findAllEmployeesWithSchedulingRole(1L)).thenReturn(Collections.emptyList());

        // When
        List<EmployeeStatisticsResponseDto> result = statisticsService.getEmployeeStatistics(null, null, null);

        // Then
        assertAll(() -> assertTrue(result.isEmpty()),
                () -> verify(cleaningTaskRepository, never()).findCompletedTasksForEmployees(any()));
    }

    @Test
    @DisplayName("getEmployeeStatistics fetches specific employees and aggregates data correctly")
    void getEmployeeStatistics_fetchesSpecificEmployees() {
        // Given
        List<Long> requestedIds = List.of(200L, 201L);
        LocalDateTime now = LocalDateTime.now();

        // Data:
        // Emp 200: CHECKOUT_CLEAN SINGLE 30min
        // Emp 200: CHECKOUT_CLEAN SINGLE 60min -> Avg 45
        // Emp 201: DEEP_CLEAN SUITE 120min
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[] {200L, TaskType.CHECKOUT_CLEAN, "SINGLE", now.minusMinutes(30), now});
        mockRows.add(new Object[] {200L, TaskType.CHECKOUT_CLEAN, "SINGLE", now.minusMinutes(60), now});
        mockRows.add(new Object[] {201L, TaskType.DEEP_CLEAN, "SUITE", now.minusMinutes(120), now});

        when(cleaningTaskRepository.findCompletedTasksForEmployees(requestedIds)).thenReturn(mockRows);

        // When
        List<EmployeeStatisticsResponseDto> result = statisticsService.getEmployeeStatistics(null, null, requestedIds);
        EmployeeStatisticsResponseDto dto200 =
                result.stream().filter(r -> r.getEmployeeId().equals(200L)).findFirst().orElseThrow();
        EmployeeStatisticsResponseDto dto201 =
                result.stream().filter(r -> r.getEmployeeId().equals(201L)).findFirst().orElseThrow();

        // Then
        assertAll(() -> assertEquals(2, result.size()),
                // Verify Emp 200
                () -> assertEquals(45.0, dto200.getAvgDurationByTaskAndRoomType().get("CHECKOUT_CLEAN").get("SINGLE")),
                // Verify Emp 201
                () -> assertEquals(120.0, dto201.getAvgDurationByTaskAndRoomType().get("DEEP_CLEAN").get("SUITE")));
    }

    @Test
    @DisplayName("getEmployeeStatistics uses date range query when dates are provided")
    void getEmployeeStatistics_usesDateRangeQuery() {
        // Given
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);
        List<Long> ids = List.of(99L);

        when(cleaningTaskRepository.findCompletedTasksForEmployeesInRange(eq(ids), any(), any()))
                .thenReturn(Collections.emptyList());

        // When
        List<EmployeeStatisticsResponseDto> result = statisticsService.getEmployeeStatistics(from, to, ids);

        // Then
        assertAll(() -> assertNotNull(result),
                () -> verify(cleaningTaskRepository).findCompletedTasksForEmployeesInRange(eq(ids),
                        any(LocalDateTime.class), any(LocalDateTime.class)),
                () -> verify(cleaningTaskRepository, never()).findCompletedTasksForEmployees(any()));
    }

    @Test
    @DisplayName("getEmployeeStatistics returns DTO with empty map for requested employee with no data")
    void getEmployeeStatistics_returnsEmptyDtoForNoData() {
        // Given
        List<Long> ids = List.of(300L);
        when(cleaningTaskRepository.findCompletedTasksForEmployees(ids)).thenReturn(Collections.emptyList());

        // When
        List<EmployeeStatisticsResponseDto> result = statisticsService.getEmployeeStatistics(null, null, ids);

        // Then
        assertAll(() -> assertEquals(1, result.size()), () -> assertEquals(300L, result.get(0).getEmployeeId()),
                () -> assertTrue(result.get(0).getAvgDurationByTaskAndRoomType().isEmpty()));
    }

    @Test
    @DisplayName("getCleaningStatistics should throw BusinessValidationException when only periodFrom is provided")
    void getCleaningStatistics_whenOnlyFromProvided_throws() {
        // When
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> statisticsService.getCleaningStatistics(LocalDate.of(2025, 1, 1), null));

        // Then
        assertAll(() -> assertNotNull(ex),
                () -> assertTrue(
                        ex.getMessage().contains("periodFrom") || ex.getMessage().contains("provided together")),
                () -> verifyNoInteractions(cleaningTaskRepository));
    }

    @Test
    @DisplayName("getCleaningStatistics should throw BusinessValidationException when only periodTo is provided")
    void getCleaningStatistics_whenOnlyToProvided_throws() {
        // When
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> statisticsService.getCleaningStatistics(null, LocalDate.of(2025, 12, 31)));

        // Then
        assertAll(() -> assertNotNull(ex),
                () -> assertTrue(
                        ex.getMessage().contains("periodFrom") || ex.getMessage().contains("provided together")),
                () -> verifyNoInteractions(cleaningTaskRepository));
    }

    @Test
    @DisplayName("getCleaningStatistics should throw BusinessValidationException when periodFrom is after periodTo")
    void getCleaningStatistics_whenFromAfterTo_throws() {
        // Given
        LocalDate from = LocalDate.of(2025, 12, 31);
        LocalDate to = LocalDate.of(2025, 1, 1);

        // When
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> statisticsService.getCleaningStatistics(from, to));

        // Then
        assertAll(() -> assertNotNull(ex), () -> assertTrue(ex.getMessage().toLowerCase().contains("on or before")),
                () -> verifyNoInteractions(cleaningTaskRepository));
    }

    @Test
    @DisplayName("getCleaningStatistics with no period returns full history statistics")
    void getCleaningStatistics_noPeriod_returnsFullHistory() {
        // Given
        Object[] task1 = mockTaskRow("CHECKOUT_CLEAN", "SINGLE", LocalDateTime.of(2025, 1, 1, 10, 0),
                LocalDateTime.of(2025, 1, 1, 10, 32));
        Object[] task2 = mockTaskRow("CHECKOUT_CLEAN", "DOUBLE", LocalDateTime.of(2025, 1, 2, 10, 0),
                LocalDateTime.of(2025, 1, 2, 10, 48));
        List<Object[]> avgRaw = List.of(task1, task2);

        Object[] hour1 = mockHoursRow(LocalDateTime.of(2025, 1, 1, 10, 0), LocalDateTime.of(2025, 1, 1, 11, 0));
        List<Object[]> hoursRaw = List.<Object[]>of(hour1);

        when(cleaningTaskRepository.findCompletedTasksWithTimestamps()).thenReturn(avgRaw);
        when(cleaningTaskRepository.findAllCompletedTasksWithTimestamps()).thenReturn(hoursRaw);
        when(cleaningTaskRepository.countBlockedTasks()).thenReturn(1L);
        when(cleaningTaskRepository.countAllTasks()).thenReturn(10L);

        // When
        CleaningStatisticsResponseDto dto = statisticsService.getCleaningStatistics(null, null);

        // Then
        assertAll(() -> assertNotNull(dto),
                () -> assertEquals(2, dto.getAvgDurationByTaskAndRoomType().get("CHECKOUT_CLEAN").size()),
                () -> assertEquals(32.0, dto.getAvgDurationByTaskAndRoomType().get("CHECKOUT_CLEAN").get("SINGLE")),
                () -> assertEquals(48.0, dto.getAvgDurationByTaskAndRoomType().get("CHECKOUT_CLEAN").get("DOUBLE")),
                () -> assertEquals(1.0, dto.getAvgHoursByMonth().get("01")),
                () -> assertEquals(10.0, dto.getBlockedTasksPercentage(), 0.001),
                () -> verify(cleaningTaskRepository).findCompletedTasksWithTimestamps(),
                () -> verify(cleaningTaskRepository).findAllCompletedTasksWithTimestamps(),
                () -> verify(cleaningTaskRepository).countBlockedTasks(),
                () -> verify(cleaningTaskRepository).countAllTasks());
    }

    @Test
    @DisplayName("getCleaningStatistics with period filter uses range queries")
    void getCleaningStatistics_withPeriod_usesRangeQueries() {
        // Given
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        when(cleaningTaskRepository.findCompletedTasksWithTimestampsInRange(fromDt, toDt)).thenReturn(List.of());
        when(cleaningTaskRepository.findCompletedTasksWithTimestampsInRangeForHours(fromDt, toDt))
                .thenReturn(List.of());
        when(cleaningTaskRepository.countBlockedTasksInRange(fromDt, toDt)).thenReturn(0L);
        when(cleaningTaskRepository.countAllTasksInRange(fromDt, toDt)).thenReturn(5L);

        // When
        CleaningStatisticsResponseDto dto = statisticsService.getCleaningStatistics(from, to);

        // Then
        assertAll(() -> assertNotNull(dto), () -> assertTrue(dto.getAvgDurationByTaskAndRoomType().isEmpty()),
                () -> assertEquals(12, dto.getAvgHoursByMonth().size()),
                () -> assertEquals(0.0, dto.getBlockedTasksPercentage()),
                () -> verify(cleaningTaskRepository).findCompletedTasksWithTimestampsInRange(fromDt, toDt),
                () -> verify(cleaningTaskRepository).findCompletedTasksWithTimestampsInRangeForHours(fromDt, toDt),
                () -> verify(cleaningTaskRepository).countBlockedTasksInRange(fromDt, toDt),
                () -> verify(cleaningTaskRepository).countAllTasksInRange(fromDt, toDt),
                () -> verify(cleaningTaskRepository, never()).findCompletedTasksWithTimestamps());
    }

    @Test
    @DisplayName("getCleaningStatistics returns avgHoursByMonth with all 12 months defaulted to 0")
    void getCleaningStatistics_returnsAll12MonthsWithDefaults() {
        // Given - empty data
        when(cleaningTaskRepository.findAllCompletedTasksWithTimestamps()).thenReturn(List.of());
        when(cleaningTaskRepository.findCompletedTasksWithTimestamps()).thenReturn(List.of());
        when(cleaningTaskRepository.countBlockedTasks()).thenReturn(0L);
        when(cleaningTaskRepository.countAllTasks()).thenReturn(0L);

        // When
        CleaningStatisticsResponseDto dto = statisticsService.getCleaningStatistics(null, null);

        // Then
        assertAll(() -> assertEquals(12, dto.getAvgHoursByMonth().size()),
                () -> assertEquals(0.0, dto.getAvgHoursByMonth().get("01")),
                () -> assertEquals(0.0, dto.getAvgHoursByMonth().get("06")),
                () -> assertEquals(0.0, dto.getAvgHoursByMonth().get("12")));
    }

    @Test
    @DisplayName("getBookingStatistics should throw BusinessValidationException when only periodFrom is provided")
    void getBookingStatistics_whenOnlyFromProvided_throws() {
        // Given
        LocalDate from = LocalDate.of(2025, 1, 1);

        // When
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> statisticsService.getBookingStatistics(from, null, null));

        // Then
        assertAll(() -> assertNotNull(ex),
                () -> assertTrue(
                        ex.getMessage().contains("periodFrom") || ex.getMessage().contains("provided together")),
                () -> verifyNoInteractions(bookingRepository));
    }

    @Test
    @DisplayName("getBookingStatistics should throw BusinessValidationException when only periodTo is provided")
    void getBookingStatistics_whenOnlyToProvided_throws() {
        // Given
        LocalDate to = LocalDate.of(2025, 12, 31);

        // When
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> statisticsService.getBookingStatistics(null, to, null));

        // Then
        assertAll(() -> assertNotNull(ex),
                () -> assertTrue(
                        ex.getMessage().contains("periodFrom") || ex.getMessage().contains("provided together")),
                () -> verifyNoInteractions(bookingRepository));
    }

    @Test
    @DisplayName("getBookingStatistics should throw BusinessValidationException when periodFrom is after periodTo")
    void getBookingStatistics_whenFromAfterTo_throws() {
        // Given
        LocalDate from = LocalDate.of(2025, 12, 31);
        LocalDate to = LocalDate.of(2025, 1, 1);

        // When
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> statisticsService.getBookingStatistics(from, to, null));

        // Then
        assertAll(() -> assertNotNull(ex), () -> assertTrue(ex.getMessage().toLowerCase().contains("on or before")),
                () -> verifyNoInteractions(bookingRepository));
    }

    @Test
    @DisplayName("getBookingStatistics should use selected month when month param is provided")
    void getBookingStatistics_whenMonthProvided_usesThatMonthRange() {
        // Given
        YearMonth month = YearMonth.of(2025, 3);
        LocalDate expectedMonthFrom = month.atDay(1);
        LocalDate expectedMonthTo = expectedMonthFrom.with(TemporalAdjusters.lastDayOfMonth());

        when(bookingRepository.findAllBookingsCountByCheckInDay())
                .thenReturn(List.<Object[]>of(new Object[] {LocalDate.of(2025, 1, 10), 2L}));

        when(bookingRepository.findBookingsCountByCheckInDayInRange(expectedMonthFrom, expectedMonthTo))
                .thenReturn(List.<Object[]>of(new Object[] {LocalDate.of(2025, 3, 5), 1L}));

        when(bookingRepository.countAllBookingsByCheckInMonth())
                .thenReturn(List.<Object[]>of(new Object[] {2025, 3, 7L}));

        when(bookingRepository.countAllBookingsByRoomType()).thenReturn(List.<Object[]>of(new Object[] {"Deluxe", 4L}));

        // When
        BookingStatisticsResponseDto dto = statisticsService.getBookingStatistics(null, null, month);

        // Then
        assertAll(() -> assertNotNull(dto), () -> assertEquals(2L, dto.getAllBookingsByDay().get("2025-01-10")),
                () -> assertEquals(1L, dto.getMonthBookingsByDay().get("2025-03-05")),
                () -> assertEquals(7L, dto.getBookingsByMonth().get("03")),
                () -> assertEquals(4L, dto.getBookingsByRoomType().get("Deluxe")),
                () -> verify(bookingRepository, times(1)).findAllBookingsCountByCheckInDay(),
                () -> verify(bookingRepository, times(1)).findBookingsCountByCheckInDayInRange(expectedMonthFrom,
                        expectedMonthTo),
                () -> verify(bookingRepository, times(1)).countAllBookingsByCheckInMonth(),
                () -> verify(bookingRepository, times(1)).countAllBookingsByRoomType(),
                () -> verify(bookingRepository, never()).countBookingsByCheckInMonthInRange(any(), any()),
                () -> verify(bookingRepository, never()).countBookingsByRoomTypeInRange(any(), any()));
    }

    @Test
    @DisplayName("getBookingStatistics should use period filter for bar charts when periodFrom and periodTo are provided")
    void getBookingStatistics_whenPeriodProvided_usesRangeQueriesForBars() {
        // Given
        LocalDate periodFrom = LocalDate.of(2025, 1, 1);
        LocalDate periodTo = LocalDate.of(2025, 12, 31);

        when(bookingRepository.findAllBookingsCountByCheckInDay())
                .thenReturn(List.<Object[]>of(new Object[] {LocalDate.of(2024, 1, 1), 10L}));

        // Month line: just return empty to keep this test focused
        YearMonth month = YearMonth.of(2025, 1);
        LocalDate monthFrom = month.atDay(1);
        LocalDate monthTo = monthFrom.with(TemporalAdjusters.lastDayOfMonth());
        when(bookingRepository.findBookingsCountByCheckInDayInRange(monthFrom, monthTo)).thenReturn(List.of());

        when(bookingRepository.countBookingsByCheckInMonthInRange(periodFrom, periodTo))
                .thenReturn(List.of(new Object[] {2025, 1, 3L}, new Object[] {2025, 2, 5L}));

        when(bookingRepository.countBookingsByRoomTypeInRange(periodFrom, periodTo))
                .thenReturn(List.of(new Object[] {"Standard", 6L}, new Object[] {"Suite", 2L}));

        // When
        BookingStatisticsResponseDto dto = statisticsService.getBookingStatistics(periodFrom, periodTo, month);

        // Then
        assertAll(() -> assertNotNull(dto), () -> assertEquals(10L, dto.getAllBookingsByDay().get("2024-01-01")),
                () -> assertEquals(3L, dto.getBookingsByMonth().get("01")),
                () -> assertEquals(5L, dto.getBookingsByMonth().get("02")),
                () -> assertEquals(6L, dto.getBookingsByRoomType().get("Standard")),
                () -> assertEquals(2L, dto.getBookingsByRoomType().get("Suite")),
                () -> verify(bookingRepository, times(1)).countBookingsByCheckInMonthInRange(periodFrom, periodTo),
                () -> verify(bookingRepository, times(1)).countBookingsByRoomTypeInRange(periodFrom, periodTo),
                () -> verify(bookingRepository, never()).countAllBookingsByCheckInMonth(),
                () -> verify(bookingRepository, never()).countAllBookingsByRoomType());
    }

    @Test
    @DisplayName("getBookingStatistics should always return all 12 months in bookingsByMonth with zeros for missing months")
    void getBookingStatistics_alwaysReturnsAllMonthsWithDefaults() {
        // Given
        when(bookingRepository.findAllBookingsCountByCheckInDay()).thenReturn(List.of());
        when(bookingRepository.findBookingsCountByCheckInDayInRange(any(), any())).thenReturn(List.of());

        // Only one month present in DB result
        when(bookingRepository.countAllBookingsByCheckInMonth())
                .thenReturn(List.<Object[]>of(new Object[] {2025, 5, 9L}));

        when(bookingRepository.countAllBookingsByRoomType()).thenReturn(List.of());

        // When
        BookingStatisticsResponseDto dto = statisticsService.getBookingStatistics(null, null, YearMonth.of(2025, 5));

        // Then
        assertAll(() -> assertNotNull(dto), () -> assertEquals(12, dto.getBookingsByMonth().size()),
                () -> assertEquals(0L, dto.getBookingsByMonth().get("01")),
                () -> assertEquals(9L, dto.getBookingsByMonth().get("05")),
                () -> assertEquals(0L, dto.getBookingsByMonth().get("12")));
    }

    private Object[] mockTaskRow(String taskTypeStr, String roomType, LocalDateTime start, LocalDateTime end) {
        return new Object[] {TaskType.valueOf(taskTypeStr), roomType, start, end};
    }

    private Object[] mockHoursRow(LocalDateTime start, LocalDateTime end) {
        return new Object[] {start, end};
    }
}
