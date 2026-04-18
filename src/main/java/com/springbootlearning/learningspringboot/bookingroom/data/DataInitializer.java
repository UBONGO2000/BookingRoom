package com.springbootlearning.learningspringboot.bookingroom.data;

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

            rooms.add(new Room("SOLEIL", "Une salle lumineuse ideale pour les brainstormings creatifs.", 50, "Batiment A, 1er etage", true, true, true));
            rooms.add(new Room("FREEDOM", "Petite salle calme pour des entretiens ou du travail concentre.", 10, "Batiment B, RDC", false, true, true));
            rooms.add(new Room("JUPITER", "Grande salle de conference equipee pour la visioconference.", 100, "Batiment C, 5eme etage", true, true, true));
            rooms.add(new Room("MARS", "Salle spacieuse avec une vue imprenable.", 30, "Batiment A, 2eme etage", true, true, false));
            rooms.add(new Room("VENUS", "Ambiance chaleureuse pour vos reunions d'equipe.", 20, "Batiment B, 1er etage", false, true, true));
            rooms.add(new Room("NEPTUNE", "Salle equipee de technologies de pointe.", 15, "Batiment C, RDC", true, false, true));
            rooms.add(new Room("SATURNE", "Ideale pour les grandes presentations.", 80, "Batiment D, 3eme etage", true, true, true));
            rooms.add(new Room("MERCURE", "Petite salle pour des points rapides.", 8, "Batiment A, RDC", false, true, false));
            rooms.add(new Room("URANUS", "Espace creatif avec tableaux blancs partout.", 25, "Batiment B, 2eme etage", false, true, true));
            rooms.add(new Room("PLUTON", "Salle isolee pour une confidentialite maximale.", 12, "Sous-sol, Batiment C", false, false, true));
            rooms.add(new Room("GALAXY", "Immense auditorium pour evenements majeurs.", 200, "Batiment E, RDC", true, true, true));
            rooms.add(new Room("ORION", "Salle moderne avec mobilier ergonomique.", 40, "Batiment D, 1er etage", true, true, false));
            rooms.add(new Room("ANDROMEDE", "Espace polyvant pour ateliers.", 60, "Batiment B, 3eme etage", true, true, true));
            rooms.add(new Room("COSMOS", "Salle avec equipement audio haute fidelite.", 35, "Batiment A, 3eme etage", true, false, true));
            rooms.add(new Room("ZENITH", "Salle au dernier etage, tres calme.", 18, "Batiment C, 6eme etage", false, true, true));

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
                    passwordEncoder.encode("Admin@123"),
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
                    passwordEncoder.encode("User@123")
            );
            userRepository.save(user);
            System.out.println("✅ Standard user created.");
        }
    }

}
