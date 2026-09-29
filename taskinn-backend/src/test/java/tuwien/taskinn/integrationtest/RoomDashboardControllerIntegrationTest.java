package tuwien.taskinn.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.dto.response.DashboardRoomDto;
import tuwien.taskinn.entity.*;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.enums.RoomStatus;

@DisplayName("Room Dashboard Controller Integration Tests")
public class RoomDashboardControllerIntegrationTest extends BaseIntegrationTest {

    private static final String DASHBOARD_URI = "/api/v1/dashboard/rooms";

    @Test
    @DisplayName("GET /dashboard/rooms -> Fails with 403 Forbidden for unauthenticated user")
    void getCurrentRoomStates_whenUnauthenticated_thenForbidden() throws Exception {
        mockMvc.perform(get(DASHBOARD_URI)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /dashboard/rooms -> Returns 200 and empty list if no rooms exist")
    void getCurrentRoomStates_whenNoRooms_returnsEmptyList() throws Exception {
        String employeeToken = getEmployeeAuthToken();

        var result = mockMvc.perform(get(DASHBOARD_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andReturn();

        List<DashboardRoomDto> dtos =
                objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
        assertThat(dtos).isEmpty();
    }

    @Test
    @DisplayName("GET /dashboard/rooms -> Returns correct room state with guest info for checked-in booking")
    void getCurrentRoomStates_whenRoomOccupied_returnsCorrectData() throws Exception {
        RoomType rt = roomTypeRepository
                .save(RoomType.builder().name("Single").capacity(1).basePrice(BigDecimal.TEN).build());
        Room room = roomRepository.save(Room.builder()
                .roomNumber("101")
                .floor(1)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .roomType(rt)
                .svgPoints(List.of(new Point(0.0d, 0.0d), new Point(1.0d, 0.0d), new Point(1.0d, 1.0d),
                        new Point(0.0d, 1.0d)))
                .build());

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Alice")
                .lastName("Wonderland")
                .email("alice@test.com")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .build());

        bookingRepository.save(Booking.builder()
                .guest(guest)
                .rooms(Set.of(room))
                .checkInDate(LocalDate.now().minusDays(1))
                .checkOutDate(LocalDate.now().plusDays(1))
                .status(BookingStatus.CHECKED_IN)
                .numAdults(1)
                .build());

        String employeeToken = getEmployeeAuthToken();

        var result = mockMvc.perform(get(DASHBOARD_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andReturn();

        List<DashboardRoomDto> dtos =
                objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertAll("Verify Dashboard Data", () -> assertThat(dtos).hasSize(1),
                () -> assertThat(dtos.get(0).getRoomNumber()).isEqualTo("101"),
                () -> assertThat(dtos.get(0).getGuestName()).isEqualTo("Alice Wonderland"));
    }
}
