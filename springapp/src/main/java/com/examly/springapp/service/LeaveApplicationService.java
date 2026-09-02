package com.examly.springapp.service;

import com.examly.springapp.dto.LeaveDecisionRequest;
import com.examly.springapp.dto.LeaveRequest;
import com.examly.springapp.model.LeaveApplication;
import com.examly.springapp.model.Teacher;

import java.time.LocalDate;
import java.util.List;

public interface LeaveApplicationService {
    LeaveApplication applyLeave(LeaveRequest request);
    LeaveApplication approveLeave(Long leaveId, LeaveDecisionRequest request);
    LeaveApplication rejectLeave(Long leaveId, LeaveDecisionRequest request);
    List<LeaveApplication> getAllLeaves();
    List<LeaveApplication> getLeavesByTeacher(Long teacherId);
    List<LeaveApplication> getPendingLeaves();
    LeaveApplication getLeaveById(Long id);

    /**
     * Returns teachers who do not have approved/pending leaves overlapping the given date range.
     * Excludes the teacher requesting leave.
     */
    List<Teacher> getSuggestedSubstitutes(Long requestingTeacherId, LocalDate fromDate, LocalDate toDate);
}
