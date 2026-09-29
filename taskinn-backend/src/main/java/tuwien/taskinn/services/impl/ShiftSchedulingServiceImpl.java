package tuwien.taskinn.services.impl;

import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tuwien.taskinn.dto.EmployeeMonthShiftDto;
import tuwien.taskinn.dto.SchedulableEmployeeDto;
import tuwien.taskinn.dto.SchedulingRoleDto;
import tuwien.taskinn.dto.ShiftScheduleTemplateDto;
import tuwien.taskinn.dto.request.ShiftScheduleSolveRequestDto;
import tuwien.taskinn.entity.*;
import tuwien.taskinn.exceptions.InfeasibleInstanceException;
import tuwien.taskinn.mapper.EmployeeMapper;
import tuwien.taskinn.mapper.SchedulingMapper;
import tuwien.taskinn.repo.*;
import tuwien.taskinn.services.ShiftSchedulingService;
import tuwien.taskinn.services.scheduling.instance.ShiftSchedulingInstance;
import tuwien.taskinn.services.scheduling.solution.ShiftSchedulingSolution;
import tuwien.taskinn.services.scheduling.solver.GreedyShiftScheduleGenerator;
import tuwien.taskinn.services.scheduling.solver.ShiftScheduleDailyReassignmentNeighborhood;
import tuwien.taskinn.services.scheduling.solver.ShiftSchedulingSolver;
import tuwien.taskinn.services.validator.ShiftSchedulingValidator;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;

