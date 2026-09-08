package com.examly.springapp.service.impl;

import com.examly.springapp.dto.LeaveDecisionRequest;
import com.examly.springapp.dto.LeaveRequest;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.LeaveApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveApplicationServiceImpl implements LeaveApplicationService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final TimetableRepository timetableRepository;

    @Override
    @Transactional
    public LeaveApplication applyLeave(LeaveRequest request) {
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + request.getTeacherId()));

        // FIX: Date range validation — fromDate must not be after toDate
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new IllegalArgumentException(
                    "From date (" + request.getFromDate() + ") cannot be after to date (" + request.getToDate() + ")");
        }

        // FIX: Duplicate/overlap detection — prevent overlapping leave applications
        boolean hasOverlap = leaveApplicationRepository.existsOverlappingLeaveForTeacher(
                request.getTeacherId(), request.getFromDate(), request.getToDate());
        if (hasOverlap) {
            throw new IllegalStateException(
                    "Teacher already has a pending or approved leave overlapping the requested date range: "
                    + request.getFromDate() + " to " + request.getToDate());
        }

        LeaveApplication leave = LeaveApplication.builder()
                .teacher(teacher)
                .leaveType(request.getLeaveType())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .reason(request.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        return leaveApplicationRepository.save(leave);
    }

    @Override
    @Transactional
    public LeaveApplication approveLeave(Long leaveId, LeaveDecisionRequest request) {
        LeaveApplication leave = getLeaveById(leaveId);

        // FIX: State transition guard — only PENDING leaves can be approved
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot approve a leave that is not in PENDING status. Current status: " + leave.getStatus());
        }

        leave.setStatus(LeaveStatus.APPROVED);

        if (request.getApproverUserId() != null) {
            User approver = userRepository.findById(request.getApproverUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getApproverUserId()));
            leave.setApprovedBy(approver);
        }

        if (request.getSubstituteTeacherId() != null) {
            Teacher substitute = teacherRepository.findById(request.getSubstituteTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + request.getSubstituteTeacherId()));
            leave.setSubstituteTeacher(substitute);
        }

        return leaveApplicationRepository.save(leave);
    }

    @Override
    @Transactional
    public LeaveApplication rejectLeave(Long leaveId, LeaveDecisionRequest request) {
        LeaveApplication leave = getLeaveById(leaveId);

        // FIX: State transition guard — only PENDING leaves can be rejected
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot reject a leave that is not in PENDING status. Current status: " + leave.getStatus());
        }

        leave.setStatus(LeaveStatus.REJECTED);

        if (request.getApproverUserId() != null) {
            User approver = userRepository.findById(request.getApproverUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getApproverUserId()));
            leave.setApprovedBy(approver);
        }

        return leaveApplicationRepository.save(leave);
    }

    @Override
    public List<LeaveApplication> getAllLeaves() {
        return leaveApplicationRepository.findAll();
    }

    @Override
    public List<LeaveApplication> getLeavesByTeacher(Long teacherId) {
        return leaveApplicationRepository.findByTeacher_TeacherId(teacherId);
    }

    @Override
    public List<LeaveApplication> getPendingLeaves() {
        return leaveApplicationRepository.findByStatus(LeaveStatus.PENDING);
    }

    @Override
    public LeaveApplication getLeaveById(Long id) {
        return leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found with id: " + id));
    }

    /**
     * FIX: Substitution now checks both:
     * 1. Teachers who have overlapping leaves (cannot substitute if they're also on leave)
     * 2. Teachers who are already scheduled in the EXACT timetable slots of the absent teacher
     *    (cannot substitute if they have classes at the same day/period)
     */
    @Override
    public List<Teacher> getSuggestedSubstitutes(Long requestingTeacherId, LocalDate fromDate, LocalDate toDate) {
        // Find teacher IDs that have conflicting leaves in the date range
        Set<Long> busyTeacherIds = leaveApplicationRepository
                .findTeacherIdsWithLeavesOverlapping(fromDate, toDate)
                .stream().collect(Collectors.toSet());

        // Also exclude the requesting teacher themselves
        busyTeacherIds.add(requestingTeacherId);

        // Get the absent teacher's timetable slots (active entries)
        List<Timetable> absentTeacherSlots = timetableRepository
                .findByTeacher_TeacherIdAndIsActiveTrue(requestingTeacherId);

        // Build a set of "DAY_PERIOD" strings representing the absent teacher's schedule
        Set<String> conflictSlots = absentTeacherSlots.stream()
                .map(t -> t.getDay().name() + "_" + t.getPeriod())
                .collect(Collectors.toSet());

        // Find all teachers who have at least one of these day/period slots occupied
        if (!conflictSlots.isEmpty()) {
            List<Timetable> allActive = timetableRepository.findAll().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                    .collect(Collectors.toList());
            for (Timetable slot : allActive) {
                String slotKey = slot.getDay().name() + "_" + slot.getPeriod();
                if (conflictSlots.contains(slotKey)) {
                    // This teacher is already scheduled at a time the absent teacher teaches
                    busyTeacherIds.add(slot.getTeacher().getTeacherId());
                }
            }
        }

        // Return all active teachers not in the busy set
        return teacherRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .filter(t -> !busyTeacherIds.contains(t.getTeacherId()))
                .collect(Collectors.toList());
    }
}
