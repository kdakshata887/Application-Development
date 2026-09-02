package com.examly.springapp.service.impl;

import com.examly.springapp.dto.LeaveDecisionRequest;
import com.examly.springapp.dto.LeaveRequest;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.LeaveApplication;
import com.examly.springapp.model.LeaveStatus;
import com.examly.springapp.model.Teacher;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.LeaveApplicationRepository;
import com.examly.springapp.repository.TeacherRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.LeaveApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveApplicationServiceImpl implements LeaveApplicationService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    @Override
    public LeaveApplication applyLeave(LeaveRequest request) {
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + request.getTeacherId()));

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
    public LeaveApplication approveLeave(Long leaveId, LeaveDecisionRequest request) {
        LeaveApplication leave = getLeaveById(leaveId);
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
    public LeaveApplication rejectLeave(Long leaveId, LeaveDecisionRequest request) {
        LeaveApplication leave = getLeaveById(leaveId);
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
}
