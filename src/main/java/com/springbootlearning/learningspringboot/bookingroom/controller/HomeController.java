package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    private RoomService roomService;

    public HomeController(RoomService roomService){
        this.roomService = roomService;
    }

    @GetMapping
    public String home(){
        return "pages/home";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model){
        model.addAttribute("rooms",roomService.getAllRooms());
        return "pages/user/dashboard";
    }

    @GetMapping("/booking")
    public String booking(Model model){
        model.addAttribute("rooms",roomService.getAllRooms());
        return "pages/user/booking";
    }

}
