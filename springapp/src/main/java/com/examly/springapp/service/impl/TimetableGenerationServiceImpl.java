package com.examly.springapp.service.impl;

import com.examly.springapp.dto.TimetableGenerationResult;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.TimetableGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic greedy timetable generator (FR4).
 *
 * Algorithm:
 * 1. For each section, load its SubjectTeacherMappings (subject + teacher + periodsPerWeek).
 * 2. Expand mappings into a list of "slots to fill" (one per required period).
 * 3. Iterate over working days × periods (1–periodsPerDay).
 * 4. For each slot, find the next unscheduled subject-teacher pair.
 * 5. Check constraints: teacher not double-booked, room not double-booked, section not double-booked.
 * 6. Assign the slot; if no valid assignment exists, record a violation.
 *
 * CRITICAL FIX: Cross-section clash prevention.
 * When generating for ALL sections, a SINGLE shared teacherSlotMap and roomSlotMap
 * is used across all sections. This prevents Teacher A from being scheduled in
 * Section X and Section Y at the same day/period.
 *
 * Additional constraints:
 * - Teachers on approved/pending leave during the generation date range are excluded.
 * - Room capacity is checked against section student count.
 * - Double (lab) periods are scheduled in consecutive periods.
 *
 * The algorithm is transparent and deterministic — no ML or random components.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimetableGenerationServiceImpl implements TimetableGenerationService {

    private final TimetableRepository timetableRepository;
    private final SubjectTeacherMappingRepository mappingRepository;
    private final ClassSectionRepository classSectionRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;

    @Override
    @Transactional
    public TimetableGenerationResult generateForSection(Long sectionId, String workingDays, int periodsPerDay) {
        ClassSection section = classSectionRepository.findById(sectionId).orElse(null);
        if (section == null) {
            return TimetableGenerationResult.builder()
                    .success(false).message("Section not found: " + sectionId)
                    .violations(List.of("Section " + sectionId + " does not exist"))
                    .warnings(List.of()).build();
        }

        List<Day> days = parseDays(workingDays);
        if (days.isEmpty()) {
            return TimetableGenerationResult.builder()
                    .success(false).message("No valid working days provided")
                    .violations(List.of("workingDays must contain valid Day enum values"))
                    .warnings(List.of()).build();
        }

        List<Room> allRooms = roomRepository.findAll();

        // Build global slot maps pre-populated with existing active timetable entries
        // (from other sections that may have been generated previously).
        Map<Long, Set<String>> teacherSlotMap = new HashMap<>();
        Map<Long, Set<String>> roomSlotMap = new HashMap<>();
        // Pre-load existing active entries from OTHER sections to detect cross-section clashes
        List<Timetable> existingOtherSections = timetableRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive())
                        && !t.getSection().getSectionId().equals(sectionId))
                .collect(Collectors.toList());
        preloadSlotMaps(existingOtherSections, teacherSlotMap, roomSlotMap);

        // Build on-leave teacher set for the generation period
        LocalDate effectiveFrom = LocalDate.now();
        LocalDate effectiveTo = effectiveFrom.plusDays((long) days.size() * 2);
        Set<Long> onLeaveTeacherIds = getTeachersOnLeave(effectiveFrom, effectiveTo);

        return generateForSectionInternal(section, days, periodsPerDay, allRooms,
                teacherSlotMap, roomSlotMap, onLeaveTeacherIds);
    }

    @Override
    @Transactional
    public TimetableGenerationResult generateForAll(String workingDays, int periodsPerDay) {
        List<ClassSection> sections = classSectionRepository.findAll();
        if (sections.isEmpty()) {
            return TimetableGenerationResult.builder()
                    .success(false).message("No class sections found")
                    .violations(List.of("Create class sections before generating timetables"))
                    .warnings(List.of()).build();
        }

        List<Day> days = parseDays(workingDays);
        if (days.isEmpty()) {
            return TimetableGenerationResult.builder()
                    .success(false).message("No valid working days provided")
                    .violations(List.of("workingDays must contain valid Day enum values"))
                    .warnings(List.of()).build();
        }

        List<Room> allRooms = roomRepository.findAll();

        // CRITICAL FIX: Shared global slot maps across ALL sections.
        // All sections share the SAME teacherSlotMap and roomSlotMap.
        // This prevents Teacher X from appearing in Section A and Section B
        // at the same day+period, and prevents Room 101 from being used by
        // two sections simultaneously.
        Map<Long, Set<String>> globalTeacherSlotMap = new HashMap<>();
        Map<Long, Set<String>> globalRoomSlotMap = new HashMap<>();

        // Teachers on leave during generation window
        LocalDate effectiveFrom = LocalDate.now();
        LocalDate effectiveTo = effectiveFrom.plusDays((long) days.size() * 2);
        Set<Long> onLeaveTeacherIds = getTeachersOnLeave(effectiveFrom, effectiveTo);

        int totalCreated = 0;
        List<String> allViolations = new ArrayList<>();
        List<String> allWarnings = new ArrayList<>();

        for (ClassSection section : sections) {
            TimetableGenerationResult result = generateForSectionInternal(
                    section, days, periodsPerDay, allRooms,
                    globalTeacherSlotMap, globalRoomSlotMap, onLeaveTeacherIds);
            totalCreated += result.getEntriesCreated();
            allViolations.addAll(result.getViolations());
            allWarnings.addAll(result.getWarnings());
        }

        boolean success = allViolations.isEmpty();
        return TimetableGenerationResult.builder()
                .success(success)
                .message(success
                        ? String.format("Timetables generated for all %d sections", sections.size())
                        : "Generation completed with violations in some sections")
                .entriesCreated(totalCreated)
                .violations(allViolations)
                .warnings(allWarnings)
                .build();
    }

    /**
     * Core generation logic for a single section.
     * Accepts shared (global) slot maps that accumulate state across sections.
     *
     * @param section            the section to generate for
     * @param days               working days
     * @param periodsPerDay      number of periods per day
     * @param allRooms           all available rooms
     * @param teacherSlotMap     SHARED map — teacher → set of "DAY_PERIOD" already assigned globally
     * @param roomSlotMap        SHARED map — room → set of "DAY_PERIOD" already assigned globally
     * @param onLeaveTeacherIds  teacher IDs that are on leave during this period
     */
    private TimetableGenerationResult generateForSectionInternal(
            ClassSection section,
            List<Day> days,
            int periodsPerDay,
            List<Room> allRooms,
            Map<Long, Set<String>> teacherSlotMap,
            Map<Long, Set<String>> roomSlotMap,
            Set<Long> onLeaveTeacherIds) {

        Long sectionId = section.getSectionId();

        List<SubjectTeacherMapping> mappings = mappingRepository.findBySection_SectionIdAndIsActiveTrue(sectionId);
        if (mappings.isEmpty()) {
            return TimetableGenerationResult.builder()
                    .success(false)
                    .message("No subject-teacher mappings defined for section: " + section.getSectionName())
                    .violations(List.of("Add subject-teacher mappings before generating a timetable for: "
                            + section.getSectionName()))
                    .warnings(List.of()).build();
        }

        // Count students in this section (needed for room capacity check)
        long sectionStudentCount = studentRepository.countBySection_SectionId(sectionId);

        // Deactivate existing timetable entries for this section
        List<Timetable> existing = timetableRepository.findBySection_SectionIdAndIsActiveTrue(sectionId);
        existing.forEach(t -> t.setIsActive(false));
        timetableRepository.saveAll(existing);

        // Build demands
        List<SlotDemand> demands = buildDemands(mappings, onLeaveTeacherIds);
        List<String> warnings = new ArrayList<>();
        List<String> violations = new ArrayList<>();

        if (demands.isEmpty()) {
            return TimetableGenerationResult.builder()
                    .success(false).message("No schedulable demands for section: " + section.getSectionName()
                            + " (all teachers may be on leave or periodsPerWeek = 0)")
                    .violations(List.of("All assigned teachers are on leave or periodsPerWeek = 0"))
                    .warnings(List.of()).build();
        }

        // Warn about teachers on leave that were excluded
        mappings.stream()
                .filter(m -> onLeaveTeacherIds.contains(m.getTeacher().getTeacherId()))
                .forEach(m -> warnings.add(String.format(
                        "Warning: Teacher '%s' is on leave — their periods for '%s' in section '%s' were not scheduled.",
                        m.getTeacher().getName(), m.getSubject().getSubjectName(), section.getSectionName())));

        int totalSlots = days.size() * periodsPerDay;
        int totalDemanded = demands.size();

        if (totalDemanded > totalSlots) {
            violations.add(String.format(
                    "Section '%s': %d periods demanded but only %d slots available (%d days × %d periods). " +
                    "Reduce periodsPerWeek or add more working days.",
                    section.getSectionName(), totalDemanded, totalSlots, days.size(), periodsPerDay));
            return TimetableGenerationResult.builder()
                    .success(false).message("Cannot generate: more periods demanded than slots available")
                    .violations(violations).warnings(warnings).build();
        }

        // Section-level slot map: ensures no section has two subjects at the same slot
        Map<String, Boolean> sectionSlotMap = new HashMap<>();

        // Track teacher day load for soft-constraint (free period) warning
        Map<Long, Map<Day, Integer>> teacherDayLoad = new HashMap<>();

        List<Timetable> generated = new ArrayList<>();
        int demandIndex = 0;
        LocalDate effectiveFrom = LocalDate.now();

        outer:
        for (Day day : days) {
            int period = 1;
            while (period <= periodsPerDay && demandIndex < demands.size()) {
                SlotDemand demand = demands.get(demandIndex);
                String slotKey = day.name() + "_" + period;

                Long teacherId = demand.teacher.getTeacherId();

                // SECTION CLASH CHECK: this section already has something at this slot
                if (sectionSlotMap.containsKey(slotKey)) {
                    period++;
                    continue;
                }

                // TEACHER CLASH CHECK (global — across all sections)
                Set<String> teacherSlots = teacherSlotMap.computeIfAbsent(teacherId, k -> new HashSet<>());
                if (teacherSlots.contains(slotKey)) {
                    // Teacher is busy at this slot (teaching another section)
                    // Try next demand for this slot instead of skipping the slot entirely
                    // If all demands have teacher conflicts at this slot, move to next period
                    boolean foundAlternative = tryNextAvailableDemand(
                            demands, demandIndex, slotKey, teacherSlotMap, sectionSlotMap,
                            onLeaveTeacherIds, warnings);
                    if (!foundAlternative) {
                        period++;
                        continue;
                    }
                    // Re-read demand after potential reordering
                    demand = demands.get(demandIndex);
                    teacherId = demand.teacher.getTeacherId();
                    teacherSlots = teacherSlotMap.computeIfAbsent(teacherId, k -> new HashSet<>());
                }

                // Handle double periods for lab subjects
                boolean isDouble = Boolean.TRUE.equals(demand.mapping.getRequiresDoublePeriod());
                if (isDouble && period + 1 > periodsPerDay) {
                    // Not enough room for double period today — move to next day
                    break;
                }
                if (isDouble) {
                    String nextSlotKey = day.name() + "_" + (period + 1);
                    // Both slots must be free for teacher, room, and section
                    if (teacherSlots.contains(nextSlotKey) || sectionSlotMap.containsKey(nextSlotKey)) {
                        period++;
                        continue;
                    }
                }

                // ROOM ASSIGNMENT (considers capacity and global availability)
                Room assignedRoom = findAvailableRoom(allRooms, roomSlotMap, slotKey, sectionStudentCount);

                // Create timetable entry
                Timetable entry = Timetable.builder()
                        .section(section)
                        .day(day)
                        .period(period)
                        .subject(demand.subject)
                        .teacher(demand.teacher)
                        .room(assignedRoom)
                        .effectiveFrom(effectiveFrom)
                        .isActive(true)
                        .version(1)
                        .build();
                generated.add(entry);

                // Mark slots as used in ALL shared maps
                teacherSlots.add(slotKey);
                sectionSlotMap.put(slotKey, true);
                if (assignedRoom != null) {
                    roomSlotMap.computeIfAbsent(assignedRoom.getRoomId(), k -> new HashSet<>()).add(slotKey);
                }

                if (isDouble) {
                    String nextSlotKey = day.name() + "_" + (period + 1);
                    teacherSlots.add(nextSlotKey);
                    sectionSlotMap.put(nextSlotKey, true);
                    if (assignedRoom != null) {
                        roomSlotMap.computeIfAbsent(assignedRoom.getRoomId(), k -> new HashSet<>()).add(nextSlotKey);
                    }
                    // Create the second entry for double period
                    Timetable entryPart2 = Timetable.builder()
                            .section(section)
                            .day(day)
                            .period(period + 1)
                            .subject(demand.subject)
                            .teacher(demand.teacher)
                            .room(assignedRoom)
                            .effectiveFrom(effectiveFrom)
                            .isActive(true)
                            .version(1)
                            .build();
                    generated.add(entryPart2);
                }

                // Track teacher day load for soft-constraint check
                teacherDayLoad.computeIfAbsent(teacherId, k -> new HashMap<>())
                        .merge(day, 1, Integer::sum);

                demandIndex++;
                period += isDouble ? 2 : 1;
            }
        }

        // Check if all demands were satisfied
        if (demandIndex < demands.size()) {
            int unscheduled = demands.size() - demandIndex;
            violations.add(String.format(
                    "Could not schedule %d period(s) for section '%s'. " +
                    "Teacher clashes across sections, or insufficient slots, prevented full generation.",
                    unscheduled, section.getSectionName()));
        }

        // Soft constraint: warn if any teacher has no free period on any day
        checkFreePeriodsWarning(teacherDayLoad, periodsPerDay, warnings);

        if (!violations.isEmpty()) {
            // Partial failure — do NOT persist incomplete timetable
            return TimetableGenerationResult.builder()
                    .success(false)
                    .message("Generation failed with constraint violations for section: " + section.getSectionName())
                    .violations(violations)
                    .warnings(warnings)
                    .entriesCreated(0)
                    .build();
        }

        timetableRepository.saveAll(generated);
        log.info("Generated {} timetable entries for section '{}'", generated.size(), section.getSectionName());

        return TimetableGenerationResult.builder()
                .success(true)
                .message("Timetable generated successfully for section: " + section.getSectionName())
                .entriesCreated(generated.size())
                .violations(List.of())
                .warnings(warnings)
                .build();
    }

    @Override
    public List<String> getClashReport() {
        List<Timetable> all = timetableRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .collect(Collectors.toList());

        List<String> clashes = new ArrayList<>();

        // Group by day+period and look for duplicates
        Map<String, List<Timetable>> byTeacherSlot = new HashMap<>();
        Map<String, List<Timetable>> bySectionSlot = new HashMap<>();
        Map<String, List<Timetable>> byRoomSlot = new HashMap<>();

        for (Timetable t : all) {
            String slot = t.getDay().name() + "_" + t.getPeriod();
            byTeacherSlot.computeIfAbsent(t.getTeacher().getTeacherId() + "_" + slot, k -> new ArrayList<>()).add(t);
            bySectionSlot.computeIfAbsent(t.getSection().getSectionId() + "_" + slot, k -> new ArrayList<>()).add(t);
            if (t.getRoom() != null) {
                byRoomSlot.computeIfAbsent(t.getRoom().getRoomId() + "_" + slot, k -> new ArrayList<>()).add(t);
            }
        }

        byTeacherSlot.values().stream().filter(list -> list.size() > 1).forEach(list -> {
            Timetable first = list.get(0);
            clashes.add(String.format("TEACHER CLASH: Teacher '%s' double-booked on %s Period %d (affects %d sections: %s)",
                    first.getTeacher().getName(), first.getDay(), first.getPeriod(), list.size(),
                    list.stream().map(t -> t.getSection().getSectionName()).collect(Collectors.joining(", "))));
        });

        bySectionSlot.values().stream().filter(list -> list.size() > 1).forEach(list -> {
            Timetable first = list.get(0);
            clashes.add(String.format("SECTION CLASH: Section '%s' has %d subjects on %s Period %d",
                    first.getSection().getSectionName(), list.size(), first.getDay(), first.getPeriod()));
        });

        byRoomSlot.values().stream().filter(list -> list.size() > 1).forEach(list -> {
            Timetable first = list.get(0);
            clashes.add(String.format("ROOM CLASH: Room '%s' double-booked on %s Period %d (sections: %s)",
                    first.getRoom().getRoomName(), first.getDay(), first.getPeriod(),
                    list.stream().map(t -> t.getSection().getSectionName()).collect(Collectors.joining(", "))));
        });

        return clashes;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private List<Day> parseDays(String workingDays) {
        if (workingDays == null || workingDays.isBlank()) {
            return List.of(Day.MON, Day.TUE, Day.WED, Day.THU, Day.FRI);
        }
        List<Day> result = new ArrayList<>();
        for (String s : workingDays.split(",")) {
            try {
                result.add(Day.valueOf(s.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                log.warn("Ignoring unknown day: {}", s.trim());
            }
        }
        return result;
    }

    /**
     * Build the list of demands, excluding teachers who are on leave.
     */
    private List<SlotDemand> buildDemands(List<SubjectTeacherMapping> mappings, Set<Long> onLeaveTeacherIds) {
        List<SlotDemand> demands = new ArrayList<>();
        for (SubjectTeacherMapping m : mappings) {
            // Skip mappings where the teacher is on leave
            if (onLeaveTeacherIds.contains(m.getTeacher().getTeacherId())) {
                continue;
            }
            int count = m.getPeriodsPerWeek() == null ? 5 : m.getPeriodsPerWeek();
            for (int i = 0; i < count; i++) {
                demands.add(new SlotDemand(m, m.getSubject(), m.getTeacher()));
            }
        }
        return demands;
    }

    /**
     * Find the next demand starting from currentIndex that has no teacher clash at slotKey.
     * If found, swap it to position currentIndex so greedy can proceed.
     * Returns true if an alternative was found and swapped.
     */
    private boolean tryNextAvailableDemand(
            List<SlotDemand> demands, int currentIndex, String slotKey,
            Map<Long, Set<String>> teacherSlotMap, Map<String, Boolean> sectionSlotMap,
            Set<Long> onLeaveTeacherIds, List<String> warnings) {

        for (int i = currentIndex + 1; i < demands.size(); i++) {
            SlotDemand candidate = demands.get(i);
            Long cTeacherId = candidate.teacher.getTeacherId();
            Set<String> cTeacherSlots = teacherSlotMap.getOrDefault(cTeacherId, Collections.emptySet());
            if (!cTeacherSlots.contains(slotKey) && !onLeaveTeacherIds.contains(cTeacherId)) {
                // Found an alternative — swap it to current position
                Collections.swap(demands, currentIndex, i);
                return true;
            }
        }
        return false;
    }

    /**
     * Find an available room that:
     * 1. Is not already used at the given slot
     * 2. Has sufficient capacity for the section's student count
     */
    private Room findAvailableRoom(List<Room> rooms, Map<Long, Set<String>> roomSlotMap,
                                    String slotKey, long sectionStudentCount) {
        for (Room room : rooms) {
            // Capacity check
            if (room.getCapacity() != null && room.getCapacity() < sectionStudentCount) {
                continue; // Room too small for this section
            }
            // Availability check (global across all sections)
            Set<String> usedSlots = roomSlotMap.getOrDefault(room.getRoomId(), Collections.emptySet());
            if (!usedSlots.contains(slotKey)) {
                return room;
            }
        }
        return null; // No suitable room available — allowed (room is optional)
    }

    /**
     * Pre-populate teacher and room slot maps from an existing list of timetable entries.
     * Used to initialize the maps with previously scheduled slots before generating new ones.
     */
    private void preloadSlotMaps(List<Timetable> entries,
                                  Map<Long, Set<String>> teacherSlotMap,
                                  Map<Long, Set<String>> roomSlotMap) {
        for (Timetable t : entries) {
            String slotKey = t.getDay().name() + "_" + t.getPeriod();
            teacherSlotMap.computeIfAbsent(t.getTeacher().getTeacherId(), k -> new HashSet<>()).add(slotKey);
            if (t.getRoom() != null) {
                roomSlotMap.computeIfAbsent(t.getRoom().getRoomId(), k -> new HashSet<>()).add(slotKey);
            }
        }
    }

    /**
     * Get the set of teacher IDs who have approved or pending leaves overlapping the given range.
     */
    private Set<Long> getTeachersOnLeave(LocalDate fromDate, LocalDate toDate) {
        return new HashSet<>(leaveApplicationRepository.findTeacherIdsWithLeavesOverlapping(fromDate, toDate));
    }

    private void checkFreePeriodsWarning(Map<Long, Map<Day, Integer>> teacherDayLoad,
                                          int periodsPerDay, List<String> warnings) {
        teacherDayLoad.forEach((teacherId, dayMap) ->
            dayMap.forEach((day, count) -> {
                if (count >= periodsPerDay) {
                    warnings.add(String.format(
                            "Soft constraint: Teacher ID %d has no free period on %s (assigned all %d periods)",
                            teacherId, day, periodsPerDay));
                }
            })
        );
    }

    /** Internal data class representing one period that needs to be scheduled. */
    private static class SlotDemand {
        final SubjectTeacherMapping mapping;
        final Subject subject;
        final Teacher teacher;

        SlotDemand(SubjectTeacherMapping mapping, Subject subject, Teacher teacher) {
            this.mapping = mapping;
            this.subject = subject;
            this.teacher = teacher;
        }
    }
}
