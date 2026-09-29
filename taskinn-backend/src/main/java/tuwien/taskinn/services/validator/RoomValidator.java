package tuwien.taskinn.services.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.request.RoomRequestDto;
import tuwien.taskinn.entity.Point;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.repo.RoomRepository;

import java.util.List;
import java.util.Objects;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class RoomValidator {

    private final RoomRepository roomRepository;

    /**
     * Validates that the room number is unique among active rooms. Inactive rooms are excluded from this check to allow
     * room number reuse.
     *
     * @param roomId The ID of the room being updated (null if creating a new room).
     * @param roomNumber The room number to validate.
     * @throws BusinessValidationException if the room number is already in use by another active room.
     */
    public void validateRoomNumberUniqueness(Long roomId, String roomNumber) {
        boolean exists = roomRepository.existsActiveRoomByNumber(roomNumber, roomId);

        if (exists) {
            throw new BusinessValidationException("roomNumber",
                    "Room number '" + roomNumber + "' is already in use by another active room.");
        }
    }

    /**
     * Validates that the capacity override matches the sum of single and double bed overrides.
     *
     * @param dto The RoomRequestDto containing the override values.
     * @param roomType The existing Room Type entity.
     * @throws BusinessValidationException if the capacity override does not match the sum of bed overrides.
     */
    public void validateRoomUpdate(RoomRequestDto dto, RoomType roomType) {
        Integer effectiveCapacity =
                dto.getCapacityOverride() != null ? dto.getCapacityOverride() : roomType.getCapacity();
        Integer effectiveSingleBeds = dto.getSingleBedOverride() != null ? dto.getSingleBedOverride()
                : (roomType.getSingleBed() != null ? roomType.getSingleBed() : 0);
        Integer effectiveDoubleBeds = dto.getDoubleBedOverride() != null ? dto.getDoubleBedOverride()
                : (roomType.getDoubleBed() != null ? roomType.getDoubleBed() : 0);

        if (effectiveCapacity != effectiveSingleBeds + 2 * effectiveDoubleBeds) {
            throw new BusinessValidationException("capacityOverride",
                    "Capacity must equal the sum of single and double beds.");
        }

    }

    /**
     * Validates that the room number follows the floor prefix convention. Room numbers must be: floor prefix + minimum
     * 2 digits (e.g., floor 1: 101, 102, 1001; floor 2: 201, 202, 2001; floor 10: 1001, 1002, 10001).
     *
     * @param roomNumber The room number to validate.
     * @param floor The floor number.
     * @throws BusinessValidationException if the room number doesn't follow the pattern.
     */
    public void validateRoomNumberFloorPrefix(String roomNumber, Integer floor) {
        if (roomNumber == null || roomNumber.isEmpty()) {
            throw new BusinessValidationException("roomNumber", "Room number cannot be null or empty.");
        }

        if (floor == null) {
            throw new BusinessValidationException("floor", "Floor number cannot be null.");
        }

        String floorPrefix = String.valueOf(floor);

        if (!roomNumber.startsWith(floorPrefix)) {
            throw new BusinessValidationException("roomNumber", "Room number '" + roomNumber
                    + "' must start with floor number '" + floor + "' (e.g., " + floor + "01, " + floor + "02).");
        }

        // Ensure there are at least 2 digits after the floor prefix
        int minLength = floorPrefix.length() + 2;
        if (roomNumber.length() < minLength) {
            throw new BusinessValidationException("roomNumber",
                    "Room number '" + roomNumber + "' must have at least " + minLength + " digits: floor prefix '"
                            + floor + "' + minimum 2 room digits (e.g., " + floor + "01, " + floor + "02).");
        }

        // Verify the suffix (after floor prefix) contains only digits
        String roomSuffix = roomNumber.substring(floorPrefix.length());
        if (!roomSuffix.matches("\\d+")) {
            throw new BusinessValidationException("roomNumber",
                    "Room number '" + roomNumber + "' must have only digits after floor prefix '" + floor + "'.");
        }
    }

    /**
     * Validates that the checkout date is after the checkin date.
     *
     * @param checkInDate The check-in date
     * @param checkOutDate The check-out date
     * @throws BusinessValidationException if checkout date is not after checkin date
     */
    public void validateDateRange(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null) {
            throw new BusinessValidationException("checkInDate", "Check-in date cannot be null");
        }
        if (checkOutDate == null) {
            throw new BusinessValidationException("checkOutDate", "Check-out date cannot be null");
        }
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new BusinessValidationException("checkOutDate", "Check-out date must be after check-in date");
        }
    }

    /**
     * Checks if the proposed room's SVG points overlap with any other room on the same floor. Overlap is detected if
     * any vertex of the new room is inside an existing room, or any vertex of an existing room is inside the new room.
     *
     * @param roomId The ID of the room being updated (null if creating a new room).
     * @param floor The floor number.
     * @param newSvgPoints The SVG points of the room to be created or updated.
     * @throws BusinessValidationException if an overlap is detected.
     */
    public void validateOverlap(Long roomId, Integer floor, List<Point> newSvgPoints) {
        // 1. Fetch all existing rooms on the same floor
        List<Room> existingRoomsOnFloor = roomRepository.findAllByFloor(floor);

        for (Room existingRoom : existingRoomsOnFloor) {
            // Skip the room itself when validating an update (PUT request)
            // Also skip inactive rooms
            if ((roomId != null && Objects.equals(existingRoom.getId(), roomId)) || !existingRoom.isActive()) {
                continue;
            }

            List<Point> existingSvgPoints = existingRoom.getSvgPoints();

            // 2. Check for Overlap (Vertex-in-Polygon Check)
            if (checkIfPolygonsOverlap(newSvgPoints, existingSvgPoints)) {
                String existingRoomNumber = existingRoom.getRoomNumber();
                throw new BusinessValidationException("svgPoints",
                        "Overlap detected: The proposed room's layout overlaps with existing room " + existingRoomNumber
                                + " on floor " + floor + ".");
            }
        }
    }

    private boolean checkIfPolygonsOverlap(List<Point> polyA, List<Point> polyB) {
        if (isOverlapping(polyA, polyB))
            return true;

        return isOverlapping(polyB, polyA);
    }

    private boolean isOverlapping(List<Point> source, List<Point> target) {
        boolean anyInside = false;
        boolean allOnBoundary = true;

        for (Point p : source) {
            int status = getPointPolygonStatus(p, target);

            if (status == 1) { // INSIDE
                anyInside = true;
                break;
            }

            if (status == -1) { // OUTSIDE
                allOnBoundary = false;
            }
        }

        // Overlap detected if:
        // 1. Any point is strictly inside the other polygon
        // 2. ALL points are on the boundary (implies identical polygons)
        return anyInside || (allOnBoundary && !source.isEmpty());
    }

    /**
     * Returns location status of point p relative to polygon. 1 = Inside 0 = On Boundary -1 = Outside
     */
    private int getPointPolygonStatus(Point p, List<Point> polygon) {
        if (polygon == null || polygon.size() < 3)
            return -1;

        // 1. Explicitly check if the point lies ON any edge (Boundary Check)
        for (int i = 0; i < polygon.size(); i++) {
            Point p1 = polygon.get(i);
            Point p2 = polygon.get((i + 1) % polygon.size());
            if (isPointOnLineSegment(p, p1, p2)) {
                return 0; // On Boundary
            }
        }

        // 2. Ray Casting Algorithm (Strict Interior Check)
        int intersections = getIntersections(p, polygon);

        return (intersections % 2 != 0) ? 1 : -1;
    }

    private static int getIntersections(Point p, List<Point> polygon) {
        int intersections = 0;
        int n = polygon.size();

        for (int i = 0; i < n; i++) {
            Point p1 = polygon.get(i);
            Point p2 = polygon.get((i + 1) % n);

            // Check if the ray from p (to the right) crosses the edge (p1, p2)
            if (((p1.getY() <= p.getY() && p.getY() < p2.getY()) || (p2.getY() <= p.getY() && p.getY() < p1.getY()))
                    && (p.getX() < (p2.getX() - p1.getX()) * (p.getY() - p1.getY()) / (p2.getY() - p1.getY())
                            + p1.getX())) {
                intersections++;
            }
        }
        return intersections;
    }

    /**
     * Checks if point p lies on the line segment defined by a and b.
     */
    private boolean isPointOnLineSegment(Point p, Point a, Point b) {
        double epsilon = 0.000001; // Tolerance for floating point comparisons

        // Check if p is within the bounding box of the segment
        if (p.getX() < Math.min(a.getX(), b.getX()) - epsilon || p.getX() > Math.max(a.getX(), b.getX()) + epsilon
                || p.getY() < Math.min(a.getY(), b.getY()) - epsilon
                || p.getY() > Math.max(a.getY(), b.getY()) + epsilon) {
            return false;
        }

        // Check collinearity using cross product: (y - y1) * (x2 - x1) - (x - x1) * (y2 - y1)
        double crossProduct =
                (p.getY() - a.getY()) * (b.getX() - a.getX()) - (p.getX() - a.getX()) * (b.getY() - a.getY());

        return Math.abs(crossProduct) < epsilon;
    }
}
