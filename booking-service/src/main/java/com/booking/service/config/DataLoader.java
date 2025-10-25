package com.booking.service.config;

import com.booking.service.entity.Booking;
import com.booking.service.entity.User;
import com.booking.service.repository.BookingRepository;
import com.booking.service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        loadTestData();
    }

    private void loadTestData() {
        log.info("Loading test data for booking-service...");

        // Create users
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin"))
                .role(User.Role.ADMIN)
                .build();

        User user1 = User.builder()
                .username("user1")
                .password(passwordEncoder.encode("password1"))
                .role(User.Role.USER)
                .build();

        User user2 = User.builder()
                .username("user2")
                .password(passwordEncoder.encode("password2"))
                .role(User.Role.USER)
                .build();

        User john = User.builder()
                .username("john")
                .password(passwordEncoder.encode("john123"))
                .role(User.Role.USER)
                .build();

        User alice = User.builder()
                .username("alice")
                .password(passwordEncoder.encode("alice123"))
                .role(User.Role.USER)
                .build();

        User bob = User.builder()
                .username("bob")
                .password(passwordEncoder.encode("bob123"))
                .role(User.Role.USER)
                .build();

        List<User> users = userRepository.saveAll(List.of(admin, user1, user2, john, alice, bob));
        log.info("Created {} users", users.size());

        // Create bookings
        List<Booking> bookings = new ArrayList<>();

        // Booking 1: user1 - confirmed booking (room 1)
        bookings.add(Booking.builder()
                .user(users.get(1)) // user1
                .roomId(1L)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(7))
                .status(Booking.BookingStatus.CONFIRMED)
                .requestId(UUID.randomUUID().toString())
                .build());

        // Booking 2: john - confirmed booking (room 3)
        bookings.add(Booking.builder()
                .user(users.get(3)) // john
                .roomId(3L)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(15))
                .status(Booking.BookingStatus.CONFIRMED)
                .requestId(UUID.randomUUID().toString())
                .build());

        // Booking 3: alice - pending booking (room 5)
        bookings.add(Booking.builder()
                .user(users.get(4)) // alice
                .roomId(5L)
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(4))
                .status(Booking.BookingStatus.PENDING)
                .requestId(UUID.randomUUID().toString())
                .build());

        // Booking 4: user2 - cancelled booking (room 7)
        bookings.add(Booking.builder()
                .user(users.get(2)) // user2
                .roomId(7L)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .status(Booking.BookingStatus.CANCELLED)
                .requestId(UUID.randomUUID().toString())
                .build());

        // Booking 5: bob - confirmed booking (room 10)
        bookings.add(Booking.builder()
                .user(users.get(5)) // bob
                .roomId(10L)
                .startDate(LocalDate.now().plusDays(20))
                .endDate(LocalDate.now().plusDays(25))
                .status(Booking.BookingStatus.CONFIRMED)
                .requestId(UUID.randomUUID().toString())
                .build());

        // Booking 6: john - confirmed booking (room 15)
        bookings.add(Booking.builder()
                .user(users.get(3)) // john
                .roomId(15L)
                .startDate(LocalDate.now().plusDays(30))
                .endDate(LocalDate.now().plusDays(35))
                .status(Booking.BookingStatus.CONFIRMED)
                .requestId(UUID.randomUUID().toString())
                .build());

        // Booking 7: alice - pending booking (room 20)
        bookings.add(Booking.builder()
                .user(users.get(4)) // alice
                .roomId(20L)
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(10))
                .status(Booking.BookingStatus.PENDING)
                .requestId(UUID.randomUUID().toString())
                .build());

        // Booking 8: user1 - past booking (already completed)
        bookings.add(Booking.builder()
                .user(users.get(1)) // user1
                .roomId(2L)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(7))
                .status(Booking.BookingStatus.CONFIRMED)
                .requestId(UUID.randomUUID().toString())
                .build());

        bookings = bookingRepository.saveAll(bookings);
        log.info("Created {} bookings", bookings.size());

        log.info("Test data loading completed successfully!");
        log.info("Total users: {}", userRepository.count());
        log.info("Total bookings: {}", bookingRepository.count());
        log.info("Confirmed bookings: {}", bookingRepository.countByStatus(Booking.BookingStatus.CONFIRMED));
        log.info("Pending bookings: {}", bookingRepository.countByStatus(Booking.BookingStatus.PENDING));
        log.info("Cancelled bookings: {}", bookingRepository.countByStatus(Booking.BookingStatus.CANCELLED));

        log.info("\n========== TEST USERS ==========");
        log.info("Admin: username=admin, password=admin");
        log.info("User1: username=user1, password=password1");
        log.info("User2: username=user2, password=password2");
        log.info("John: username=john, password=john123");
        log.info("Alice: username=alice, password=alice123");
        log.info("Bob: username=bob, password=bob123");
        log.info("================================\n");
    }
}
