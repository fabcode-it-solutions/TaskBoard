package tuwien.taskinn.services;

import tuwien.taskinn.dto.response.BookingStatisticsResponseDto;
import tuwien.taskinn.dto.response.CleaningStatisticsResponseDto;
import tuwien.taskinn.dto.response.MinibarStatisticsResponseDto;
import tuwien.taskinn.dto.response.EmployeeStatisticsResponseDto;

import java.util.List;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface StatisticsService {

    /**
     * Retrieves booking statistics for the admin dashboard, combining:
     *
     * All history: bookings grouped by check-in day (unfiltered). Selected month: bookings grouped by check-in day for
     * the given month (or the current month if null). Bar charts: bookings grouped by month and by room type,
     * optionally filtered by a given period.
     *
     * @param periodFrom inclusive start date for filtering the bar charts (optional; must be used together with
     *        periodTo
     * @param periodTo inclusive end date for filtering the bar charts (optional; must be used together with periodFrom
     * @param month month to compute the month-specific daily line plot for (optional; defaults to the current month)
     * @return aggregated booking statistics for the dashboard (never null)
     * @throws IllegalArgumentException if periodFrom is after periodTo
     */
    BookingStatisticsResponseDto getBookingStatistics(LocalDate periodFrom, LocalDate periodTo, YearMonth month);

    /**
     * Retrieves aggregated cleaning statistics.
     *
     * @param periodFrom optional start date (inclusive) for filtering statistics; if null, statistics are calculated
     *        over all available data
     * @param periodTo optional end date (inclusive) for filtering statistics; if null, statistics are calculated over
     *        all available data
     * @return a CleaningStatisticsResponseDto containing aggregated metrics such as average task duration, monthly
     *         working hours, and blocked task percentage
     */
    CleaningStatisticsResponseDto getCleaningStatistics(LocalDate periodFrom, LocalDate periodTo);

    /**
     * Retrieves cleaning statistics grouped by employee.
     *
     * @param periodFrom optional start date (inclusive)
     * @param periodTo optional end date (inclusive)
     * @param employeeIds optional list of employee IDs. If null/empty, defaults to the first employee alphabetically.
     * @return a list of DTOs containing statistics for each requested employee.
     */
    List<EmployeeStatisticsResponseDto> getEmployeeStatistics(LocalDate periodFrom, LocalDate periodTo,
            List<Long> employeeIds);

    /**
     * Retrieves minibar statistics including aggregated sales by quantity and earnings, as well as daily sales trends.
     *
     * @param periodFrom inclusive start date for filtering statistics (optional; must be used together with periodTo
     * @param periodTo inclusive end date for filtering statistics (optional; must be used together with periodFrom
     * @param itemIds optional list of inventory item IDs to include in the daily sales line charts. If null or empty,
     *        the service defaults to selecting the first item alphabetically.
     * @return a MinibarStatisticsResponseDto containing sales distribution maps and daily sales data (never null).
     */
    MinibarStatisticsResponseDto getMinibarStatistics(LocalDate periodFrom, LocalDate periodTo, List<Long> itemIds);
}