import com.lowagie.text.*;
import java.time.format.TextStyle;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftSchedulingServiceImpl implements ShiftSchedulingService {

    private final EmployeeMapper employeeMapper;
    private final SchedulingMapper schedulingMapper;
    private final ShiftSchedulingValidator shiftSchedulingValidator;
    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;
    private final SchedulingRoleRepository schedulingRoleRepository;
    private final PTORepository unavailabilityRepository;
    private final ShiftScheduleTemplateRepository scheduleTemplateRepository;

    @Override
    public List<SchedulableEmployeeDto> getSchedulableEmployees() {
        return employeeRepository.findAllSchedulableEmployees().stream().map(employeeMapper::toSchedulableDto).toList();
    }

    @Override
    public List<EmployeeMonthShiftDto> getShiftsForMonth(Integer year, Integer month) {
        shiftSchedulingValidator.validateYearAndMonth(year, month);

        final LocalDate monthStart = LocalDate.of(year, month, 1);
        final LocalDate monthEnd = monthStart.with(lastDayOfMonth());

        // The following uses the dates from the relevant entities in the database to produce a representation based
        // on day indices in a given month.

        final HashMap<Long, ArrayList<Integer>> dayIndices = new HashMap<>();
        final HashMap<Long, ArrayList<Integer>> ptoIndices = new HashMap<>();

        final List<Shift> storedShifts = shiftRepository.findAllShiftsInTimeframe(monthStart.atStartOfDay(),
                monthEnd.plusDays(1).atStartOfDay());
        for (Shift s : storedShifts) {
            final long employeeId = s.getEmployee().getId();
            if (!dayIndices.containsKey(employeeId)) {
                dayIndices.put(employeeId, new ArrayList<>());
            }
            dayIndices.get(employeeId).add(s.getDate().getDayOfMonth() - 1);
        }

        final List<PTO> storedUnavailabilities =
                unavailabilityRepository.findAllPTOsInTimeframe(monthStart, monthEnd.plusDays(1));
        for (PTO u : storedUnavailabilities) {
            final long employeeId = u.getEmployee().getId();
            if (!ptoIndices.containsKey(employeeId)) {
                ptoIndices.put(employeeId, new ArrayList<>());
            }
            for (LocalDate t = u.getStartDate(); (t.isBefore(monthEnd.plusDays(1))
                    && t.isBefore(u.getEndDate().plusDays(1))); t = t.plusDays(1)) {
                ptoIndices.get(employeeId).add(t.getDayOfMonth() - 1);
            }
        }

        final ArrayList<EmployeeMonthShiftDto> result = new ArrayList<>();

        for (long employeeId : dayIndices.keySet()) {
            result.add(EmployeeMonthShiftDto.builder()
                    .employee(employeeMapper.toSchedulableDto(employeeRepository.findById(employeeId)
                            .orElseThrow(() -> new EntityNotFoundException("No employee found with ID " + employeeId))))
                    .dayIndices(dayIndices.get(employeeId))
                    .ptoIndices(ptoIndices.getOrDefault(employeeId, new ArrayList<>()))
                    .build());
        }
        for (long employeeId : ptoIndices.keySet()) {
            if (!dayIndices.containsKey(employeeId)) {
                result.add(EmployeeMonthShiftDto.builder()
                        .employee(employeeMapper.toSchedulableDto(employeeRepository.findById(employeeId)
                                .orElseThrow(
                                        () -> new EntityNotFoundException("No employee found with ID " + employeeId))))
                        .dayIndices(new ArrayList<>())
                        .ptoIndices(ptoIndices.getOrDefault(employeeId, new ArrayList<>()))
                        .build());
            }
        }

        return result;
    }

    @Override
    @Transactional
    public void putShiftSchedule(int year, int month, List<EmployeeMonthShiftDto> shifts) {
        shiftSchedulingValidator.validateYearAndMonth(year, month);

        final LocalDateTime monthStart = LocalDateTime.of(year, month, 1, 0, 0);
        final LocalDateTime monthEnd = monthStart.with(lastDayOfMonth());
        final long monthDays = ChronoUnit.DAYS.between(monthStart, monthEnd) + 1;

        shifts.forEach(x -> shiftSchedulingValidator.validateEmployeeMonthShiftForPut(monthDays, x));

        shiftRepository.deleteAllShiftsOfEmployeesInTimeframe(
                shifts.stream().map(x -> x.getEmployee().getId()).toList(), monthStart, monthEnd);

        for (EmployeeMonthShiftDto monthShift : shifts) {
            final Long employeeId = monthShift.getEmployee().getId();
            final Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new EntityNotFoundException("No employee found with ID " + employeeId));
            for (int dayIndex : monthShift.getDayIndices()) {
                shiftRepository.save(
                        Shift.builder().date(monthStart.plusDays(dayIndex)).department("").employee(employee).build());
            }
        }
    }

    @Override
    public List<EmployeeMonthShiftDto> solveShiftScheduling(ShiftScheduleSolveRequestDto request) {
        shiftSchedulingValidator.validateShiftScheduleSolveRequest(request);

        final LocalDate monthStart = LocalDate.of(request.getYear(), request.getMonth(), 1);
        final LocalDate monthEnd = monthStart.with(lastDayOfMonth());
        final long monthDays = ChronoUnit.DAYS.between(monthStart, monthEnd) + 1;

        boolean anyStaffing = false;
        for (int staffingLevel : request.getStaffingLevels()) {
            if (staffingLevel > 0) {
                anyStaffing = true;
                break;
            }
        }
        if (!anyStaffing) {
            return List.of();
        }

        // The following converts the shift scheduling problem description from the format of the DTO into the
        // format that the solver expects

        final ArrayList<Boolean> weekStarts = new ArrayList<>();
        for (int i = 0; i < monthDays; i++) {
            weekStarts.add(monthStart.plusDays(i).getDayOfWeek() == DayOfWeek.MONDAY);
        }

        final HashMap<Long, ArrayList<Integer>> ptoMap = new HashMap<>();
        for (long id : request.getEmployeeIds()) {
            ptoMap.put(id, new ArrayList<>());
        }

        final List<PTO> ptos = unavailabilityRepository.findEmployeePTOsInTimeframe(request.getEmployeeIds(),
                monthStart, monthEnd.plusDays(1));
        for (final PTO u : ptos) {
            final LocalDate startTime = u.getStartDate().isBefore(monthStart) ? monthStart : u.getStartDate();
            final LocalDate endTime = u.getEndDate().isAfter(monthEnd.plusDays(1)) ? monthEnd : u.getEndDate();

            final ArrayList<Integer> ptoList = ptoMap.get(u.getEmployee().getId());
            for (LocalDate day = startTime; day.isBefore(endTime.plusDays(1)); day = day.plusDays(1)) {
                ptoList.add(day.getDayOfMonth() - 1);
            }
        }

        final List<ShiftSchedulingInstance.StaffMember> staffMembers = request.getEmployeeIds().stream().map(id -> {
            final Employee employee = employeeRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("No employee found with ID " + id));
            return new ShiftSchedulingInstance.StaffMember(employee.getWorkingHoursPerWeek().intValue(),
                    employee.getHourlyWage().intValue(), employee.getHourlyOvertimeWage().intValue(), ptoMap.get(id));
        }).toList();

        // Do the actual solving

        // Instance describing the given shift scheduling problem
        final ShiftSchedulingInstance instance = new ShiftSchedulingInstance(staffMembers.stream(),
                request.getStaffingLevels().stream(), request.getDailyHours().stream(), weekStarts.stream());
        // Random number generator used throughout the solving process
        final Random random = new Random(instance.instanceSeed());
        // Generator for initial seed solutions
        final var generator = new GreedyShiftScheduleGenerator();
        // Definition of solution neighborhoods for steps within the solver
        final var neighborhood = new ShiftScheduleDailyReassignmentNeighborhood();
        // The actual solver itself
        final var solver = new ShiftSchedulingSolver(50, 10, 900, generator, neighborhood);
        ShiftSchedulingSolution solution;
        try {
            solution = solver.solve(random, instance);
        } catch (InfeasibleInstanceException e) {
            throw e;
        } catch (RuntimeException e) {
            // Last ditch measure: if the solver failed in an unexpected way, return the seed solution. This ensures
            // that the user sees at least something.
            solution = generator.generate(instance);
        }

        // Convert the obtained solution to fit into the required DTO

        final ArrayList<EmployeeMonthShiftDto> result = new ArrayList<>();
        for (int i = 0; i < request.getEmployeeIds().size(); i++) {
            final Long employeeId = request.getEmployeeIds().get(i);
            final Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new EntityNotFoundException("No employee found with ID " + employeeId));

            ArrayList<Integer> dayIndices = new ArrayList<>();
            for (int j = 0; j < monthDays; j++) {
                if (solution.getAssigned(i, j)) {
                    dayIndices.add(j);
                }
            }

            result.add(EmployeeMonthShiftDto.builder()
                    .employee(employeeMapper.toSchedulableDto(employee))
                    .dayIndices(dayIndices)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional
    public List<SchedulingRoleDto> getAllSchedulingRoles() {
        final List<SchedulingRole> roles = schedulingRoleRepository.findAll();

        final ArrayList<SchedulingRoleDto> result = new ArrayList<>();
        for (final SchedulingRole role : roles) {
            result.add(SchedulingRoleDto.builder()
                    .id(role.getId())
                    .name(role.getRoleName())
                    .employees(employeeRepository.findAllEmployeesWithSchedulingRole(role.getId())
                            .stream()
                            .map(employeeMapper::toSchedulableDto)
                            .toList())
                    .build());
        }

        return result;
    }

    @Override
    @Transactional
    public void deleteSchedulingRole(Long id) {
        final List<Employee> impactedEmployees = employeeRepository.findAllEmployeesWithSchedulingRole(id);
        for (final Employee employee : impactedEmployees) {
            employee.setSchedulingRole(null);
            employeeRepository.save(employee);
        }
        schedulingRoleRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void createSchedulingRole(SchedulingRoleDto request) {
        shiftSchedulingValidator.validateSchedulingRole(request);

        final SchedulingRole newRole =
                schedulingRoleRepository.save(SchedulingRole.builder().roleName(request.getName()).build());

        for (final SchedulableEmployeeDto dtoEmployee : request.getEmployees()) {
            final Employee employee = employeeRepository.findById(dtoEmployee.getId())
                    .orElseThrow(() -> new EntityNotFoundException("No employee found with ID " + dtoEmployee.getId()));
            employee.setSchedulingRole(newRole);
            employeeRepository.save(employee);
        }
    }

    @Override
    @Transactional
    public void updateSchedulingRole(Long id, SchedulingRoleDto request) {
        shiftSchedulingValidator.validateSchedulingRole(request);

        final SchedulingRole role = schedulingRoleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No scheduling role found with ID " + id));
        role.setRoleName(request.getName());
        final SchedulingRole newRole = schedulingRoleRepository.save(role);
        final HashSet<Long> previousEmployees =
                new HashSet<>(employeeRepository.findAllEmployeesWithSchedulingRole(newRole.getId())
                        .stream()
                        .map(Employee::getId)
                        .toList());

        for (final SchedulableEmployeeDto dtoEmployee : request.getEmployees()) {
            final Employee employee = employeeRepository.findById(dtoEmployee.getId())
                    .orElseThrow(() -> new EntityNotFoundException("No employee found with ID " + dtoEmployee.getId()));
            previousEmployees.remove(employee.getId());
            employee.setSchedulingRole(newRole);
            employeeRepository.save(employee);
        }

        for (final Long unassignedEmployee : previousEmployees) {
            final Employee employee = employeeRepository.findById(unassignedEmployee)
                    .orElseThrow(() -> new EntityNotFoundException("No employee found with ID " + unassignedEmployee));
            employee.setSchedulingRole(null);
            employeeRepository.save(employee);
        }
    }

    @Override
    @Transactional
    public List<ShiftScheduleTemplateDto> getAllShiftScheduleTemplates() {
        return scheduleTemplateRepository.findAll().stream().map(schedulingMapper::toDto).toList();
    }

    @Override
    @Transactional
    public void deleteShiftScheduleTemplate(Long id) {
        scheduleTemplateRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void createShiftScheduleTemplate(ShiftScheduleTemplateDto request) {
        shiftSchedulingValidator.validateShiftScheduleTemplate(request);

        scheduleTemplateRepository
                .save(ShiftScheduleTemplate.builder().name(request.getName()).content(request.getContent()).build());
    }

    @Override
    @Transactional
    public void updateShiftScheduleTemplate(Long id, ShiftScheduleTemplateDto request) {
        shiftSchedulingValidator.validateShiftScheduleTemplate(request);

        final ShiftScheduleTemplate template = scheduleTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No scheduling template found with ID " + id));
        template.setName(request.getName());
        template.setContent(request.getContent());
        scheduleTemplateRepository.save(template);
    }

    @Override
    public byte[] getShiftSchedulePdf(Integer year, Integer month) {
        List<EmployeeMonthShiftDto> scheduleData = getShiftsForMonth(year, month);

        List<SchedulableEmployeeDto> allActiveEmployees = getSchedulableEmployees();

        LocalDate monthDate = LocalDate.of(year, month, 1);
        int daysInMonth = monthDate.lengthOfMonth();

        Map<Long, EmployeeMonthShiftDto> scheduleMap =
                scheduleData.stream().collect(Collectors.toMap(dto -> dto.getEmployee().getId(), Function.identity()));

        List<EmployeeMonthShiftDto> completeScheduleData = allActiveEmployees.stream().map(employeeDto -> {
            return scheduleMap.getOrDefault(employeeDto.getId(),
                    EmployeeMonthShiftDto.builder()
                            .employee(employeeDto)
                            .dayIndices(new ArrayList<>())
                            .ptoIndices(new ArrayList<>())
                            .build());
        }).collect(Collectors.toList());

        List<EmployeeMonthShiftDto> unassignedStaff = completeScheduleData.stream()
                .filter(dto -> dto.getEmployee().getSchedulingRoleName() == null)
                .collect(Collectors.toList());

        Map<String, List<EmployeeMonthShiftDto>> employeesByRole = completeScheduleData.stream()
                .filter(dto -> dto.getEmployee().getSchedulingRoleName() != null)
                .collect(Collectors.groupingBy(dto -> dto.getEmployee().getSchedulingRoleName()));

        List<String> sortedRoles = new ArrayList<>(employeesByRole.keySet());
        Collections.sort(sortedRoles);

        long totalShifts = completeScheduleData.stream().mapToLong(e -> e.getDayIndices().size()).sum();
        long totalPtoDays = completeScheduleData.stream().mapToLong(e -> e.getPtoIndices().size()).sum();
        long activeEmployees = completeScheduleData.stream().filter(e -> !e.getDayIndices().isEmpty()).count();

        Color primaryColor = new Color(50, 80, 160);
        Color workColor = new Color(200, 230, 200);
        Color ptoColor = new Color(255, 200, 200);
        Color zebraColor = new Color(250, 250, 255);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, primaryColor);
            Paragraph title = new Paragraph("TaskInn Shift Schedule Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 14, Color.DARK_GRAY);
            String monthString = monthDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            Paragraph subtitle = new Paragraph("Period: " + monthString + " " + year, subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(40);
            document.add(subtitle);

            PdfPTable statsTable = new PdfPTable(4);
            statsTable.setWidthPercentage(80);
            statsTable.setSpacingAfter(20);
            addStatCard(statsTable, "Total Employees", String.valueOf(completeScheduleData.size()), primaryColor);
            addStatCard(statsTable, "Active Staff", String.valueOf(activeEmployees), primaryColor);
            addStatCard(statsTable, "Total Shifts", String.valueOf(totalShifts), new Color(0, 128, 0));
            addStatCard(statsTable, "Total PTO Days", String.valueOf(totalPtoDays), new Color(200, 0, 0));
            document.add(statsTable);

            for (String roleName : sortedRoles) {
                List<EmployeeMonthShiftDto> roleEmployees = employeesByRole.get(roleName);
                generateRolePage(document, roleName, roleEmployees, daysInMonth, monthDate, primaryColor, workColor,
                        ptoColor, zebraColor);
            }

            if (!unassignedStaff.isEmpty()) {
                generateRolePage(document, "Unassigned Staff", unassignedStaff, daysInMonth, monthDate, primaryColor,
                        workColor, ptoColor, zebraColor);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private void generateRolePage(Document document, String title, List<EmployeeMonthShiftDto> employees,
            int daysInMonth, LocalDate monthDate, Color primaryColor, Color workColor, Color ptoColor, Color zebraColor)
            throws DocumentException {
        document.newPage();

        Font roleTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
        Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font legendFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

        Paragraph roleHeader = new Paragraph("Department: " + title, roleTitleFont);
        roleHeader.setSpacingAfter(10);
        document.add(roleHeader);

        PdfPTable table = new PdfPTable(1 + daysInMonth);
        table.setWidthPercentage(100);
        float[] widths = new float[1 + daysInMonth];
        widths[0] = 4f;
        for (int i = 1; i < widths.length; i++)
            widths[i] = 1f;
        table.setWidths(widths);

        addHeaderCell(table, "Employee", primaryColor, Color.WHITE);
        for (int i = 1; i <= daysInMonth; i++) {
            LocalDate dayDate = LocalDate.of(monthDate.getYear(), monthDate.getMonth(), i);
            boolean isWeekend = dayDate.getDayOfWeek().getValue() >= 6;
            Color bg = isWeekend ? new Color(200, 200, 220) : primaryColor;
            addHeaderCell(table, String.valueOf(i), bg, Color.WHITE);
        }

        employees.sort(Comparator.comparing(e -> e.getEmployee().getLastName()));

        boolean alternate = false;
        for (EmployeeMonthShiftDto employeeShift : employees) {
            Color rowBg = alternate ? zebraColor : Color.WHITE;
            alternate = !alternate;

            String name = employeeShift.getEmployee().getFirstName() + " " + employeeShift.getEmployee().getLastName();
            PdfPCell nameCell = new PdfPCell(new Phrase(name, nameFont));
            nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nameCell.setPadding(4);
            nameCell.setBackgroundColor(rowBg);
            table.addCell(nameCell);

            for (int day = 0; day < daysInMonth; day++) {
                PdfPCell cell = new PdfPCell();
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);

                LocalDate currentDayDate = monthDate.plusDays(day);
                boolean isWeekend = currentDayDate.getDayOfWeek().getValue() >= 6;
                Color baseColor = isWeekend ? new Color(245, 245, 245) : rowBg;
                cell.setBackgroundColor(baseColor);

                if (employeeShift.getDayIndices().contains(day)) {
                    cell.setPhrase(new Phrase("S", cellFont));
                    cell.setBackgroundColor(workColor);
                } else if (employeeShift.getPtoIndices().contains(day)) {
                    cell.setPhrase(new Phrase("PTO", cellFont));
                    cell.setBackgroundColor(ptoColor);
                } else {
                    cell.setPhrase(new Phrase("", cellFont));
                }
                table.addCell(cell);
            }
        }
        document.add(table);

        Paragraph legend = new Paragraph("Legend: S = Scheduled Shift, PTO = Paid Time Off", legendFont);
        legend.setSpacingBefore(5);
        document.add(legend);
    }

    private void addStatCard(PdfPTable table, String label, String value, Color valueColor) {
        PdfPTable card = new PdfPTable(1);

        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, valueColor);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        card.addCell(valueCell);

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        card.addCell(labelCell);

        PdfPCell wrapper = new PdfPCell(card);
        wrapper.setPadding(10);
        wrapper.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(wrapper);
    }

    private void addHeaderCell(PdfPTable table, String text, Color bgColor, Color textColor) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, textColor);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        table.addCell(cell);
    }
}
