package tuwien.taskinn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tuwien.taskinn.annotation.RequireRole;
import tuwien.taskinn.dto.response.BookingStatisticsResponseDto;
import tuwien.taskinn.dto.response.CleaningStatisticsResponseDto;
import tuwien.taskinn.dto.response.MinibarStatisticsResponseDto;
import tuwien.taskinn.dto.response.EmployeeStatisticsResponseDto;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.services.StatisticsService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RequireRole(Roles.ADMIN)
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Retrieves comprehensive booking statistics for the admin dashboard.
     *
     * @param periodFrom optional start date (ISO-8601 format, yyyy-MM-dd) for filtering the bar chart statistics; must
     *        be provided if periodTo is present
     * @param periodTo optional end date (ISO-8601 format, yyyy-MM-dd) for filtering the bar chart statistics; must be
     *        provided if periodFrom is present
     * @param month optional specific month (format yyyy-MM) to focus the daily breakdown on; defaults to the current
     *        month if not specified
     * @return HTTP 200 response containing a BookingStatisticsResponseDto with the aggregated metrics
     */
    @GetMapping("/bookings")
    public ResponseEntity<BookingStatisticsResponseDto> getBookingStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,

            @RequestParam(required = false) YearMonth month) {
        var stats = statisticsService.getBookingStatistics(periodFrom, periodTo, month);
        return ResponseEntity.ok(stats);
    }

    /**
     * REST endpoint for retrieving cleaning statistics for the admin dashboard.
     *
     * @param periodFrom optional start date (ISO-8601 format, yyyy-MM-dd) for filtering statistics
     * @param periodTo optional end date (ISO-8601 format, yyyy-MM-dd) for filtering statistics
     * @return HTTP 200 response containing a CleaningStatisticsResponseDto with aggregated statistics
     */
    @GetMapping("/cleaning")
    public ResponseEntity<CleaningStatisticsResponseDto> getCleaningStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo) {
        var stats = statisticsService.getCleaningStatistics(periodFrom, periodTo);
        return ResponseEntity.ok(stats);
    }

    /**
     * Retrieves cleaning statistics for specific employees. If no employees are specified, defaults to the first
     * employee alphabetically.
     *
     * @param periodFrom optional start date
     * @param periodTo optional end date
     * @param employeeIds optional list of employee IDs to fetch statistics for
     */
    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeStatisticsResponseDto>> getEmployeeStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) List<Long> employeeIds) {

        var stats = statisticsService.getEmployeeStatistics(periodFrom, periodTo, employeeIds);
        return ResponseEntity.ok(stats);
    }

    /**
     * Retrieves minibar statistics including sales distribution and daily trends.
     *
     * @param periodFrom optional start date
     * @param periodTo optional end date
     * @param itemIds optional list of inventory item IDs for the line chart. If null/empty, defaults to first item.
     */
    @GetMapping("/minibar")
    public ResponseEntity<MinibarStatisticsResponseDto> getMinibarStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(required = false) List<Long> itemIds) {

        var stats = statisticsService.getMinibarStatistics(periodFrom, periodTo, itemIds);
        return ResponseEntity.ok(stats);
    }
}
