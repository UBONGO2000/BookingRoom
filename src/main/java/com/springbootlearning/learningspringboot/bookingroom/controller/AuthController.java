package com.springbootlearning.learningspringboot.bookingroom.controller;

import com.springbootlearning.learningspringboot.bookingroom.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    @GetMapping("/register")
    public String register(Model model){
        model.addAttribute("user",new User());
        return "pages/user/register";
    }


    @PostMapping("/register")
    public String register(@ModelAttribute("user") User user){
        return "redirect:/login";
    }
}
