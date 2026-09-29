package tuwien.taskinn.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.basetest.TestData;
import tuwien.taskinn.dto.request.CheckInGuestDto;
import tuwien.taskinn.dto.request.CreateOrUpdateBookingRequestDto;
import tuwien.taskinn.dto.request.GuestInfoDto;
import tuwien.taskinn.dto.request.RoomOptionsRequestDto;
import tuwien.taskinn.dto.request.RoomTypeSelectionDto;
import tuwien.taskinn.dto.response.*;
import tuwien.taskinn.entity.*;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.repo.RoomTypeRepository;
import tuwien.taskinn.services.EmailService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Booking Controller Integration Tests")
public class BookingControllerIntegrationTest extends BaseIntegrationTest implements TestData {

    private static final String BOOKING_BASE_URI = BASE_URI + "/booking";
    private static final String BOOKING_CREATE_URI = BOOKING_BASE_URI + "/create-booking";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @MockBean
    private EmailService emailService;

    @Test
    @DisplayName("POST /room-options -> Returns room options for valid booking criteria")
    public void getRoomOptions_whenValidCriteria_returnsRoomOptions() throws Exception {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(5);
        Integer numRooms = 2;
        Integer numAdults = 4;
        Integer numChildren = 2;

        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        // When & Then
        var mvcResult = mockMvc.perform(post(BOOKING_BASE_URI + "/room-options").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<RoomOptionDto> roomOptions =
                objectMapper.readValue(responseBody, new TypeReference<List<RoomOptionDto>>() {});

        assertAll(() -> assertThat(roomOptions).isNotNull(),
                // TODO: Add assertions for the room options when the logic is implemented
                () -> assertThat(roomOptions).isEmpty());
    }

    @Test
    @DisplayName("POST /room-options -> Returns 400 Bad Request when check-out date is before check-in date")
    public void getRoomOptions_whenCheckOutBeforeCheckIn_returnsBadRequest() throws Exception {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(2);
        LocalDate checkOutDate = LocalDate.now().plusDays(1);
        Integer numRooms = 1;
        Integer numAdults = 2;
        Integer numChildren = 0;

        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        // When
        Map<String, String> validationErrors = performBadRequestAndGetValidationErrors(
                post(BOOKING_BASE_URI + "/room-options").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)));

