package com.springbootlearning.learningspringboot.bookingroom.data;

import com.springbootlearning.learningspringboot.bookingroom.RoomService;
import com.springbootlearning.learningspringboot.bookingroom.model.Role;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.model.User;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import com.springbootlearning.learningspringboot.bookingroom.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {

    private  RoomRepository roomrepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;


    public DataInitializer(RoomRepository roomrepository, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.roomrepository = roomrepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Bean
    CommandLineRunner initDatabase(RoomRepository roomrepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {

            List<Room> rooms = new ArrayList<Room>();

            Room room1 = new Room("SOLEIL","zaezaeza",50,"Place THOMAS SANKARA",true,true,true);
            Room room2 = new Room("FREEDOM","zaezaeza",10,"Place NELSON MANDELA",false,true,true);

            rooms.add(room1);
            rooms.add(room2);
            for(Room room : rooms){
                if(!roomrepository.existsByName(room.getName())){
                    roomrepository.save(room);
                }
            }
        };
    }


    @EventListener(ApplicationReadyEvent.class)
    public void seedUsers() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User(
                    "jean",
                    "paul",
                    "admin",
                    "admin@gmail.com",
                    passwordEncoder.encode("admin123"),
                    Role.ADMIN
            );
            userRepository.save(admin);
            System.out.println("✅ Admin user created.");
        }

        if (userRepository.findByUsername("user").isEmpty()) {
            User user = new User(
                    "jacque",
                    "pierre",
                    "user",
                    "user@gmail.com",
                    passwordEncoder.encode("user123")
            );
            userRepository.save(user);
            System.out.println("✅ Standard user created.");
        }
    }

}
