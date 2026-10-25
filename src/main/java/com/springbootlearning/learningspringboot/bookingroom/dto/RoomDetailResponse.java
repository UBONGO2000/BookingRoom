package com.springbootlearning.learningspringboot.bookingroom.dto;

import java.util.List;

public record RoomDetailResponse(RoomResponse room, List<Object> evaluations) {
}
