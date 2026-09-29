package tuwien.taskinn.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tuwien.taskinn.dto.response.BookingStatisticsResponseDto;
import tuwien.taskinn.dto.response.CleaningStatisticsResponseDto;
import tuwien.taskinn.dto.response.MinibarStatisticsResponseDto;
import tuwien.taskinn.dto.response.EmployeeStatisticsResponseDto;
import tuwien.taskinn.repo.*;
import tuwien.taskinn.enums.TaskType;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.SchedulingRole;
import tuwien.taskinn.services.StatisticsService;
import tuwien.taskinn.exceptions.BusinessValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class StatisticsServiceImpl implements StatisticsService {

    private final BookingRepository bookingRepository;
    private final CleaningTaskRepository cleaningTaskRepository;
    private final EmployeeRepository employeeRepository;
    private final MinibarConsumptionRepository minibarConsumptionRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SchedulingRoleRepository schedulingRoleRepository;

    @Override
    public MinibarStatisticsResponseDto getMinibarStatistics(LocalDate periodFrom, LocalDate periodTo,
            List<Long> itemIds) {
        // Validation
        if ((periodFrom != null) ^ (periodTo != null)) {
            throw new BusinessValidationException("minibar statistics",
                    "Both periodFrom and periodTo must be provided together.");
        }
        if (periodFrom != null && periodFrom.isAfter(periodTo)) {
            throw new BusinessValidationException("minibar statistics", "periodFrom must be on or before periodTo.");
        }

        // Convert LocalDate to Instant for DB queries
        // Using system default zone for conversion
        Instant start = periodFrom != null ? periodFrom.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant end = periodTo != null ? periodTo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;

        // Calculate Pie Charts (Aggregated Data)
        List<Object[]> aggregatedStats = minibarConsumptionRepository.findAggregatedStats(start, end);

        Map<String, Long> salesByQuantity = new HashMap<>();
        Map<String, Double> salesByEarnings = new HashMap<>();

        for (Object[] row : aggregatedStats) {
            String itemName = (String) row[0];
            Long totalQty = (Long) row[1];
            BigDecimal totalRevenue = (BigDecimal) row[2];

            salesByQuantity.put(itemName, totalQty);
            salesByEarnings.put(itemName, totalRevenue.doubleValue());
        }

        // Calculate Line Charts (Daily Sales)
        List<Long> targetItemIds;

        if (itemIds != null && !itemIds.isEmpty()) {
            targetItemIds = itemIds;
        } else {
            // Default to first alphabetical item
            targetItemIds = inventoryItemRepository.findTopByOrderByNameAsc()
                    .map(item -> List.of(item.getId()))
                    .orElse(Collections.emptyList());
        }

        Map<Long, Map<String, Long>> dailyItemSales = new HashMap<>();

        if (!targetItemIds.isEmpty()) {
            List<Object[]> rawConsumption =
                    minibarConsumptionRepository.findConsumptionsForItems(targetItemIds, start, end);

            // Group by Item ID -> Date String -> Sum Quantity
            dailyItemSales = rawConsumption.stream()
                    .collect(Collectors.groupingBy(row -> (Long) row[0], // Item ID
                            Collectors.groupingBy(
                                    row -> ((Instant) row[1]).atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                                    Collectors.summingLong(row -> ((Integer) row[2]).longValue()))));

            // Ensure queried IDs exist in the map even if they have 0 sales
            for (Long id : targetItemIds) {
                dailyItemSales.putIfAbsent(id, new HashMap<>());
            }
        }

        return MinibarStatisticsResponseDto.builder()
                .salesByEarnings(salesByEarnings)
                .salesByQuantity(salesByQuantity)
                .dailyItemSales(dailyItemSales)
                .build();
    }

    @Override
    public List<EmployeeStatisticsResponseDto> getEmployeeStatistics(LocalDate periodFrom, LocalDate periodTo,
            List<Long> employeeIds) {
        // Validation
        if ((periodFrom != null) ^ (periodTo != null)) {
            throw new BusinessValidationException("employee statistics",
                    "Both periodFrom and periodTo must be provided together.");
        }
        if (periodFrom != null && periodFrom.isAfter(periodTo)) {
            throw new BusinessValidationException("employee statistics", "periodFrom must be on or before periodTo.");
        }

        LocalDateTime from = periodFrom != null ? periodFrom.atStartOfDay() : null;
        LocalDateTime to = periodTo != null ? periodTo.plusDays(1).atStartOfDay() : null;

        // Handle Default Employee Logic
        List<Long> targetIds;
        if (employeeIds == null || employeeIds.isEmpty()) {
            // Find "Housekeeping" role
            Optional<SchedulingRole> housekeepingRole = schedulingRoleRepository.findByRoleName("Housekeeping");

            if (housekeepingRole.isPresent()) {
                // Find all employees with this role, sorted alphabetically (as defined in repo query)
                List<Employee> housekeepers =
                        employeeRepository.findAllEmployeesWithSchedulingRole(housekeepingRole.get().getId());

                if (!housekeepers.isEmpty()) {
                    // Pick the first one
                    targetIds = List.of(housekeepers.get(0).getId());
                } else {
                    return Collections.emptyList();
                }
            } else {
                return Collections.emptyList();
            }
        } else {
            targetIds = employeeIds;
        }

        List<Object[]> rawData =
                (from != null) ? cleaningTaskRepository.findCompletedTasksForEmployeesInRange(targetIds, from, to)
                        : cleaningTaskRepository.findCompletedTasksForEmployees(targetIds);

        // Aggregate Data: Group by Employee -> TaskType -> RoomType -> Avg Duration
        Map<Long, Map<String, Map<String, Double>>> groupedStats = rawData.stream()
                .collect(Collectors.groupingBy(row -> (Long) row[0], // Employee ID
                        Collectors.groupingBy(row -> ((TaskType) row[1]).name(), // Task Type
                                Collectors.groupingBy(row -> (String) row[2], // Room Type Name
                                        Collectors.averagingLong(row -> {
                                            LocalDateTime start = (LocalDateTime) row[3];
                                            LocalDateTime end = (LocalDateTime) row[4];
                                            return Duration.between(start, end).toMinutes();
                                        })))));

        // Map to DTOs
        List<EmployeeStatisticsResponseDto> result = new ArrayList<>();

        for (Long id : targetIds) {
            Map<String, Map<String, Double>> stats = groupedStats.getOrDefault(id, new HashMap<>());
            result.add(
                    EmployeeStatisticsResponseDto.builder().employeeId(id).avgDurationByTaskAndRoomType(stats).build());
        }

        return result;
    }

    @Override
    public CleaningStatisticsResponseDto getCleaningStatistics(LocalDate periodFrom, LocalDate periodTo) {
        // Validation
        if ((periodFrom != null) ^ (periodTo != null)) {
            throw new BusinessValidationException("cleaning statistics",
                    "Both periodFrom and periodTo must be provided together.");
        }
        if (periodFrom != null && periodFrom.isAfter(periodTo)) {
            throw new BusinessValidationException("cleaning statistics", "periodFrom must be on or before periodTo.");
        }

        LocalDateTime from = periodFrom != null ? periodFrom.atStartOfDay() : null;
        LocalDateTime to = periodTo != null ? periodTo.plusDays(1).atStartOfDay() : null;

        // AVG DURATION BY TASK AND ROOM TYPE
        List<Object[]> avgRaw = from != null ? cleaningTaskRepository.findCompletedTasksWithTimestampsInRange(from, to)
                : cleaningTaskRepository.findCompletedTasksWithTimestamps();

        Map<String, Map<String, Double>> avgByTaskRoom = avgRaw.stream()
                .collect(Collectors.groupingBy(row -> ((TaskType) row[0]).name(),
                        Collectors.groupingBy(row -> (String) row[1], Collectors.averagingLong(row -> {
                            LocalDateTime start = (LocalDateTime) row[2];
                            LocalDateTime end = (LocalDateTime) row[3];
                            return Duration.between(start, end).toMinutes();
                        }))));

        // TOTAL HOURS BY MONTH
        List<Object[]> hoursRaw =
                from != null ? cleaningTaskRepository.findCompletedTasksWithTimestampsInRangeForHours(from, to)
                        : cleaningTaskRepository.findAllCompletedTasksWithTimestamps();

        Map<YearMonth, Double> hoursPerYearMonth = hoursRaw.stream()
                .collect(Collectors.groupingBy(row -> YearMonth.from((LocalDateTime) row[0]),
                        Collectors.summingDouble(row -> {
                            LocalDateTime start = (LocalDateTime) row[0];
                            LocalDateTime end = (LocalDateTime) row[1];
                            return Duration.between(start, end).toMinutes() / 60.0;
                        })));

        Map<String, Double> avgHoursByMonth = hoursPerYearMonth.entrySet()
                .stream()
                .collect(Collectors.groupingBy(e -> String.format("%02d", e.getKey().getMonthValue()),
                        Collectors.averagingDouble(Map.Entry::getValue)));

        for (int m = 1; m <= 12; m++) {
            avgHoursByMonth.putIfAbsent(String.format("%02d", m), 0.0);
        }

        // BLOCKED TASKS PERCENTAGE
        Long blockedCount = from != null ? cleaningTaskRepository.countBlockedTasksInRange(from, to)
                : cleaningTaskRepository.countBlockedTasks();
        Long totalCount = from != null ? cleaningTaskRepository.countAllTasksInRange(from, to)
                : cleaningTaskRepository.countAllTasks();
        Double blockedPct = totalCount > 0 ? (blockedCount.doubleValue() * 100.0) / totalCount : 0.0;

        return CleaningStatisticsResponseDto.builder()
                .avgDurationByTaskAndRoomType(avgByTaskRoom)
                .avgHoursByMonth(avgHoursByMonth)
                .blockedTasksPercentage(blockedPct)
                .build();
    }

    @Override
    public BookingStatisticsResponseDto getBookingStatistics(LocalDate periodFrom, LocalDate periodTo,
            YearMonth month) {

        // Validation (bar charts period filter)
        final boolean fromProvided = periodFrom != null;
        final boolean toProvided = periodTo != null;

        // Enforce "both or none"
        if (fromProvided ^ toProvided) {
            throw new BusinessValidationException("booking statistics",
                    "Both periodFrom and periodTo must be provided together (or both omitted).");
        }

        // Enforce correct ordering
        if (fromProvided && periodFrom.isAfter(periodTo)) {
            throw new BusinessValidationException("booking statistics", "periodFrom must be on or before periodTo.");
        }

        // Full history: List -> Map ("2026-01-11" -> count)
        Map<String, Long> allByDay = bookingRepository.findAllBookingsCountByCheckInDay()
                .stream()
                .collect(Collectors.toMap(row -> ((LocalDate) row[0]).toString(), row -> (Long) row[1]));

        // Current/selected month
        LocalDate monthFrom = month != null ? month.atDay(1) : YearMonth.now().atDay(1);
        LocalDate monthTo = monthFrom.with(TemporalAdjusters.lastDayOfMonth());
        Map<String, Long> monthByDay = bookingRepository.findBookingsCountByCheckInDayInRange(monthFrom, monthTo)
                .stream()
                .collect(Collectors.toMap(row -> ((LocalDate) row[0]).toString(), row -> (Long) row[1]));

        // Monthly bars
        Map<String, Long> monthlyMap;
        if (periodFrom != null && periodTo != null) {
            List<Object[]> raw = bookingRepository.countBookingsByCheckInMonthInRange(periodFrom, periodTo);
            monthlyMap = getMonthlyBookingsMap(raw, periodFrom, periodTo);
        } else {
            List<Object[]> raw = bookingRepository.countAllBookingsByCheckInMonth();
            monthlyMap = getMonthlyBookingsMap(raw, null, null);
        }

        // Room type bars
        List<Object[]> rawRoomTypes;
        if (periodFrom != null && periodTo != null) {
            rawRoomTypes = bookingRepository.countBookingsByRoomTypeInRange(periodFrom, periodTo);
        } else {
            rawRoomTypes = bookingRepository.countAllBookingsByRoomType();
        }
        Map<String, Long> byRoomType = rawRoomTypes.stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1],
                        (existing, replacement) -> existing // Handle potential duplicates (unlikely)
                ));

        return BookingStatisticsResponseDto.builder()
                .allBookingsByDay(allByDay)
                .monthBookingsByDay(monthByDay)
                .bookingsByMonth(monthlyMap)
                .bookingsByRoomType(byRoomType)
                .build();
    }

    private Map<String, Long> getMonthlyBookingsMap(List<Object[]> raw, LocalDate from, LocalDate to) {
        YearMonth currentYear = YearMonth.now();
        Map<String, Long> result = new HashMap<>();
        for (int m = 1; m <= 12; m++) {
            result.put(String.format("%02d", m), 0L);
        }

        raw.forEach(row -> {
            Integer year = ((Number) row[0]).intValue();
            Integer monthNum = ((Number) row[1]).intValue();
            Long count = (Long) row[2];
            String monthKey = String.format("%02d", monthNum);

            // Filter by period if provided
            if (from != null && to != null) {
                YearMonth ym = YearMonth.of(year, monthNum);
                if (ym.atEndOfMonth().isBefore(from) || ym.atDay(1).isAfter(to)) {
                    return;
                }
            }
            result.put(monthKey, count);
        });
        return result;
    }

    private boolean isInPeriod(String monthStr, LocalDate from, LocalDate to) {
        if (from == null || to == null)
            return true;
        YearMonth ym = YearMonth.parse(monthStr);
        return !ym.atEndOfMonth().isBefore(from) && !ym.atDay(1).isAfter(to);
    }
}
