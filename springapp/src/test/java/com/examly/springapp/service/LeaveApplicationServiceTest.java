package com.examly.springapp.service;

import com.examly.springapp.dto.LeaveDecisionRequest;
import com.examly.springapp.dto.LeaveRequest;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.impl.LeaveApplicationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LeaveApplicationServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class LeaveApplicationServiceTest {

    @Mock LeaveApplicationRepository leaveApplicationRepository;
    @Mock TeacherRepository teacherRepository;
    @Mock UserRepository userRepository;
    @Mock TimetableRepository timetableRepository;

    @InjectMocks
    LeaveApplicationServiceImpl leaveService;

    private Teacher buildTeacher(Long id, String name) {
        return Teacher.builder().teacherId(id).name(name).employeeId("EMP00" + id).isActive(true).build();
    }

    @Test
    void applyLeave_shouldCreatePendingLeave_whenTeacherExists() {
        Teacher teacher = buildTeacher(1L, "Alice");
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        LeaveRequest req = LeaveRequest.builder()
                .teacherId(1L).leaveType(LeaveType.SL)
                .fromDate(LocalDate.now()).toDate(LocalDate.now().plusDays(2))
                .reason("Fever").build();

        LeaveApplication saved = LeaveApplication.builder()
                .leaveId(1L).teacher(teacher).status(LeaveStatus.PENDING)
                .leaveType(LeaveType.SL).build();
        when(leaveApplicationRepository.save(any())).thenReturn(saved);

        when(leaveApplicationRepository.existsOverlappingLeaveForTeacher(
                eq(1L), any(), any())).thenReturn(false);

        LeaveApplication result = leaveService.applyLeave(req);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(result.getTeacher().getTeacherId()).isEqualTo(1L);
    }

    @Test
    void applyLeave_shouldThrow_whenTeacherNotFound() {
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        LeaveRequest req = LeaveRequest.builder().teacherId(999L)
                .leaveType(LeaveType.CL)
                .fromDate(LocalDate.now()).toDate(LocalDate.now())
                .reason("Test").build();

        // No overlap-check stub needed — the service throws ResourceNotFoundException
        // before reaching the overlap check (teacher 999 doesn't exist)
        assertThatThrownBy(() -> leaveService.applyLeave(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Teacher not found");
    }

    @Test
    void approveLeave_shouldSetStatusToApproved() {
        Teacher teacher = buildTeacher(1L, "Alice");
        User approver = User.builder().userId(10L).username("principal").role(Role.PRINCIPAL).build();
        Teacher substitute = buildTeacher(2L, "Bob");

        LeaveApplication leave = LeaveApplication.builder()
                .leaveId(1L).teacher(teacher).status(LeaveStatus.PENDING).build();

        when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(userRepository.findById(10L)).thenReturn(Optional.of(approver));
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(substitute));
        when(leaveApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LeaveDecisionRequest decision = new LeaveDecisionRequest();
        decision.setApproverUserId(10L);
        decision.setSubstituteTeacherId(2L);

        LeaveApplication result = leaveService.approveLeave(1L, decision);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(result.getApprovedBy().getUsername()).isEqualTo("principal");
        assertThat(result.getSubstituteTeacher().getName()).isEqualTo("Bob");
    }

    @Test
    void rejectLeave_shouldSetStatusToRejected() {
        Teacher teacher = buildTeacher(1L, "Alice");
        LeaveApplication leave = LeaveApplication.builder()
                .leaveId(1L).teacher(teacher).status(LeaveStatus.PENDING).build();

        when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(leaveApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LeaveDecisionRequest decision = new LeaveDecisionRequest();
        LeaveApplication result = leaveService.rejectLeave(1L, decision);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.REJECTED);
    }

    @Test
    void getSuggestedSubstitutes_shouldExcludeRequestingTeacher_andBusyTeachers() {
        Teacher t1 = buildTeacher(1L, "Alice");
        Teacher t2 = buildTeacher(2L, "Bob");
        Teacher t3 = buildTeacher(3L, "Charlie");

        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(2);

        // Bob is busy (has overlapping leave)
        when(leaveApplicationRepository.findTeacherIdsWithLeavesOverlapping(from, to))
                .thenReturn(List.of(2L));

        // All teachers in DB
        when(teacherRepository.findAll()).thenReturn(List.of(t1, t2, t3));

        // Requesting is Alice (1L) — should be excluded
        // Alice has no active timetable entries — conflictSlots will be empty,
        // so the timetable.findAll() branch is never entered (no stub needed for it).
        when(timetableRepository.findByTeacher_TeacherIdAndIsActiveTrue(1L)).thenReturn(List.of());

        List<Teacher> substitutes = leaveService.getSuggestedSubstitutes(1L, from, to);

        // Should only return Charlie — Alice is the requester, Bob is busy
        assertThat(substitutes).hasSize(1);
        assertThat(substitutes.get(0).getName()).isEqualTo("Charlie");
    }
}
