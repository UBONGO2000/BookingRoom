package com.springbootlearning.learningspringboot.bookingroom.data;

import com.springbootlearning.learningspringboot.bookingroom.RoomService;
import com.springbootlearning.learningspringboot.bookingroom.model.Room;
import com.springbootlearning.learningspringboot.bookingroom.repository.RoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RoomDataInitializer {

    private  RoomRepository roomrepository;
    @Bean
    CommandLineRunner initDatabase(RoomRepository roomrepository) {
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

}
