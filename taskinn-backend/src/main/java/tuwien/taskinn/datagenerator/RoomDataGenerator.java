package tuwien.taskinn.datagenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tuwien.taskinn.entity.Point;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.entity.Staircase;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.repo.StaircaseRepository;

/**
 * Generates a specified number of sample Rooms on application startup. Creates realistic clusters of room types and
 * varies their initial statuses.
 */
@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class RoomDataGenerator {

    private final RoomRepository roomRepository;
    private final StaircaseRepository staircaseRepository;
    private final Map<Integer, List<SvgPoint>> roomShapes = new LinkedHashMap<>();

    @Value("${app.datagen.floor-count:5}")
    private int floorCount;

    public List<Room> generateData(List<RoomType> roomTypes) {
        if (roomRepository.count() > 0) {
            log.info("Rooms already exist. Skipping generation.");
            return roomRepository.findAll();
        }

        this.loadSvgData();

        if (roomShapes.isEmpty()) {
            log.warn("No room shapes found in CSV. Skipping room generation.");
            return new ArrayList<>();
        }

        if (roomTypes.isEmpty()) {
            log.warn("No room types available. Skipping room generation.");
            return new ArrayList<>();
        }

        int totalRooms = roomShapes.size() * floorCount;
        log.info("Generating {} rooms ({} shapes x {} floors) based on CSV...", totalRooms, roomShapes.size(),
                floorCount);

        this.generateStaircases();

        List<RoomType> sortedRoomTypes =
                roomTypes.stream().sorted(Comparator.comparing(RoomType::getBasePrice)).toList();

        List<Room> roomsToCreate = new ArrayList<>();
        List<Integer> shapeIds = new ArrayList<>(roomShapes.keySet());
        int roomIndex = 0;

        for (int floor = 1; floor <= floorCount; floor++) {
            for (int i = 0; i < shapeIds.size(); i++) {
                Integer shapeId = shapeIds.get(i);
                List<SvgPoint> shapePoints = roomShapes.get(shapeId);

                String roomNumber = String.format("%d%02d", floor, i + 1);
                RoomType assignedType = sortedRoomTypes.get(roomIndex % sortedRoomTypes.size());
                List<Point> svgPoints = shapePoints.stream().map(p -> new Point(p.x, p.y)).collect(Collectors.toList());

                roomsToCreate.add(createRoom(roomNumber, floor, assignedType, svgPoints));
                roomIndex++;
            }
        }

        List<Room> savedRooms = roomRepository.saveAll(roomsToCreate);
        log.info("Room data generation complete. Created {} rooms.", savedRooms.size());

        return savedRooms;
    }

    private void generateStaircases() {
        if (staircaseRepository.count() > 0) {
            log.info("Staircases already exist. Skipping generation.");
            return;
        }

        double staircaseX = 793.4666748046875d;
        double staircaseY = 663.3048095703125d;

        for (int floor = 1; floor <= floorCount; floor++) {
            Staircase staircase =
                    Staircase.builder().floor(floor).name("").active(true).x(staircaseX).y(staircaseY).build();
            staircaseRepository.save(staircase);
        }

        log.info("Created staircases for {} floors.", floorCount);
    }

    private Room createRoom(String roomNumber, int floor, RoomType type, List<Point> points) {
        return Room.builder()
                .roomNumber(roomNumber)
                .floor(floor)
                .status(RoomStatus.AVAILABLE)
                .roomType(type)
                .active(true)
                .svgPoints(points)
                .build();
    }

    static class SvgPoint {
        double x;
        double y;
        int order;

        SvgPoint(double x, double y, int order) {
            this.x = x;
            this.y = y;
            this.order = order;
        }
    }

    private void loadSvgData() {
        log.info("Loading SVG room point data from CSV...");
        ClassPathResource resource = new ClassPathResource("datagen/ROOM_SVG_POINTS.csv");
        try (InputStream is = resource.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] values = line.split(",");
                if (values.length < 4) {
                    continue;
                }

                try {
                    // Format: SHAPE_ID,X,Y,POINT_INDEX
                    int shapeId = Integer.parseInt(values[0].trim());
                    double x = Double.parseDouble(values[1].trim());
                    double y = Double.parseDouble(values[2].trim());
                    int order = Integer.parseInt(values[3].trim());

                    roomShapes.computeIfAbsent(shapeId, k -> new ArrayList<>()).add(new SvgPoint(x, y, order));
                } catch (NumberFormatException e) {
                    log.warn("Could not parse line in CSV: {}", line);
                }
            }

            roomShapes.values().forEach(points -> points.sort(Comparator.comparingInt(p -> p.order)));

            log.info("Successfully loaded {} room shapes from CSV.", roomShapes.size());

        } catch (IOException e) {
            log.warn("Could not load ROOM_SVG_POINTS.csv. No rooms will be generated from CSV.", e);
        }
    }
}