        // Then
        assertAll(() -> assertThat(validationErrors).hasSize(1), () -> assertThat(validationErrors)
                .containsEntry("checkOutDate", "Check-out date must be after check-in date"));
    }

    @Test
    @DisplayName("POST /room-options -> Returns 400 Bad Request when check-in date is in the past")
    public void getRoomOptions_whenCheckInAndCheckOutDatesInPast_returnsBadRequest() throws Exception {
        // Given
        LocalDate checkInDate = LocalDate.now().minusDays(2);
        LocalDate checkOutDate = LocalDate.now().minusDays(1);
        Integer numRooms = 0;
        Integer numAdults = 0;
        Integer numChildren = -1;

        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        // When
        Map<String, String> validationErrors = performBadRequestAndGetValidationErrors(
                post(BOOKING_BASE_URI + "/room-options").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)));

        // Then
        assertAll(() -> assertThat(validationErrors).hasSize(5),
                () -> assertThat(validationErrors).containsEntry("checkInDate",
                        "must be a date in the present or in the future"),
                () -> assertThat(validationErrors).containsEntry("checkOutDate", "must be a future date"),
                () -> assertThat(validationErrors).containsEntry("numRooms", "must be greater than or equal to 1"),
                () -> assertThat(validationErrors).containsEntry("numAdults", "must be greater than or equal to 1"),
                () -> assertThat(validationErrors).containsEntry("numChildren", "must be greater than or equal to 0"));
    }

    @Test
    @DisplayName("GET /{bookingId}/checkin -> Returns list of rooms for valid bookingId to admin")
    public void getRoomsForCheckin_whenValidBookingIdAndAdmin_returnsRoomList() throws Exception {
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();
        String adminToken = getAdminAuthToken();

        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/" + bookingId + "/checkin/rooms")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<RoomForCheckinResponseDto> rooms =
                objectMapper.readValue(responseBody, new TypeReference<List<RoomForCheckinResponseDto>>() {});
        assertAll(() -> assertThat(rooms).isNotNull(), () -> assertThat(rooms).isNotEmpty());
    }

    @Test
    @DisplayName("GET /{bookingId}/checkin -> Returns list of rooms for valid bookingId to employee")
    public void getRoomsForCheckin_whenValidBookingIdAndEmployee_returnsRoomList() throws Exception {
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();
        String employeeAuthToken = getEmployeeAuthToken();

        var mvcResult = mockMvc
                .perform(get(BOOKING_BASE_URI + "/" + bookingId + "/checkin/rooms").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + employeeAuthToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        List<RoomForCheckinResponseDto> rooms =
                objectMapper.readValue(responseBody, new TypeReference<List<RoomForCheckinResponseDto>>() {});
        assertAll(() -> assertThat(rooms).isNotNull(), () -> assertThat(rooms).isNotEmpty());
    }

    @Test
    @DisplayName("GET /{bookingId}/checkin -> Denies access for unauthorized role")
    void getRoomsForCheckin_givenUnauthorizedAuth_thenReturnsForbidden() throws Exception {
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();

        mockMvc.perform(get(BOOKING_BASE_URI + "/" + bookingId + "/checkin/rooms")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /{bookingId}/checkin -> Successfully checks in booking for admin")
    public void checkInBooking_whenValidRequestAndAdmin_thenReturnsOk() throws Exception {
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();
        String adminToken = getAdminAuthToken();

        CheckInGuestDto guestA = createValidGuest(ids.get(1));
        CheckInGuestDto guestB = createValidGuest2(ids.get(2));

        List<CheckInGuestDto> guests = List.of(guestA, guestB);
        String guestsJson = objectMapper.writeValueAsString(guests);

        MockMultipartFile guestsPart =
                new MockMultipartFile("guests", "", MediaType.APPLICATION_JSON_VALUE, guestsJson.getBytes());
        MockMultipartFile guestFile0 =
                new MockMultipartFile("guestFiles_0", "id1.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-1".getBytes());
        MockMultipartFile guestFile1 =
                new MockMultipartFile("guestFiles_1", "id2.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-2".getBytes());

        mockMvc.perform(multipart(BOOKING_BASE_URI + "/" + bookingId + "/checkin").file(guestsPart)
                .file(guestFile0)
                .file(guestFile1)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /{bookingId}/checkin -> Successfully checks in booking for employee")
    public void checkInBooking_whenValidRequestAndEmployee_thenReturnsOk() throws Exception {
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();
        String employeeAuthToken = getEmployeeAuthToken();

        CheckInGuestDto guestA = createValidGuest(ids.get(1));
        CheckInGuestDto guestB = createValidGuest2(ids.get(2));

        List<CheckInGuestDto> guests = List.of(guestA, guestB);
        String guestsJson = objectMapper.writeValueAsString(guests);

        MockMultipartFile guestsPart =
                new MockMultipartFile("guests", "", MediaType.APPLICATION_JSON_VALUE, guestsJson.getBytes());
        MockMultipartFile guestFile0 =
                new MockMultipartFile("guestFiles_0", "id1.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-1".getBytes());
        MockMultipartFile guestFile1 =
                new MockMultipartFile("guestFiles_1", "id2.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-2".getBytes());

        mockMvc.perform(multipart(BOOKING_BASE_URI + "/" + bookingId + "/checkin").file(guestsPart)
                .file(guestFile0)
                .file(guestFile1)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeAuthToken)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /{bookingId}/checkin -> Denies access for unauthorized role")
    void checkInBooking_givenUnauthorizedAuth_thenReturnsForbidden() throws Exception {
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();

        CheckInGuestDto guestA = createValidGuest(ids.get(1));
        CheckInGuestDto guestB = createValidGuest2(ids.get(2));

        List<CheckInGuestDto> guests = List.of(guestA, guestB);
        String guestsJson = objectMapper.writeValueAsString(guests);

        MockMultipartFile guestsPart =
                new MockMultipartFile("guests", "", MediaType.APPLICATION_JSON_VALUE, guestsJson.getBytes());
        MockMultipartFile guestFile0 =
                new MockMultipartFile("guestFiles_0", "id1.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-1".getBytes());
        MockMultipartFile guestFile1 =
                new MockMultipartFile("guestFiles_1", "id2.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-2".getBytes());

        // No authorization header - should return 403 Forbidden
        mockMvc.perform(multipart(BOOKING_BASE_URI + "/" + bookingId + "/checkin").file(guestsPart)
                .file(guestFile0)
                .file(guestFile1)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /{bookingId}/checkout -> Allows access for admin on CHECKED_IN booking")
    public void checkOutBooking_whenCheckedInAndAdmin_thenReturnsOk() throws Exception {
        // Given: create booking
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();
        String adminToken = getAdminAuthToken();

        CheckInGuestDto guestA = createValidGuest(ids.get(1));
        CheckInGuestDto guestB = createValidGuest2(ids.get(2));

        List<CheckInGuestDto> guests = List.of(guestA, guestB);
        String guestsJson = objectMapper.writeValueAsString(guests);

        MockMultipartFile guestsPart =
                new MockMultipartFile("guests", "", MediaType.APPLICATION_JSON_VALUE, guestsJson.getBytes());
        MockMultipartFile guestFile0 =
                new MockMultipartFile("guestFiles_0", "id1.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-1".getBytes());
        MockMultipartFile guestFile1 =
                new MockMultipartFile("guestFiles_1", "id2.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-2".getBytes());

        mockMvc.perform(multipart(BOOKING_BASE_URI + "/" + bookingId + "/checkin").file(guestsPart)
                .file(guestFile0)
                .file(guestFile1)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk());

        // When & Then: admin can checkout
        mockMvc.perform(put(BOOKING_BASE_URI + "/" + bookingId + "/checkout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /{bookingId}/checkout -> Allows access for employee on CHECKED_IN booking")
    public void checkOutBooking_whenCheckedInAndEmployee_thenReturnsOk() throws Exception {
        // Given: create booking
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();
        String employeeToken = getEmployeeAuthToken();

        CheckInGuestDto guestA = createValidGuest(ids.get(1));
        CheckInGuestDto guestB = createValidGuest2(ids.get(2));

        List<CheckInGuestDto> guests = List.of(guestA, guestB);
        String guestsJson = objectMapper.writeValueAsString(guests);

        MockMultipartFile guestsPart =
                new MockMultipartFile("guests", "", MediaType.APPLICATION_JSON_VALUE, guestsJson.getBytes());
        MockMultipartFile guestFile0 =
                new MockMultipartFile("guestFiles_0", "id1.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-1".getBytes());
        MockMultipartFile guestFile1 =
                new MockMultipartFile("guestFiles_1", "id2.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-2".getBytes());

        // Check-in as employee so status becomes CHECKED_IN
        mockMvc.perform(multipart(BOOKING_BASE_URI + "/" + bookingId + "/checkin").file(guestsPart)
                .file(guestFile0)
                .file(guestFile1)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isOk());

        // When & Then: employee can checkout
        mockMvc.perform(put(BOOKING_BASE_URI + "/" + bookingId + "/checkout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /{bookingId}/checkout -> Denies access for unauthorized role on CHECKED_IN booking")
    public void checkOutBooking_whenUnauthorized_thenReturnsForbidden() throws Exception {
        // Given: create booking and check-in (as admin)
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();
        String adminToken = getAdminAuthToken();

        CheckInGuestDto guestA = createValidGuest(ids.get(1));
        CheckInGuestDto guestB = createValidGuest2(ids.get(2));

        List<CheckInGuestDto> guests = List.of(guestA, guestB);
        String guestsJson = objectMapper.writeValueAsString(guests);

        MockMultipartFile guestsPart =
                new MockMultipartFile("guests", "", MediaType.APPLICATION_JSON_VALUE, guestsJson.getBytes());
        MockMultipartFile guestFile0 =
                new MockMultipartFile("guestFiles_0", "id1.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-1".getBytes());
        MockMultipartFile guestFile1 =
                new MockMultipartFile("guestFiles_1", "id2.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-2".getBytes());

        mockMvc.perform(multipart(BOOKING_BASE_URI + "/" + bookingId + "/checkin").file(guestsPart)
                .file(guestFile0)
                .file(guestFile1)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk());

        // When & Then: no Authorization header -> 403
        mockMvc.perform(put(BOOKING_BASE_URI + "/" + bookingId + "/checkout").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    /**
     * Utility method to perform a bad request and extract the validation errors as field->message pairs. This helps
     * verify that proper validation feedback is provided to clients.
     *
     * @param requestBuilder the MockMvc request builder for the bad request
     * @return Map of field names to their validation error messages
     * @throws Exception if the request fails or response parsing fails
     */
    private Map<String, String> performBadRequestAndGetValidationErrors(RequestBuilder requestBuilder)
            throws Exception {
        var mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto errorResponse = objectMapper.readValue(responseBody, new TypeReference<ErrorResponseDto>() {});

        return errorResponse.getValidationErrors()
                .stream()
                .collect(Collectors.toMap(ErrorResponseDto.ValidationError::getField,
                        ErrorResponseDto.ValidationError::getMessage));
    }

    private CheckInGuestDto createValidGuest(Long roomId) {
        CheckInGuestDto guest = new CheckInGuestDto();
        guest.setFirstName("John");
        guest.setLastName("Doe");
        guest.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guest.setRoomId(roomId);
        guest.setEmail("john.doe@example.com");
        guest.setPhoneNumber("+123456789");
        return guest;
    }

    private CheckInGuestDto createValidGuest2(Long roomId) {
        CheckInGuestDto guest = new CheckInGuestDto();
        guest.setFirstName("John");
        guest.setLastName("Dos");
        guest.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guest.setRoomId(roomId);
        guest.setEmail("john.dos@example.com");
        guest.setPhoneNumber("+12345678910");
        return guest;
    }

    @Test
    @DisplayName("POST /booking/create-booking -> Successfully creates booking with valid room IDs")
    public void createBooking_whenValidRequest_createsBookingAndReturnsDetails() throws Exception {
        // Given - Find 2 available rooms
        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(2).toList();
        if (availableRooms.size() < 2) {
            // Skip test if no suitable rooms found
            return;
        }

        LocalDate checkInDate = LocalDate.now().plusDays(10);
        LocalDate checkOutDate = LocalDate.now().plusDays(15);

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Jane");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("jane.doe.booking@example.com");
        guestInfo.setPhone("+43 123 456 7890");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(3);
        requestDto.setNumChildren(1);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(availableRooms.stream().map(Room::getId).toList());
        requestDto.setCreatedManually(true);

        // When
        String adminToken = getAdminAuthToken();
        var mvcResult = mockMvc.perform(post(BOOKING_CREATE_URI).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        CreateOrUpdateBookingResponseDto response =
                objectMapper.readValue(responseBody, CreateOrUpdateBookingResponseDto.class);

        // Then
        assertAll(() -> assertThat(response).isNotNull(), () -> assertThat(response.getBookingId()).isNotNull(),
                () -> assertThat(response.getGuestEmail()).isEqualTo("jane.doe.booking@example.com"),
                () -> assertThat(response.getStatus()).isEqualTo("CONFIRMED"),
                () -> assertThat(response.getCheckInDate()).isEqualTo(checkInDate),
                () -> assertThat(response.getCheckOutDate()).isEqualTo(checkOutDate),
                () -> assertThat(response.getAssignedRoomNumbers()).hasSize(2),
                () -> assertThat(response.getAssignedRoomNumbers())
                        .allMatch(roomNumber -> roomNumber != null && !roomNumber.isEmpty()));
    }

    @Test
    @DisplayName("POST /booking/create-booking -> Successfully creates booking with multiple room types")
    public void createBooking_whenMultipleRoomTypes_createsBookingWithAllSelectedTypes() throws Exception {
        // Given - Find 2 room types with available rooms
        List<RoomType> allRoomTypes = roomTypeRepository.findAll();
        if (allRoomTypes.size() < 2) {
            // Skip test if less than 2 room types in database
            return;
        }

        // Find room types that have at least 1 available room each
        List<RoomType> roomTypesWithRooms = new java.util.ArrayList<>();
        for (RoomType roomType : allRoomTypes) {
            boolean hasAvailableRoom = roomRepository.findAll()
                    .stream()
                    .anyMatch(r -> r.getRoomType().getId().equals(roomType.getId())
                            && r.getStatus() == RoomStatus.AVAILABLE);

            if (hasAvailableRoom) {
                roomTypesWithRooms.add(roomType);
                if (roomTypesWithRooms.size() == 2) {
                    break;
                }
            }
        }

        if (roomTypesWithRooms.size() < 2) {
            // Skip test if not enough room types with available rooms
            return;
        }

        RoomType type1 = roomTypesWithRooms.get(0);
        RoomType type2 = roomTypesWithRooms.get(1);

        LocalDate checkInDate = LocalDate.now().plusDays(20);
        LocalDate checkOutDate = LocalDate.now().plusDays(25);

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Multi");
        guestInfo.setLastName("TypeBooking");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("multi.type@example.com");
        guestInfo.setPhone("+43 999 888 7777");

        List<Room> type1Rooms = roomRepository.findAll()
                .stream()
                .filter(r -> r.getRoomType().getId().equals(type1.getId()) && r.getStatus() == RoomStatus.AVAILABLE)
                .limit(1)
                .toList();
        List<Room> type2Rooms = roomRepository.findAll()
                .stream()
                .filter(r -> r.getRoomType().getId().equals(type2.getId()) && r.getStatus() == RoomStatus.AVAILABLE)
                .limit(1)
                .toList();

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(4);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(type1Rooms.getFirst().getId(), type2Rooms.getFirst().getId()));
        requestDto.setCreatedManually(true);

        // When
        String adminToken = getAdminAuthToken();
        var mvcResult = mockMvc.perform(post(BOOKING_CREATE_URI).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        CreateOrUpdateBookingResponseDto response =
                objectMapper.readValue(responseBody, CreateOrUpdateBookingResponseDto.class);

        // Then
        assertAll(() -> assertThat(response).isNotNull(), () -> assertThat(response.getBookingId()).isNotNull(),
                () -> assertThat(response.getGuestEmail()).isEqualTo("multi.type@example.com"),
                () -> assertThat(response.getAssignedRoomNumbers()).hasSize(2));
    }

    @Test
    @DisplayName("POST /booking/create-booking -> Returns 400 when check-out date is before check-in date")
    public void createBooking_whenInvalidDates_returnsBadRequest() throws Exception {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(3); // Invalid: before check-in

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Invalid");
        guestInfo.setLastName("Dates");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("invalid@example.com");

        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(1).toList();
        if (availableRooms.isEmpty()) {
            // Skip test if no suitable rooms found
            return;
        }

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(availableRooms.getFirst().getId()));
        requestDto.setCreatedManually(true);

        // When & Then
        String adminToken = getAdminAuthToken();
        mockMvc.perform(post(BOOKING_CREATE_URI).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /booking/create-booking -> Returns 400 when missing required fields")
    public void createBooking_whenMissingRequiredFields_returnsBadRequest() throws Exception {
        // Given - Request with missing guestInfo (roomTypeSelections is now optional)
        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(LocalDate.now().plusDays(1));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(3));
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        // Missing guestInfo

        // When
        Map<String, String> validationErrors =
                performBadRequestAndGetValidationErrors(post(BOOKING_CREATE_URI).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                        .content(objectMapper.writeValueAsString(requestDto)));

        // Then
        assertThat(validationErrors).containsKey("guestInfo");
    }

    @Test
    @DisplayName("POST /booking/create-booking -> Returns 400 when neither room type selections nor room IDs provided")
    public void createBooking_whenNoRoomSelectionProvided_returnsBadRequest() throws Exception {
        // Given
        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Test");
        guestInfo.setLastName("User");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("test@example.com");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(LocalDate.now().plusDays(1));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(3));
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomTypeSelections(List.of()); // Empty list
        requestDto.setRoomIds(null);

        // When & Then
        String adminToken = getAdminAuthToken();
        var mvcResult = mockMvc
                .perform(post(BOOKING_CREATE_URI).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto errorResponse = objectMapper.readValue(responseBody, ErrorResponseDto.class);

        assertThat(errorResponse.getMessage()).contains("Room IDs are required for manual bookings");
    }

    @Test
    @DisplayName("POST /booking/create-booking -> Returns 400 when guest email is invalid")
    public void createBooking_whenInvalidEmail_returnsBadRequest() throws Exception {
        // Given
        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Test");
        guestInfo.setLastName("User");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("invalid-email"); // Invalid email format

        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(1).toList();
        if (availableRooms.isEmpty()) {
            // Skip test if no suitable rooms found
            return;
        }

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(LocalDate.now().plusDays(1));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(3));
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(availableRooms.getFirst().getId()));
        requestDto.setCreatedManually(true);

        // When
        Map<String, String> validationErrors =
                performBadRequestAndGetValidationErrors(post(BOOKING_CREATE_URI).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAuthToken())
                        .content(objectMapper.writeValueAsString(requestDto)));

        // Then
        assertAll(() -> assertThat(validationErrors).containsKey("guestInfo.email"),
                () -> assertThat(validationErrors.get("guestInfo.email")).contains("Email must be valid"));
    }

    // ===== Manual Booking Tests (with roomIds) =====

    @Test
    @DisplayName("POST /booking/create-booking -> Successfully creates manual booking with specific room IDs")
    public void createBooking_whenManualBookingWithRoomIds_createsBookingSuccessfully() throws Exception {
        // Given - Find 2 available rooms
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = LocalDate.now().plusDays(35);

        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(2).toList();

        if (availableRooms.size() < 2) {
            // Skip test if not enough available rooms
            return;
        }

        List<Long> roomIds = availableRooms.stream().map(Room::getId).toList();

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Manual");
        guestInfo.setLastName("Booking");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("manual.booking@example.com");
        guestInfo.setPhone("+43 555 666 7777");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(roomIds);
        requestDto.setCreatedManually(true);

        // When
        String adminToken = getAdminAuthToken();
        var mvcResult = mockMvc.perform(post(BOOKING_CREATE_URI).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        CreateOrUpdateBookingResponseDto response =
                objectMapper.readValue(responseBody, CreateOrUpdateBookingResponseDto.class);

        // Then
        assertAll(() -> assertThat(response).isNotNull(), () -> assertThat(response.getBookingId()).isNotNull(),
                () -> assertThat(response.getGuestEmail()).isEqualTo("manual.booking@example.com"),
                () -> assertThat(response.getStatus()).isEqualTo("CONFIRMED"),
                () -> assertThat(response.getCheckInDate()).isEqualTo(checkInDate),
                () -> assertThat(response.getCheckOutDate()).isEqualTo(checkOutDate),
                () -> assertThat(response.getAssignedRoomNumbers()).hasSize(2));
    }

    @Test
    @DisplayName("POST /booking/create-booking -> Succeeds when roomIds are provided (roomTypeSelections ignored)")
    public void createBooking_whenBothRoomSelectionsProvided_createsBookingSuccessfully() throws Exception {
        // Given
        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(2).toList();
        if (availableRooms.size() < 2) {
            // Skip test if not enough available rooms
            return;
        }

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Manual");
        guestInfo.setLastName("Booking");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("manual.booking2@example.com");

        List<RoomTypeSelectionDto> selections =
                availableRooms.stream().map(room -> new RoomTypeSelectionDto(room.getRoomType().getId(), 1)).toList();

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(LocalDate.now().plusDays(1));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(3));
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomTypeSelections(selections);
        requestDto.setRoomIds(availableRooms.stream().map(Room::getId).toList());
        requestDto.setCreatedManually(true);

        String adminToken = getAdminAuthToken();
        // When
        var mvcResult = mockMvc.perform(post(BOOKING_CREATE_URI).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        CreateOrUpdateBookingResponseDto response =
                objectMapper.readValue(responseBody, CreateOrUpdateBookingResponseDto.class);

        assertAll(() -> assertThat(response).isNotNull(), () -> assertThat(response.getBookingId()).isNotNull(),
                () -> assertThat(response.getAssignedRoomNumbers()).hasSize(2));
    }

    @Test
    @DisplayName("POST /booking/create-booking -> Returns 400 when manual booking room is not available")
    public void createBooking_whenManualBookingRoomNotAvailable_returnsBadRequest() throws Exception {
        // Given - Try to book a room that will be unavailable
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);

        // First create a booking to make rooms unavailable
        List<Long> bookedRoomIds = createValidBooking().subList(1, 3); // Get room IDs

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Conflict");
        guestInfo.setLastName("Booking");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("conflict@example.com");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(bookedRoomIds); // Try to book already booked rooms
        requestDto.setCreatedManually(true);

        String adminToken = getAdminAuthToken();
        // When & Then
        var mvcResult = mockMvc
                .perform(post(BOOKING_CREATE_URI).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto errorResponse = objectMapper.readValue(responseBody, ErrorResponseDto.class);

        assertThat(errorResponse.getMessage()).contains("not available for the selected dates");
    }

    @Test
    @DisplayName("POST /booking/create-booking -> Returns 400 when manual booking with non-existent room ID")
    public void createBooking_whenManualBookingWithInvalidRoomId_returnsBadRequest() throws Exception {
        // Given
        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Invalid");
        guestInfo.setLastName("Room");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("invalid.room@example.com");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(LocalDate.now().plusDays(40));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(45));
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(99999L)); // Non-existent room ID
        requestDto.setCreatedManually(true);

        String adminToken = getAdminAuthToken();
        // When & Then
        var mvcResult = mockMvc
                .perform(post(BOOKING_CREATE_URI).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto errorResponse = objectMapper.readValue(responseBody, ErrorResponseDto.class);

        assertThat(errorResponse.getMessage()).contains("Some rooms were not found");
    }

    @Test
    @DisplayName("GET /booking -> Returns list of booking summaries for authorized user")
    public void getAllBookings_whenAuthenticated_returnsBookingSummaries() throws Exception {
        // Given - Create a couple of bookings to ensure the list is not empty
        createValidBooking();
        String adminToken = getAdminAuthToken();

        // When & Then
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        String responseBody = mvcResult.getResponse().getContentAsString();
        List<BookingSummaryResponseDto> bookings = objectMapper.readValue(responseBody, new TypeReference<>() {});

        assertAll("Verify booking summary list", () -> assertThat(bookings).isNotNull(),
                () -> assertThat(bookings).isNotEmpty(), () -> assertThat(bookings.get(0).getGuestName()).isNotBlank(),
                () -> assertThat(bookings.get(0).getStatus()).isNotNull());
    }

    @Test
    @DisplayName("GET /booking -> Fails with 403 Forbidden for unauthenticated user")
    public void getAllBookings_whenUnauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get(BOOKING_BASE_URI)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /booking/{id} -> Returns detailed booking for authorized user")
    public void getBookingDetails_whenAuthenticatedAndBookingExists_returnsDetails() throws Exception {
        // --- GIVEN ---
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();
        String adminToken = getAdminAuthToken();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AssertionError("Test setup failed: Booking not found"));

        // --- WHEN ---
        var mvcResult = mockMvc.perform(
                get(BOOKING_BASE_URI + "/" + bookingId).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        // --- THEN ---
        String responseBody = mvcResult.getResponse().getContentAsString();
        DetailedBookingResponseDto bookingDetails =
                objectMapper.readValue(responseBody, DetailedBookingResponseDto.class);

        assertAll("Verify detailed booking response matches the entity data",
                () -> assertThat(bookingDetails).isNotNull(),

                () -> assertThat(bookingDetails.getBookingId()).isEqualTo(booking.getId()),
                () -> assertThat(bookingDetails.getStatus()).isEqualTo(booking.getStatus()),
                () -> assertThat(bookingDetails.getCheckInDate()).isEqualTo(booking.getCheckInDate()),
                () -> assertThat(bookingDetails.getCheckOutDate()).isEqualTo(booking.getCheckOutDate()),
                () -> assertThat(bookingDetails.getNumAdults()).isEqualTo(booking.getNumAdults()),

                () -> assertThat(bookingDetails.getPrimaryGuest()).isNotNull(),
                () -> assertThat(bookingDetails.getPrimaryGuest().getId()).isEqualTo(booking.getGuest().getId()),
                () -> assertThat(bookingDetails.getPrimaryGuest().getFirstName())
                        .isEqualTo(booking.getGuest().getFirstName()),
                () -> assertThat(bookingDetails.getPrimaryGuest().getLastName())
                        .isEqualTo(booking.getGuest().getLastName()),
                () -> assertThat(bookingDetails.getPrimaryGuest().getEmail()).isEqualTo(booking.getGuest().getEmail()),

                () -> assertThat(bookingDetails.getRooms()).isNotNull(),
                () -> assertThat(bookingDetails.getRooms()).hasSize(booking.getRooms().size()), () -> {
                    Set<String> expectedRoomNumbers =
                            booking.getRooms().stream().map(Room::getRoomNumber).collect(Collectors.toSet());

                    Set<String> actualRoomNumbers = bookingDetails.getRooms()
                            .stream()
                            .map(DetailedBookingResponseDto.RoomInfo::getRoomNumber)
                            .collect(Collectors.toSet());

                    assertThat(actualRoomNumbers).isEqualTo(expectedRoomNumbers);
                });
    }

    @Test
    @DisplayName("GET /booking/{id} -> Fails with 403 Forbidden for unauthenticated user")
    public void getBookingDetails_whenUnauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get(BOOKING_BASE_URI + "/1")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /booking/{id} -> Returns 404 Not Found for non-existent booking")
    public void getBookingDetails_whenBookingDoesNotExist_returnsNotFound() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();
        long nonExistentBookingId = 9999L;

        // When & Then
        mockMvc.perform(get(BOOKING_BASE_URI + "/" + nonExistentBookingId).header(HttpHeaders.AUTHORIZATION,
                "Bearer " + adminToken)).andExpect(status().isNotFound());
    }

    // ==================== GET /api/v1/booking/checkins/today/count ====================

    @Test
    @DisplayName("GET /booking/checkins/today/count -> Fails with 403 Forbidden for unauthenticated user")
    public void getTodaysCheckInCount_whenUnauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get(BOOKING_BASE_URI + "/checkins/today/count")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /booking/checkins/today/count -> Returns 0 when no bookings need check-in today")
    public void getTodaysCheckInCount_whenNoBookingsToday_returnsZero() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/checkins/today/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        // Then
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("GET /booking/checkins/today/count -> Returns correct count when bookings need check-in today")
    public void getTodaysCheckInCount_whenBookingsNeedCheckInToday_returnsCorrectCount() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();
        LocalDate today = LocalDate.now();

        RoomType roomType = roomTypeRepository.save(
                RoomType.builder().name("Standard").capacity(2).basePrice(java.math.BigDecimal.valueOf(100)).build());

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("checkin@test.com")
                .build());

        Room room1 = createAndSaveRoom("1301", 13, roomType);
        Room room2 = createAndSaveRoom("1302", 13, roomType);

        // Create 2 bookings with check-in today and CONFIRMED status
        bookingRepository.save(Booking.builder()
                .checkInDate(today)
                .checkOutDate(today.plusDays(2))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        bookingRepository.save(Booking.builder()
                .checkInDate(today)
                .checkOutDate(today.plusDays(1))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(room2))
                .numAdults(1)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/checkins/today/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        // Then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("GET /booking/checkins/today/count -> Excludes bookings with non-CONFIRMED/PENDING status")
    public void getTodaysCheckInCount_whenBookingsAlreadyCheckedIn_doesNotCountThem() throws Exception {
        // Given
        String employeeToken = getEmployeeAuthToken();
        LocalDate today = LocalDate.now();

        RoomType roomType = roomTypeRepository.save(
                RoomType.builder().name("Standard").capacity(2).basePrice(java.math.BigDecimal.valueOf(100)).build());

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("status@test.com")
                .build());

        Room room1 = createAndSaveRoom("1401", 14, roomType);
        Room room2 = createAndSaveRoom("1402", 14, roomType);
        Room room3 = createAndSaveRoom("1403", 14, roomType);

        // Create bookings with different statuses
        bookingRepository.save(Booking.builder()
                .checkInDate(today)
                .checkOutDate(today.plusDays(2))
                .guest(guest)
                .status(BookingStatus.CHECKED_IN)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        bookingRepository.save(Booking.builder()
                .checkInDate(today)
                .checkOutDate(today.plusDays(1))
                .guest(guest)
                .status(BookingStatus.CANCELLED)
                .rooms(Set.of(room2))
                .numAdults(1)
                .numChildren(0)
                .build());

        bookingRepository.save(Booking.builder()
                .checkInDate(today)
                .checkOutDate(today.plusDays(1))
                .guest(guest)
                .status(BookingStatus.CHECKED_OUT)
                .rooms(Set.of(room3))
                .numAdults(1)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/checkins/today/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        // Then - should be 0 because none are CONFIRMED or PENDING
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("GET /booking/checkins/today/count -> Excludes bookings with different check-in dates")
    public void getTodaysCheckInCount_whenBookingsHaveDifferentCheckInDates_doesNotCountThem() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();
        LocalDate today = LocalDate.now();

        RoomType roomType = roomTypeRepository.save(
                RoomType.builder().name("Standard").capacity(2).basePrice(java.math.BigDecimal.valueOf(100)).build());

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("dates@test.com")
                .build());

        Room room1 = createAndSaveRoom("1501", 15, roomType);
        Room room2 = createAndSaveRoom("1502", 15, roomType);

        // Past booking
        bookingRepository.save(Booking.builder()
                .checkInDate(today.minusDays(5))
                .checkOutDate(today.plusDays(1))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        // Future booking
        bookingRepository.save(Booking.builder()
                .checkInDate(today.plusDays(2))
                .checkOutDate(today.plusDays(5))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(room2))
                .numAdults(1)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/checkins/today/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        // Then - should be 0 because neither booking has check in date of today
        assertThat(count).isEqualTo(0L);
    }

    // ==================== GET /api/v1/booking/checkouts/today/count ====================

    @Test
    @DisplayName("GET /booking/checkouts/today/count -> Fails with 403 Forbidden for unauthenticated user")
    public void getTodaysCheckOutCount_whenUnauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get(BOOKING_BASE_URI + "/checkouts/today/count")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /booking/checkouts/today/count -> Returns 0 when no bookings need check-out today")
    public void getTodaysCheckOutCount_whenNoBookingsToday_returnsZero() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();

        // When
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/checkouts/today/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        // Then
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("GET /booking/checkouts/today/count -> Returns correct count when bookings need check-out today")
    public void getTodaysCheckOutCount_whenBookingsNeedCheckOutToday_returnsCorrectCount() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();
        LocalDate today = LocalDate.now();

        RoomType roomType = roomTypeRepository.save(
                RoomType.builder().name("Standard").capacity(2).basePrice(java.math.BigDecimal.valueOf(100)).build());

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("checkout@test.com")
                .build());

        Room room1 = createAndSaveRoom("1601", 16, roomType);
        Room room2 = createAndSaveRoom("1602", 16, roomType);

        // Create 2 bookings with check-out today and CHECKED_IN status
        bookingRepository.save(Booking.builder()
                .checkInDate(today.minusDays(3))
                .checkOutDate(today)
                .guest(guest)
                .status(BookingStatus.CHECKED_IN)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        bookingRepository.save(Booking.builder()
                .checkInDate(today.minusDays(1))
                .checkOutDate(today)
                .guest(guest)
                .status(BookingStatus.CHECKED_IN)
                .rooms(Set.of(room2))
                .numAdults(1)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/checkouts/today/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        // Then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("GET /booking/checkouts/today/count -> Excludes bookings with non-CHECKED_IN status")
    public void getTodaysCheckOutCount_whenBookingsNotCheckedIn_doesNotCountThem() throws Exception {
        // Given
        String employeeToken = getEmployeeAuthToken();
        LocalDate today = LocalDate.now();

        RoomType roomType = roomTypeRepository.save(
                RoomType.builder().name("Standard").capacity(2).basePrice(java.math.BigDecimal.valueOf(100)).build());

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("checkoutstatus@test.com")
                .build());

        Room room1 = createAndSaveRoom("1701", 17, roomType);
        Room room2 = createAndSaveRoom("1702", 17, roomType);
        Room room3 = createAndSaveRoom("1703", 17, roomType);

        // Create bookings with different statuses (all with checkout date today)
        bookingRepository.save(Booking.builder()
                .checkInDate(today.minusDays(2))
                .checkOutDate(today)
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        bookingRepository.save(Booking.builder()
                .checkInDate(today.minusDays(1))
                .checkOutDate(today)
                .guest(guest)
                .status(BookingStatus.CANCELLED)
                .rooms(Set.of(room2))
                .numAdults(1)
                .numChildren(0)
                .build());

        bookingRepository.save(Booking.builder()
                .checkInDate(today.minusDays(3))
                .checkOutDate(today)
                .guest(guest)
                .status(BookingStatus.CHECKED_OUT)
                .rooms(Set.of(room3))
                .numAdults(1)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/checkouts/today/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        // Then - should be 0 because none are CHECKED_IN
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("GET /booking/checkouts/today/count -> Excludes bookings with different check-out dates")
    public void getTodaysCheckOutCount_whenBookingsHaveDifferentCheckOutDates_doesNotCountThem() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();
        LocalDate today = LocalDate.now();

        RoomType roomType = roomTypeRepository.save(
                RoomType.builder().name("Standard").capacity(2).basePrice(java.math.BigDecimal.valueOf(100)).build());

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("checkoutdates@test.com")
                .build());

        Room room1 = createAndSaveRoom("1801", 18, roomType);
        Room room2 = createAndSaveRoom("1802", 18, roomType);

        // Past checkout (yesterday)
        bookingRepository.save(Booking.builder()
                .checkInDate(today.minusDays(5))
                .checkOutDate(today.minusDays(1))
                .guest(guest)
                .status(BookingStatus.CHECKED_IN)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        // Future checkout (tomorrow)
        bookingRepository.save(Booking.builder()
                .checkInDate(today.minusDays(2))
                .checkOutDate(today.plusDays(1))
                .guest(guest)
                .status(BookingStatus.CHECKED_IN)
                .rooms(Set.of(room2))
                .numAdults(1)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/checkouts/today/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        // Then - should be 0 because neither booking has check-out date of today
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("GET /booking/checkouts/today/count -> Employee can access endpoint")
    public void getTodaysCheckOutCount_whenEmployee_returnsCount() throws Exception {
        // Given
        String employeeToken = getEmployeeAuthToken();
        LocalDate today = LocalDate.now();

        RoomType roomType = roomTypeRepository.save(
                RoomType.builder().name("Standard").capacity(2).basePrice(java.math.BigDecimal.valueOf(100)).build());

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("employeecheckout@test.com")
                .build());

        Room room = createAndSaveRoom("1901", 19, roomType);

        bookingRepository.save(Booking.builder()
                .checkInDate(today.minusDays(2))
                .checkOutDate(today)
                .guest(guest)
                .status(BookingStatus.CHECKED_IN)
                .rooms(Set.of(room))
                .numAdults(2)
                .numChildren(0)
                .build());

        // When
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/checkouts/today/count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        Long count = objectMapper.readValue(responseBody, Long.class);

        // Then
        assertThat(count).isEqualTo(1L);
    }

    // ==================== PUT /api/v1/booking (Update Booking) ====================

    @Test
    @DisplayName("PUT /booking -> Successfully updates staff booking with valid data")
    public void updateBooking_whenValidStaffBooking_updatesSuccessfully() throws Exception {
        // Given - Create a manual (staff) booking
        LocalDate initialCheckIn = LocalDate.now().plusDays(10);
        LocalDate initialCheckOut = LocalDate.now().plusDays(15);

        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(2).toList();

        if (availableRooms.size() < 2) {
            return; // Skip test if not enough rooms
        }

        Room room1 = availableRooms.get(0);
        Room room2 = availableRooms.get(1);

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Update")
                .lastName("Test")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .email("update@test.com")
                .build());

        Booking booking = bookingRepository.save(Booking.builder()
                .checkInDate(initialCheckIn)
                .checkOutDate(initialCheckOut)
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .createdManually(true)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        // Prepare update request with new dates and different room
        LocalDate newCheckIn = LocalDate.now().plusDays(20);
        LocalDate newCheckOut = LocalDate.now().plusDays(25);

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Update");
        guestInfo.setLastName("Test");
        guestInfo.setDateOfBirth(LocalDate.of(1985, 5, 15));
        guestInfo.setEmail("updated@test.com");
        guestInfo.setPhone("+43 111 222 333");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(newCheckIn);
        requestDto.setCheckOutDate(newCheckOut);
        requestDto.setNumAdults(3);
        requestDto.setNumChildren(1);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(room2.getId()));

        // When
        mockMvc.perform(put(BOOKING_BASE_URI).param("bookingId", booking.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk());

        // Then - Verify booking was updated
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertAll("Verify booking was updated", () -> assertThat(updatedBooking.getCheckInDate()).isEqualTo(newCheckIn),
                () -> assertThat(updatedBooking.getCheckOutDate()).isEqualTo(newCheckOut),
                () -> assertThat(updatedBooking.getNumAdults()).isEqualTo(3),
                () -> assertThat(updatedBooking.getNumChildren()).isEqualTo(1),
                () -> assertThat(updatedBooking.getGuest().getEmail()).isEqualTo("updated@test.com"),
                () -> assertThat(updatedBooking.getRooms()).hasSize(1),
                () -> assertThat(updatedBooking.getRooms().iterator().next().getId()).isEqualTo(room2.getId()));
    }

    @Test
    @DisplayName("PUT /booking -> Returns 404 when booking does not exist")
    public void updateBooking_whenBookingNotFound_returnsNotFound() throws Exception {
        // Given
        Long nonExistentBookingId = 99999L;

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Test");
        guestInfo.setLastName("User");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("test@example.com");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(LocalDate.now().plusDays(1));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(3));
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(101L));

        // When & Then
        mockMvc.perform(put(BOOKING_BASE_URI).param("bookingId", nonExistentBookingId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getEmployeeAuthToken())
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /booking -> Returns 400 when booking is not a staff booking")
    public void updateBooking_whenNotStaffBooking_returnsBadRequest() throws Exception {
        // Given - Create a customer booking (createdManually = false)
        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Customer")
                .lastName("Booking")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("customer@test.com")
                .build());

        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(1).toList();

        if (availableRooms.isEmpty()) {
            return;
        }

        Booking customerBooking = bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(10))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .createdManually(false) // Customer booking
                .rooms(Set.of(availableRooms.get(0)))
                .numAdults(2)
                .numChildren(0)
                .build());

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Customer");
        guestInfo.setLastName("Booking");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("customer@test.com");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(LocalDate.now().plusDays(6));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(11));
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(availableRooms.get(0).getId()));

        // When & Then
        var mvcResult = mockMvc
                .perform(put(BOOKING_BASE_URI).param("bookingId", customerBooking.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto errorResponse = objectMapper.readValue(responseBody, ErrorResponseDto.class);

        assertThat(errorResponse.getMessage()).contains("Only staff bookings can be updated");
    }

    @Test
    @DisplayName("PUT /booking -> Returns 400 when check-out date is before check-in date")
    public void updateBooking_whenInvalidDates_returnsBadRequest() throws Exception {
        // Given - Create a staff booking
        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Date")
                .lastName("Test")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("datetest@test.com")
                .build());

        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(1).toList();

        if (availableRooms.isEmpty()) {
            return;
        }

        Booking booking = bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(10))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .createdManually(true)
                .rooms(Set.of(availableRooms.get(0)))
                .numAdults(2)
                .numChildren(0)
                .build());

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Date");
        guestInfo.setLastName("Test");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("datetest@test.com");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(LocalDate.now().plusDays(10)); // Invalid: after check-out
        requestDto.setCheckOutDate(LocalDate.now().plusDays(8));
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(availableRooms.get(0).getId()));

        // When & Then
        var mvcResult = mockMvc
                .perform(put(BOOKING_BASE_URI).param("bookingId", booking.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto errorResponse = objectMapper.readValue(responseBody, ErrorResponseDto.class);

        assertThat(errorResponse.getMessage()).contains("Check-out date must be after check-in date");
    }

    @Test
    @DisplayName("PUT /booking -> Returns 400 when guest is under 18 years old")
    public void updateBooking_whenGuestUnder18_returnsBadRequest() throws Exception {
        // Given - Create a staff booking
        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Adult")
                .lastName("Guest")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("adult@test.com")
                .build());

        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(1).toList();

        if (availableRooms.isEmpty()) {
            return;
        }

        Booking booking = bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(10))
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .createdManually(true)
                .rooms(Set.of(availableRooms.get(0)))
                .numAdults(2)
                .numChildren(0)
                .build());

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Young");
        guestInfo.setLastName("Guest");
        guestInfo.setDateOfBirth(LocalDate.now().minusYears(17)); // Under 18
        guestInfo.setEmail("young@test.com");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(LocalDate.now().plusDays(5));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(10));
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(availableRooms.get(0).getId()));

        // When & Then
        var mvcResult = mockMvc
                .perform(put(BOOKING_BASE_URI).param("bookingId", booking.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto errorResponse = objectMapper.readValue(responseBody, ErrorResponseDto.class);

        assertThat(errorResponse.getMessage()).contains("Booking guest must be at least 18 years old");
    }

    @Test
    @DisplayName("PUT /booking -> Successfully updates booking while keeping the same room")
    public void updateBooking_whenKeepingSameRoom_updatesSuccessfully() throws Exception {
        // Given - Create a staff booking
        LocalDate initialCheckIn = LocalDate.now().plusDays(10);
        LocalDate initialCheckOut = LocalDate.now().plusDays(15);

        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(1).toList();

        if (availableRooms.isEmpty()) {
            return;
        }

        Room room = availableRooms.get(0);

        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Same")
                .lastName("Room")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("sameroom@test.com")
                .build());

        Booking booking = bookingRepository.save(Booking.builder()
                .checkInDate(initialCheckIn)
                .checkOutDate(initialCheckOut)
                .guest(guest)
                .status(BookingStatus.CONFIRMED)
                .createdManually(true)
                .rooms(Set.of(room))
                .numAdults(2)
                .numChildren(0)
                .build());

        // Update with new dates but same room
        LocalDate newCheckIn = LocalDate.now().plusDays(20);
        LocalDate newCheckOut = LocalDate.now().plusDays(25);

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Same");
        guestInfo.setLastName("Room");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("sameroom@test.com");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(newCheckIn);
        requestDto.setCheckOutDate(newCheckOut);
        requestDto.setNumAdults(3);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(room.getId())); // Same room

        // When
        mockMvc.perform(put(BOOKING_BASE_URI).param("bookingId", booking.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk());

        // Then - Verify booking was updated
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertAll("Verify booking was updated with same room",
                () -> assertThat(updatedBooking.getCheckInDate()).isEqualTo(newCheckIn),
                () -> assertThat(updatedBooking.getCheckOutDate()).isEqualTo(newCheckOut),
                () -> assertThat(updatedBooking.getNumAdults()).isEqualTo(3),
                () -> assertThat(updatedBooking.getRooms()).hasSize(1),
                () -> assertThat(updatedBooking.getRooms().iterator().next().getId()).isEqualTo(room.getId()));
    }

    @Test
    @DisplayName("PUT /booking -> Returns 400 when updating with room that is booked by another booking")
    public void updateBooking_whenRoomBookedByAnother_returnsBadRequest() throws Exception {
        // Given - Create two staff bookings
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = LocalDate.now().plusDays(15);

        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(2).toList();

        if (availableRooms.size() < 2) {
            return;
        }

        Room room1 = availableRooms.get(0);
        Room room2 = availableRooms.get(1);

        Guest guest1 = guestRepository.save(Guest.builder()
                .firstName("First")
                .lastName("Booking")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("first@test.com")
                .build());

        Guest guest2 = guestRepository.save(Guest.builder()
                .firstName("Second")
                .lastName("Booking")
                .dateOfBirth(LocalDate.of(1985, 5, 10))
                .email("second@test.com")
                .build());

        // First booking with room1
        Booking booking1 = bookingRepository.save(Booking.builder()
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .guest(guest1)
                .status(BookingStatus.CONFIRMED)
                .createdManually(true)
                .rooms(Set.of(room1))
                .numAdults(2)
                .numChildren(0)
                .build());

        // Second booking with room2 (will try to change to room1)
        Booking booking2 = bookingRepository.save(Booking.builder()
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .guest(guest2)
                .status(BookingStatus.CONFIRMED)
                .createdManually(true)
                .rooms(Set.of(room2))
                .numAdults(2)
                .numChildren(0)
                .build());

        // Try to update booking2 to use room1 (which is already booked by booking1)
        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Second");
        guestInfo.setLastName("Booking");
        guestInfo.setDateOfBirth(LocalDate.of(1985, 5, 10));
        guestInfo.setEmail("second@test.com");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkIn);
        requestDto.setCheckOutDate(checkOut);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(room1.getId())); // Try to take room1

        // When & Then
        var mvcResult = mockMvc
                .perform(put(BOOKING_BASE_URI).param("bookingId", booking2.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        ErrorResponseDto errorResponse = objectMapper.readValue(responseBody, ErrorResponseDto.class);

        assertThat(errorResponse.getMessage()).contains("not available for the selected dates");
    }

    @Test
    public void getSpecialBookingConstraints_whenConstrainedRoomsExistAndAuthorized_returnRoomIds() throws Exception {
        final var guest1 = guestRepository.save(Guest.builder()
                .firstName("guest")
                .lastName("one")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("one@guest.local")
                .build());
        final var guest2 = guestRepository.save(Guest.builder()
                .firstName("guest")
                .lastName("two")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("two@guest.local")
                .build());
        final var guest3 = guestRepository.save(Guest.builder()
                .firstName("guest")
                .lastName("three")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("three@guest.local")
                .build());

        final var roomType = roomTypeRepository
                .save(RoomType.builder().name("room type").capacity(1).basePrice(BigDecimal.ONE).build());

        final var room1 = roomRepository.save(Room.builder()
                .roomNumber("100")
                .floor(1)
                .status(RoomStatus.OCCUPIED)
                .active(true)
                .svgPoints(List.of(new Point(0.0, 0.0), new Point(1.0, 0.0), new Point(0.5, 1.0)))
                .roomType(roomType)
                .build());
        final var room2 = roomRepository.save(Room.builder()
                .roomNumber("101")
                .floor(1)
                .status(RoomStatus.OCCUPIED)
                .active(true)
                .svgPoints(List.of(new Point(10.0, 10.0), new Point(11.0, 10.0), new Point(10.5, 11.0)))
                .roomType(roomType)
                .build());
        final var room3 = roomRepository.save(Room.builder()
                .roomNumber("102")
                .floor(1)
                .status(RoomStatus.OCCUPIED)
                .active(true)
                .svgPoints(List.of(new Point(20.0, 20.0), new Point(21.0, 20.0), new Point(20.5, 21.0)))
                .roomType(roomType)
                .build());

        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.of(2025, 1, 2))
                .checkOutDate(LocalDate.of(2025, 1, 10))
                .numAdults(1)
                .status(BookingStatus.CHECKED_IN)
                .earlyCheckIn(true)
                .guest(guest1)
                .rooms(Set.of(room1))
                .build());
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.of(2025, 1, 1))
                .checkOutDate(LocalDate.of(2025, 1, 2))
                .numAdults(1)
                .status(BookingStatus.CHECKED_IN)
                .lateCheckOut(true)
                .guest(guest2)
                .rooms(Set.of(room2))
                .build());
        bookingRepository.save(Booking.builder()
                .checkInDate(LocalDate.of(2025, 1, 1))
                .checkOutDate(LocalDate.of(2025, 1, 10))
                .numAdults(1)
                .status(BookingStatus.CHECKED_IN)
                .guest(guest3)
                .rooms(Set.of(room3))
                .build());

        final MvcResult mvcResult =
                mockMvc.perform(get(BOOKING_BASE_URI + "/constrained-rooms/2025/1/2").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + getAdminAuthToken())).andExpect(status().isOk()).andReturn();
        final String responseString = mvcResult.getResponse().getContentAsString();
        final SpecialBookingConstraintsDto result = objectMapper.readValue(responseString, new TypeReference<>() {});

        assertAll(() -> assertThat(result.getEarlyCheckInRoomIds()).contains(room1.getId()),
                () -> assertThat(result.getEarlyCheckInRoomIds()).doesNotContain(room3.getId()),
                () -> assertThat(result.getLateCheckOutRoomIds()).contains(room2.getId()),
                () -> assertThat(result.getLateCheckOutRoomIds()).doesNotContain(room3.getId()));
    }

    @Test
    public void getSpecialBookingConstraints_whenNotAuthorized_returnForbidden() throws Exception {
        mockMvc.perform(get(BOOKING_BASE_URI + "/constrained-rooms/2025/1/1")).andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/booking/begin-booking ====================

    @Test
    @DisplayName("POST /booking/begin-booking -> Successfully creates Stripe session with valid booking data")
    public void beginBooking_whenValidData_returnsStripeSessionUrl() throws Exception {
        // Given
        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(2).toList();

        if (availableRooms.size() < 2) {
            return; // Skip if not enough rooms
        }

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Stripe");
        guestInfo.setLastName("Customer");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("stripe.customer@example.com");
        guestInfo.setPhone("+43 555 123 4567");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(LocalDate.now().plusDays(5));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(10));
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(availableRooms.stream().map(Room::getId).toList());

        // When
        var mvcResult =
                mockMvc.perform(post(BOOKING_BASE_URI + "/begin-booking").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk()).andReturn();

        // Then
        String responseBody = mvcResult.getResponse().getContentAsString();
        Map<String, String> response = objectMapper.readValue(responseBody, new TypeReference<>() {});

        assertAll(() -> assertThat(response).containsKey("url"), () -> assertThat(response.get("url")).isNotNull(),
                () -> assertThat(response.get("url")).startsWith("https://"));
    }

    @Test
    @DisplayName("POST /booking/begin-booking -> Returns 400 when check-out date is before check-in date")
    public void beginBooking_whenInvalidDates_returnsBadRequest() throws Exception {
        // Given
        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(1).toList();

        assumeTrue(!availableRooms.isEmpty(), "No available rooms for test");

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Invalid");
        guestInfo.setLastName("Dates");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("invalid.dates@example.com");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(LocalDate.now().plusDays(10));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(5)); // Invalid: before check-in
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(availableRooms.get(0).getId()));

        // When & Then
        mockMvc.perform(post(BOOKING_BASE_URI + "/begin-booking").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /booking/begin-booking -> Returns 400 when guest info is missing")
    public void beginBooking_whenMissingGuestInfo_returnsBadRequest() throws Exception {
        // Given
        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(1).toList();

        if (availableRooms.isEmpty()) {
            return;
        }

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(LocalDate.now().plusDays(5));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(10));
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setRoomIds(List.of(availableRooms.get(0).getId()));
        // Missing guestInfo

        // When
        Map<String, String> validationErrors = performBadRequestAndGetValidationErrors(
                post(BOOKING_BASE_URI + "/begin-booking").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)));

        // Then
        assertThat(validationErrors).containsKey("guestInfo");
    }

    // ==================== GET /api/v1/booking/checkout-session ====================

    @Test
    @DisplayName("GET /booking/checkout-session -> Returns booking details for valid session ID")
    public void getBookingFromSession_whenValidSessionId_returnsBookingDetails() throws Exception {
        // Given - Create a booking first via begin-booking to get a session
        List<Room> availableRooms =
                roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(1).toList();

        if (availableRooms.isEmpty()) {
            return;
        }

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Session");
        guestInfo.setLastName("Test");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("session.test@example.com");
        guestInfo.setPhone("+43 111 222 333");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(LocalDate.now().plusDays(5));
        requestDto.setCheckOutDate(LocalDate.now().plusDays(10));
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(availableRooms.get(0).getId()));

        // Begin booking to get session URL
        var beginResult =
                mockMvc.perform(post(BOOKING_BASE_URI + "/begin-booking").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))).andExpect(status().isOk()).andReturn();

        String beginResponseBody = beginResult.getResponse().getContentAsString();
        Map<String, String> beginResponse = objectMapper.readValue(beginResponseBody, new TypeReference<>() {});
        String sessionUrl = beginResponse.get("url");

        // Extract session ID from URL (format: https://...?session_id=...)
        String sessionId = sessionUrl.substring(sessionUrl.lastIndexOf("=") + 1);

        // When - Get booking from session
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/checkout-session").param("session_id", sessionId))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseBody = mvcResult.getResponse().getContentAsString();
        CreateOrUpdateBookingResponseDto bookingResponse =
                objectMapper.readValue(responseBody, CreateOrUpdateBookingResponseDto.class);

        assertAll(() -> assertThat(bookingResponse).isNotNull(),
                () -> assertThat(bookingResponse.getBookingId()).isNotNull(),
                () -> assertThat(bookingResponse.getGuestEmail()).isEqualTo("session.test@example.com"),
                () -> assertThat(bookingResponse.getStatus()).isEqualTo("PENDING"));
    }

    @Test
    @DisplayName("GET /booking/checkout-session -> Returns 400 for invalid session ID")
    public void getBookingFromSession_whenInvalidSessionId_returnsBadRequest() throws Exception {
        // Given
        String invalidSessionId = "cs_test_invalid_session_id_12345";

        // When & Then
        mockMvc.perform(get(BOOKING_BASE_URI + "/checkout-session").param("session_id", invalidSessionId))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /api/v1/booking/{bookingId}/can-checkout ====================

    @Test
    @DisplayName("GET /booking/{bookingId}/can-checkout -> Returns true when booking can be checked out (admin)")
    public void canCheckout_whenAdminAndBookingCanCheckout_returnsTrue() throws Exception {
        // Given - Create and check in a booking
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();
        String adminToken = getAdminAuthToken();

        CheckInGuestDto guest1 = createValidGuest(ids.get(1));
        CheckInGuestDto guest2 = createValidGuest2(ids.get(2));

        List<CheckInGuestDto> guests = List.of(guest1, guest2);
        String guestsJson = objectMapper.writeValueAsString(guests);

        MockMultipartFile guestsPart =
                new MockMultipartFile("guests", "", MediaType.APPLICATION_JSON_VALUE, guestsJson.getBytes());
        MockMultipartFile guestFile0 =
                new MockMultipartFile("guestFiles_0", "id1.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-1".getBytes());
        MockMultipartFile guestFile1 =
                new MockMultipartFile("guestFiles_1", "id2.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-image-2".getBytes());

        // Check in the booking
        mockMvc.perform(multipart(BOOKING_BASE_URI + "/" + bookingId + "/checkin").file(guestsPart)
                .file(guestFile0)
                .file(guestFile1)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk());

        // When
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/" + bookingId + "/can-checkout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk()).andReturn();

        // Then
        String responseBody = mvcResult.getResponse().getContentAsString();
        Boolean canCheckout = objectMapper.readValue(responseBody, Boolean.class);

        assertThat(canCheckout).isTrue();
    }

    @Test
    @DisplayName("GET /booking/{bookingId}/can-checkout -> Returns false when booking cannot be checked out")
    public void canCheckout_whenBookingCannotCheckout_returnsFalse() throws Exception {
        // Given - Create booking but don't check in
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();
        String adminToken = getAdminAuthToken();

        // When - Try to check if can checkout (booking is CONFIRMED, not CHECKED_IN)
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/" + bookingId + "/can-checkout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk()).andReturn();

        // Then
        String responseBody = mvcResult.getResponse().getContentAsString();
        Boolean canCheckout = objectMapper.readValue(responseBody, Boolean.class);

        assertThat(canCheckout).isFalse();
    }

    @Test
    @DisplayName("GET /booking/{bookingId}/can-checkout -> Allows employee to check checkout eligibility")
    public void canCheckout_whenEmployee_returnsResult() throws Exception {
        // Given
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();
        String employeeToken = getEmployeeAuthToken();

        // When
        var mvcResult = mockMvc.perform(get(BOOKING_BASE_URI + "/" + bookingId + "/can-checkout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)).andExpect(status().isOk()).andReturn();

        // Then
        String responseBody = mvcResult.getResponse().getContentAsString();
        Boolean canCheckout = objectMapper.readValue(responseBody, Boolean.class);

        assertThat(canCheckout).isNotNull();
    }

    @Test
    @DisplayName("GET /booking/{bookingId}/can-checkout -> Returns 403 Forbidden for unauthenticated user")
    public void canCheckout_whenUnauthenticated_returnsForbidden() throws Exception {
        // Given
        List<Long> ids = createValidBooking();
        Long bookingId = ids.getFirst();

        // When & Then
        mockMvc.perform(get(BOOKING_BASE_URI + "/" + bookingId + "/can-checkout")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /booking/{bookingId}/can-checkout -> Returns 404 for non-existent booking")
    public void canCheckout_whenBookingNotFound_returnsNotFound() throws Exception {
        // Given
        String adminToken = getAdminAuthToken();
        Long nonExistentBookingId = 99999L;

        // When & Then
        mockMvc.perform(get(BOOKING_BASE_URI + "/" + nonExistentBookingId + "/can-checkout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isNotFound());
    }

}
