package com.examly.springapp.service.impl;

import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Room;
import com.examly.springapp.repository.RoomRepository;
import com.examly.springapp.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    @Override
    public Room createRoom(Room room) {
        return roomRepository.save(room);
    }

    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }

    @Override
    public Room updateRoom(Long id, Room update) {
        Room room = getRoomById(id);
        if (update.getRoomName() != null) room.setRoomName(update.getRoomName());
        if (update.getRoomType() != null) room.setRoomType(update.getRoomType());
        if (update.getCapacity() != null) room.setCapacity(update.getCapacity());
        return roomRepository.save(room);
    }

    @Override
    public void deleteRoom(Long id) {
        roomRepository.delete(getRoomById(id));
    }
}
