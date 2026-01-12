package com.springbootlearning.learningspringboot.bookingroom.model;


import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private String location;

    @Column(nullable=false)
    private Boolean projector;

    @Column(nullable=false)
    private Boolean whiteboard;

    @Column(nullable=false)
    private Boolean videoconferencing;

    @Column(nullable = false)
    private Boolean available = true;

    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<Booking> bookings =new ArrayList<Booking>();



    public Room(){}


    public Room(String name, String description, Integer capacity, String location, Boolean projector, Boolean whiteboard, Boolean videoconferencing) {
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.location = location;
        this.projector = projector;
        this.whiteboard = whiteboard;
        this.videoconferencing = videoconferencing;
        this.available = true;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Boolean getProjector() {
        return projector;
    }

    public void setProjector(Boolean projector) {
        this.projector = projector;
    }

    public Boolean getWhiteboard() {
        return whiteboard;
    }

    public void setWhiteboard(Boolean whiteboard) {
        this.whiteboard = whiteboard;
    }

    public Boolean getVideoconferencing() {
        return videoconferencing;
    }

    public void setVideoconferencing(Boolean videoconferencing) {
        this.videoconferencing = videoconferencing;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }

    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDateTime availableFrom) {
        this.availableFrom = availableFrom;
    }

    public LocalDateTime getAvailableUntil() {
        return availableUntil;
    }

    public void setAvailableUntil(LocalDateTime availableUntil) {
        this.availableUntil = availableUntil;
    }

    public boolean isAvailable() {
        return this.available;
    }
}
