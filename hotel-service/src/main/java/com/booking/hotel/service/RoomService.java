package com.booking.hotel.service;

import com.booking.hotel.dto.RoomCreateRequest;
import com.booking.hotel.dto.RoomDTO;
import com.booking.hotel.entity.Hotel;
import com.booking.hotel.entity.Room;
import com.booking.hotel.exception.DuplicateRoomNumberException;
import com.booking.hotel.exception.ResourceNotFoundException;
import com.booking.hotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelService hotelService;

    @Transactional
    public RoomDTO createRoom(RoomCreateRequest request) {
        log.debug("Creating room: number={}, hotelId={}", request.getNumber(), request.getHotelId());

        Hotel hotel = hotelService.getHotelById(request.getHotelId());

        // Check if room with this number already exists in this hotel
        if (roomRepository.existsByHotelIdAndNumber(hotel.getId(), request.getNumber())) {
            log.warn("Attempt to create duplicate room: number={}, hotelId={}",
                    request.getNumber(), hotel.getId());
            throw new DuplicateRoomNumberException(
                    String.format("Room with number '%s' already exists in hotel with ID %d",
                            request.getNumber(), hotel.getId()));
        }

        Room room = Room.builder()
                .hotel(hotel)
                .number(request.getNumber())
                .available(true)
                .timesBooked(0)
                .build();

        room = roomRepository.save(room);
        log.info("Room created successfully: id={}, number={}, hotelId={}",
                room.getId(), room.getNumber(), hotel.getId());

        return mapToDTO(room);
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getAvailableRooms(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching available rooms for dates: {} to {}", startDate, endDate);

        return roomRepository.findAvailableRoomsForDates(startDate, endDate)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getRecommendedRooms(Long hotelId, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching recommended rooms for hotel: {}, dates: {} to {}",
                hotelId, startDate, endDate);

        return roomRepository.findRecommendedRoomsForDates(hotelId, startDate, endDate)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
    }

    private RoomDTO mapToDTO(Room room) {
        return RoomDTO.builder()
                .id(room.getId())
                .hotelId(room.getHotel().getId())
                .number(room.getNumber())
                .available(room.getAvailable())
                .timesBooked(room.getTimesBooked())
                .build();
    }
}
