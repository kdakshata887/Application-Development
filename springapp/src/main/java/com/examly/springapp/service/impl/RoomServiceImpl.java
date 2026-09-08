package com.examly.springapp.service.impl;

import com.examly.springapp.dto.BulkDeleteResult;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Room;
import com.examly.springapp.repository.RoomRepository;
import com.examly.springapp.repository.TimetableRepository;
import com.examly.springapp.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final TimetableRepository timetableRepository;

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

    /**
     * Bulk-delete rooms by ID.
     * Rooms with active timetable entries cannot be deleted.
     */
    @Override
    @Transactional
    public BulkDeleteResult bulkDelete(List<Long> ids) {
        List<Long> deletedIds = new ArrayList<>();
        List<BulkDeleteResult.FailedEntry> failed = new ArrayList<>();

        for (Long id : ids) {
            try {
                Room room = roomRepository.findById(id).orElse(null);
                if (room == null) {
                    failed.add(new BulkDeleteResult.FailedEntry(id, "Room not found"));
                    continue;
                }
                if (timetableRepository.existsByRoom_RoomIdAndIsActiveTrue(id)) {
                    failed.add(new BulkDeleteResult.FailedEntry(id,
                            "Cannot delete room '" + room.getRoomName() + "' — assigned to active timetable entries. Remove from timetable first."));
                    continue;
                }
                roomRepository.delete(room);
                deletedIds.add(id);
            } catch (Exception e) {
                failed.add(new BulkDeleteResult.FailedEntry(id, "Deletion failed: " + e.getMessage()));
            }
        }

        return BulkDeleteResult.builder()
                .deletedCount(deletedIds.size())
                .failedCount(failed.size())
                .deletedIds(deletedIds)
                .failed(failed)
                .build();
    }
}
