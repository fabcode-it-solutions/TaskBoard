package tuwien.taskinn.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tuwien.taskinn.dto.request.MaintenanceTicketCreationUpdateDto;
import tuwien.taskinn.dto.response.MaintenanceTicketDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.MaintenanceTicket;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.TicketStatus;
import tuwien.taskinn.events.MaintenanceTicketChangedEvent;
import tuwien.taskinn.mapper.MaintenanceMapper;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.MaintenanceTicketRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.services.MaintenanceService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final EmployeeRepository employeeRepository;
    private final RoomRepository roomRepository;
    private final MaintenanceMapper maintenanceMapper;

    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public MaintenanceTicketDto createMaintenanceTicket(MaintenanceTicketCreationUpdateDto requestDto,
            String reporterEmail) {

        if (reporterEmail == null) {
            throw new IllegalArgumentException("Reporter ID cannot be null");
        }

        Employee reporter = employeeRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new IllegalStateException("Employee not found: " + reporterEmail));

        Room room = this.roomRepository.findById(requestDto.getRoomId())
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + requestDto.getRoomId()));

        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .priority(requestDto.getPriority())
                .status(TicketStatus.OPEN)
                .room(room)
                .reporter(reporter)
                .build();

        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket);

        this.publisher.publishEvent(new MaintenanceTicketChangedEvent(saved.getRoom().getId()));

        return this.maintenanceMapper.toDto(saved);
    }

    @Override
    public List<MaintenanceTicketDto> getAllMaintenanceTickets() {
        return maintenanceTicketRepository.findAll()
                .stream()
                .map(maintenanceMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public MaintenanceTicketDto getMaintenanceTicketById(Long id) {
        MaintenanceTicket ticket = maintenanceTicketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("MaintenanceTicket not found: " + id));
        return maintenanceMapper.toDto(ticket);
    }

    @Override
    @Transactional
    public MaintenanceTicketDto updateMaintenanceTicket(Long id, MaintenanceTicketCreationUpdateDto requestDto) {
        MaintenanceTicket existing = maintenanceTicketRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("MaintenanceTicket not found with id: " + id));

        if (!id.equals(requestDto.getId())) {
            throw new IllegalArgumentException("ID in path and request body do not match");
        }

        existing.setTitle(requestDto.getTitle());
        existing.setDescription(requestDto.getDescription());
        existing.setPriority(requestDto.getPriority());
        existing.setStatus(requestDto.getStatus());

        if (requestDto.getRoomId() != null && !requestDto.getRoomId().equals(existing.getRoom().getId())) {
            Room newRoom = roomRepository.findById(requestDto.getRoomId())
                    .orElseThrow(() -> new NoSuchElementException("Room not found with id: " + requestDto.getRoomId()));
            existing.setRoom(newRoom);
        }

        MaintenanceTicket saved = maintenanceTicketRepository.save(existing);

        this.publisher.publishEvent(new MaintenanceTicketChangedEvent(saved.getRoom().getId()));

        return maintenanceMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteMaintenanceTicket(Long id) {
        if (!maintenanceTicketRepository.existsById(id)) {
            throw new NoSuchElementException("MaintenanceTicket not found: " + id);
        }

        MaintenanceTicket saved = maintenanceTicketRepository.findById(id).get();
        Long roomId = saved.getRoom().getId();

        maintenanceTicketRepository.deleteById(id);

        this.publisher.publishEvent(new MaintenanceTicketChangedEvent(roomId));

    }

    @Override
    public List<MaintenanceTicketDto> getAllOpenMaintenanceTickets() {
        return maintenanceTicketRepository.findMaintenanceTicketsByStatus_Open()
                .stream()
                .map(maintenanceMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Long countHighPriorityTicketsNeedingAttention() {
        return maintenanceTicketRepository.countHighPriorityTicketsNeedingAttention();
    }
}
