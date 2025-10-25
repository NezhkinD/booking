package com.booking.hotel.config;

import com.booking.hotel.entity.Hotel;
import com.booking.hotel.entity.Room;
import com.booking.hotel.repository.HotelRepository;
import com.booking.hotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        loadTestData();
    }

    private void loadTestData() {
        log.info("Loading test data for hotel-service...");

        Hotel grandHotel = Hotel.builder()
                .name("Grand Hotel")
                .address("123 Main Street, New York, NY 10001")
                .rooms(new ArrayList<>())
                .build();

        Hotel budgetInn = Hotel.builder()
                .name("Budget Inn")
                .address("456 Oak Avenue, Los Angeles, CA 90001")
                .rooms(new ArrayList<>())
                .build();

        Hotel luxuryResort = Hotel.builder()
                .name("Luxury Resort & Spa")
                .address("789 Beach Road, Miami, FL 33101")
                .rooms(new ArrayList<>())
                .build();

        Hotel cityCenter = Hotel.builder()
                .name("City Center Hotel")
                .address("321 Downtown Street, Chicago, IL 60601")
                .rooms(new ArrayList<>())
                .build();

        Hotel mountainView = Hotel.builder()
                .name("Mountain View Lodge")
                .address("555 Alpine Road, Denver, CO 80201")
                .rooms(new ArrayList<>())
                .build();

        List<Hotel> hotels = List.of(grandHotel, budgetInn, luxuryResort, cityCenter, mountainView);
        hotels = hotelRepository.saveAll(hotels);
        log.info("Created {} hotels", hotels.size());

        List<Room> allRooms = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Room room = Room.builder()
                    .hotel(hotels.get(0))
                    .number("10" + i)
                    .available(i <= 8)
                    .timesBooked(i > 8 ? 5 : 0)
                    .build();
            allRooms.add(room);
        }

        for (int i = 1; i <= 7; i++) {
            Room room = Room.builder()
                    .hotel(hotels.get(1))
                    .number("20" + i)
                    .available(true)
                    .timesBooked(0)
                    .build();
            allRooms.add(room);
        }

        for (int i = 1; i <= 15; i++) {
            Room room = Room.builder()
                    .hotel(hotels.get(2))
                    .number("30" + (i < 10 ? "0" + i : i))
                    .available(i <= 12)
                    .timesBooked(i > 12 ? 3 : 0)
                    .build();
            allRooms.add(room);
        }

        for (int i = 1; i <= 8; i++) {
            Room room = Room.builder()
                    .hotel(hotels.get(3))
                    .number("40" + i)
                    .available(i <= 6)
                    .timesBooked(i > 6 ? 2 : 0)
                    .build();
            allRooms.add(room);
        }

        for (int i = 1; i <= 12; i++) {
            Room room = Room.builder()
                    .hotel(hotels.get(4))
                    .number("50" + (i < 10 ? "0" + i : i))
                    .available(i <= 10)
                    .timesBooked(i > 10 ? 4 : 0)
                    .build();
            allRooms.add(room);
        }

        allRooms = roomRepository.saveAll(allRooms);
        log.info("Created {} rooms across all hotels", allRooms.size());

        log.info("Test data loading completed successfully!");
        log.info("Available hotels: {}", hotelRepository.count());
        log.info("Total rooms: {}", roomRepository.count());
        log.info("Available rooms: {}", roomRepository.countByAvailable(true));
    }
}
