package com.springbootlearning.learningspringboot.bookingroom.dto;

public class RoomSearchDto {

    private String name;
    private Integer capacity;
    private boolean projector;
    private boolean whiteboard;
    private boolean videoconferencing;


    public RoomSearchDto(){}


    public RoomSearchDto(String name, Integer capacity, boolean projector, boolean whiteboard, boolean videoconferencing) {
        this.name = name;
        this.capacity = capacity;
        this.projector = projector;
        this.whiteboard = whiteboard;
        this.videoconferencing = videoconferencing;
    }


    //-------- getter and setter --------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public boolean isProjector() {
        return projector;
    }

    public void setProjector(boolean projector) {
        this.projector = projector;
    }

    public boolean isWhiteboard() {
        return whiteboard;
    }

    public void setWhiteboard(boolean whiteboard) {
        this.whiteboard = whiteboard;
    }

    public boolean isVideoconferencing() {
        return videoconferencing;
    }

    public void setVideoconferencing(boolean videoconferencing) {
        this.videoconferencing = videoconferencing;
    }


    @Override
    public String toString() {
        return "RoomSearchDto{" +
                "name='" + name + '\'' +
                ", capacity=" + capacity +
                ", projector=" + projector +
                ", whiteboard=" + whiteboard +
                ", videoconferencing=" + videoconferencing +
                '}';
    }
}
