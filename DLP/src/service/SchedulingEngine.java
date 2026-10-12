package service;

import dao.LessonPlanDAO;
import dao.SchoolEventDAO;
import dao.SchoolCalendarDAO;
import dao.NotificationDAO;
import dao.UserDAO;
import model.LessonPlan;
import model.SchoolEvent;
import model.SchoolCalendar;
import model.Notification;
import model.User;
import util.ErrorLogger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SchedulingEngine {

    private static SchedulingEngine instance;
    private LessonPlanDAO lessonPlanDAO;
    private SchoolEventDAO schoolEventDAO;
    private SchoolCalendarDAO schoolCalendarDAO;
    private NotificationDAO notificationDAO;
    private UserDAO userDAO;
    private ErrorLogger errorLogger;
    
    private SchedulingEngine() {
        this.lessonPlanDAO = new LessonPlanDAO();
        this.schoolEventDAO = new SchoolEventDAO();
        this.schoolCalendarDAO = new SchoolCalendarDAO();
        this.notificationDAO = new NotificationDAO();
        this.userDAO = new UserDAO();
        this.errorLogger = ErrorLogger.getInstance();
    }
    
    public static synchronized SchedulingEngine getInstance() {
        if (instance == null) {
            instance = new SchedulingEngine();
        }
        return instance;
    }
    
    /**
     * Distribute lesson plans across available teaching days
     * @param teacherId The teacher's user ID
     * @param academicYear The academic year (e.g., 2026)
     * @param startDate The start date of the academic year
     */
    public void distributeLessons(String teacherId, int academicYear, LocalDate startDate) {
        try {
            // Get all unscheduled lessons for the teacher
            List<LessonPlan> unscheduledLessons = getUnscheduledLessons(teacherId);
            
            if (unscheduledLessons.isEmpty()) {
                errorLogger.logInfo("SchedulingEngine", "distributeLessons", 
                    "No unscheduled lessons found for teacher: " + teacherId);
                return;
            }
            
            // Get school events for the academic year
            List<SchoolEvent> schoolEvents = getSchoolEvents(academicYear);
            
            // Calculate available teaching days
            List<LocalDate> availableDays = calculateAvailableTeachingDays(startDate, schoolEvents, academicYear);
            
            if (availableDays.size() < unscheduledLessons.size()) {
                errorLogger.logWarning("SchedulingEngine", "distributeLessons", 
                    "Not enough teaching days available. Available: " + availableDays.size() + 
                    ", Lessons: " + unscheduledLessons.size());
                return;
            }
            
            // Assign dates to lessons
            for (int i = 0; i < unscheduledLessons.size() && i < availableDays.size(); i++) {
                LessonPlan lesson = unscheduledLessons.get(i);
                LocalDate scheduledDate = availableDays.get(i);
                
                lesson.setLessonDate(scheduledDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                lesson.setStatus(LessonPlan.STATUS_SCHEDULED);
                
                lessonPlanDAO.updateLessonPlan(lesson);
            }
            
            errorLogger.logInfo("SchedulingEngine", "distributeLessons", 
                "Successfully distributed " + unscheduledLessons.size() + " lessons for teacher: " + teacherId);
                
        } catch (Exception e) {
            errorLogger.logError("SchedulingEngine", "distributeLessons", 
                "Failed to distribute lessons for teacher: " + teacherId, e);
        }
    }
    
    /**
     * Handle lesson status change and apply Push-Back Protocol
     * @param lessonId The lesson plan ID
     * @param newStatus The new status (COMPLETED, INCOMPLETE, EXTENDED)
     * @return whether the status change and any required rescheduling were saved
     */
    public boolean handleLessonStatusChange(String lessonId, String newStatus) {
        try {
            LessonPlan lesson = lessonPlanDAO.findById(lessonId);
            if (lesson == null) {
                errorLogger.logWarning("SchedulingEngine", "handleLessonStatusChange",
                    "Lesson not found: " + lessonId);
                return false;
            }

            if (!lessonPlanDAO.updateStatus(lessonId, newStatus)) {
                errorLogger.logWarning("SchedulingEngine", "handleLessonStatusChange",
                    "Could not save status for lesson: " + lessonId);
                return false;
            }
            lesson.setStatus(newStatus);

            // If lesson is incomplete or extended, apply Push-Back Protocol
            if (LessonPlan.STATUS_INCOMPLETE.equals(newStatus) ||
                LessonPlan.STATUS_EXTENDED.equals(newStatus)) {
                if (lesson.getLessonDate() != null && !lesson.getLessonDate().isEmpty()
                        && !applyPushBackProtocol(lesson)) {
                    return false;
                }
            }

            errorLogger.logInfo("SchedulingEngine", "handleLessonStatusChange",
                "Lesson status updated: " + lessonId + " -> " + newStatus);
            return true;

        } catch (Exception e) {
            errorLogger.logError("SchedulingEngine", "handleLessonStatusChange",
                "Failed to handle status change for lesson: " + lessonId, e);
            return false;
        }
    }

    /**
     * Apply half-day event constraints - ensure only one lesson on half-days
     * This should be called after distributing lessons or when events are added
     */
    public void applyHalfDayConstraints(int academicYear) {
        try {
            List<SchoolEvent> schoolEvents = getSchoolEvents(academicYear);
            List<SchoolEvent> halfDayEvents = new ArrayList<>();

            // Filter for half-day events
            for (SchoolEvent event : schoolEvents) {
                if ("HALF_DAY".equals(event.getEventType())) {
                    halfDayEvents.add(event);
                }
            }

            // For each half-day event, ensure only one lesson is scheduled
            for (SchoolEvent halfDayEvent : halfDayEvents) {
                LocalDate eventDate = LocalDate.parse(halfDayEvent.getEventDate());

                // Get all lessons scheduled on this date
                List<LessonPlan> lessonsOnDate = getLessonsOnDate(eventDate);

                if (lessonsOnDate.size() > 1) {
                    // Keep only the first lesson, reschedule the rest
                    LessonPlan firstLesson = lessonsOnDate.get(0);
                    List<LessonPlan> lessonsToReschedule = lessonsOnDate.subList(1, lessonsOnDate.size());

                    for (LessonPlan lesson : lessonsToReschedule) {
                        // Reschedule to next available day
                        LocalDate nextAvailableDay = findNextAvailableTeachingDay(eventDate, schoolEvents);

                        if (nextAvailableDay != null) {
                            String oldDate = lesson.getLessonDate();
                            lesson.setLessonDate(nextAvailableDay.format(DateTimeFormatter.ISO_LOCAL_DATE));
                            lesson.setStatus(LessonPlan.STATUS_RESCHEDULED);

                            // Add rescheduling note
                            String rescheduleNote = "Rescheduled due to half-day event on " +
                                eventDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
                            lesson.setReschedulingNote(rescheduleNote);

                            lessonPlanDAO.updateLessonPlan(lesson);

                            errorLogger.logInfo("SchedulingEngine", "applyHalfDayConstraints",
                                "Rescheduled lesson " + lesson.getLessonPlanId() + " from " + oldDate +
                                " to " + lesson.getLessonDate() + " (half-day constraint)");
                        } else {
                            // Term boundary reached
                            errorLogger.logWarning("SchedulingEngine", "applyHalfDayConstraints",
                                "Term boundary reached - cannot reschedule lesson: " + lesson.getLessonPlanId());
                            lesson.setStatus("CONSOLIDATION_REQUIRED");
                            lessonPlanDAO.updateStatus(lesson.getLessonPlanId(), "CONSOLIDATION_REQUIRED");
                        }
                    }
                }
            }

        } catch (Exception e) {
            errorLogger.logError("SchedulingEngine", "applyHalfDayConstraints",
                "Failed to apply half-day constraints", e);
        }
    }

    /**
     * Get all lessons scheduled on a specific date
     */
    private List<LessonPlan> getLessonsOnDate(LocalDate date) {
        List<LessonPlan> allLessons = lessonPlanDAO.getAllLessonPlans();
        List<LessonPlan> lessonsOnDate = new ArrayList<>();

        String dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

        for (LessonPlan lesson : allLessons) {
            if (dateString.equals(lesson.getLessonDate())) {
                lessonsOnDate.add(lesson);
            }
        }

        // Sort by creation time to ensure consistent ordering
        lessonsOnDate.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });

        return lessonsOnDate;
    }
    
    /**
     * Apply Push-Back Protocol - reschedule subsequent lessons
     */
    private boolean applyPushBackProtocol(LessonPlan affectedLesson) {
        try {
            String teacherId = affectedLesson.getTeacherId();
            LocalDate affectedDate = LocalDate.parse(affectedLesson.getLessonDate());
            
            List<LessonPlan> teacherLessons = lessonPlanDAO.findByTeacher(teacherId);
            List<LessonPlan> subsequentLessons = getLessonsAfterDate(teacherId, affectedDate);
            List<SchoolEvent> schoolEvents = schoolEventDAO.getAllSchoolEvents();

            Set<String> subsequentLessonIds = new HashSet<>();
            for (LessonPlan lesson : subsequentLessons) {
                subsequentLessonIds.add(lesson.getLessonPlanId());
            }

            Set<LocalDate> occupiedDates = new HashSet<>();
            for (LessonPlan lesson : teacherLessons) {
                if (!subsequentLessonIds.contains(lesson.getLessonPlanId())
                        && lesson.getLessonDate() != null && !lesson.getLessonDate().isEmpty()) {
                    occupiedDates.add(LocalDate.parse(lesson.getLessonDate()));
                }
            }

            for (LessonPlan lesson : subsequentLessons) {
                LocalDate oldDate = LocalDate.parse(lesson.getLessonDate());
                LocalDate earliestDate = oldDate.isAfter(affectedDate)
                    ? oldDate.plusDays(1) : affectedDate.plusDays(1);
                LocalDate nextAvailableDate = findNextAvailableTeachingDay(
                    earliestDate, schoolEvents, occupiedDates);

                if (nextAvailableDate != null) {
                    lesson.setLessonDate(nextAvailableDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    lesson.setStatus(LessonPlan.STATUS_RESCHEDULED);

                    appendReschedulingNote(lesson,
                        "Rescheduled from " + oldDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            + " to " + lesson.getLessonDate()
                            + " because lesson on " + affectedDate
                            + " was marked " + affectedLesson.getStatus() + ".");

                    if (!lessonPlanDAO.updateLessonPlan(lesson)) {
                        errorLogger.logError("SchedulingEngine", "applyPushBackProtocol",
                            "Could not save rescheduled lesson: " + lesson.getLessonPlanId(), null);
                        return false;
                    }
                    occupiedDates.add(nextAvailableDate);

                    errorLogger.logInfo("SchedulingEngine", "applyPushBackProtocol",
                        "Rescheduled lesson " + lesson.getLessonPlanId() + " from "
                            + oldDate.format(DateTimeFormatter.ISO_LOCAL_DATE) +
                        " to " + lesson.getLessonDate());
                } else {
                    errorLogger.logWarning("SchedulingEngine", "applyPushBackProtocol",
                        "No available teaching day found within one year for lesson: "
                            + lesson.getLessonPlanId());
                    lesson.setStatus("CONSOLIDATION_REQUIRED");
                    if (!lessonPlanDAO.updateStatus(
                            lesson.getLessonPlanId(), "CONSOLIDATION_REQUIRED")) {
                        errorLogger.logError("SchedulingEngine", "applyPushBackProtocol",
                            "Could not save consolidation status for lesson: "
                                + lesson.getLessonPlanId(), null);
                    }
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            errorLogger.logError("SchedulingEngine", "applyPushBackProtocol", 
                "Failed to apply push-back protocol", e);
            return false;
        }
    }

    private void appendReschedulingNote(LessonPlan lesson, String note) {
        String existingNote = lesson.getReschedulingNote();
        lesson.setReschedulingNote(existingNote == null || existingNote.trim().isEmpty()
            ? note
            : existingNote + System.lineSeparator() + note);
    }
    
    /**
     * Find the next available teaching day considering school events
     */
    private LocalDate findNextAvailableTeachingDay(LocalDate startDate, List<SchoolEvent> schoolEvents) {
        return findNextAvailableTeachingDay(startDate, schoolEvents, Collections.emptySet());
    }

    private LocalDate findNextAvailableTeachingDay(
            LocalDate startDate, List<SchoolEvent> schoolEvents, Set<LocalDate> occupiedDates) {
        LocalDate currentDate = startDate;
        int maxDaysToCheck = 365; // Search up to one year ahead
        
        for (int i = 0; i < maxDaysToCheck; i++) {
            if (!occupiedDates.contains(currentDate)
                    && isAvailableTeachingDay(currentDate, schoolEvents)) {
                return currentDate;
            }
            currentDate = currentDate.plusDays(1);
        }
        
        return null; // No available day found within limit
    }
    
    /**
     * Check if a date is available for teaching
     */
    private boolean isAvailableTeachingDay(LocalDate date, List<SchoolEvent> schoolEvents) {
        // Skip weekends (Saturday and Sunday)
        if (date.getDayOfWeek().getValue() >= 6) {
            return false;
        }
        
        // Check for school events
        for (SchoolEvent event : schoolEvents) {
            LocalDate eventDate = LocalDate.parse(event.getEventDate());
            if (date.equals(eventDate)) {
                // Full day events - no teaching
                if ("FULL_DAY".equals(event.getEventType())) {
                    return false;
                }
                // Half day events - one lesson slot available
                // For simplicity, we'll allow teaching on half days
            }
        }
        
        return true;
    }
    
    /**
     * Calculate available teaching days for an academic year
     */
    private List<LocalDate> calculateAvailableTeachingDays(
            LocalDate startDate, List<SchoolEvent> schoolEvents, int academicYear) {
        List<LocalDate> availableDays = new ArrayList<>();
        LocalDate currentDate = startDate;
        int daysChecked = 0;

        // Get total teaching days from school calendar
        SchoolCalendar calendar = schoolCalendarDAO.getSchoolCalendarByYear(academicYear);
        int totalTeachingDays = calendar != null ? calendar.getTotalTeachingDays() : 180;

        while (availableDays.size() < totalTeachingDays && daysChecked < 365) {
            if (isAvailableTeachingDay(currentDate, schoolEvents)) {
                availableDays.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
            daysChecked++;
        }

        return availableDays;
    }
    
    /**
     * Get unscheduled lessons for a teacher
     */
    private List<LessonPlan> getUnscheduledLessons(String teacherId) {
        List<LessonPlan> allLessons = lessonPlanDAO.findByTeacher(teacherId);
        List<LessonPlan> unscheduled = new ArrayList<>();
        
        for (LessonPlan lesson : allLessons) {
            if (lesson.getLessonDate() == null || lesson.getLessonDate().isEmpty()) {
                unscheduled.add(lesson);
            }
        }
        
        return unscheduled;
    }
    
    /**
     * Get lessons scheduled after a specific date
     */
    private List<LessonPlan> getLessonsAfterDate(String teacherId, LocalDate date) {
        List<LessonPlan> allLessons = lessonPlanDAO.findByTeacher(teacherId);
        List<LessonPlan> subsequentLessons = new ArrayList<>();
        
        for (LessonPlan lesson : allLessons) {
            if (lesson.getLessonDate() != null && !lesson.getLessonDate().isEmpty()) {
                LocalDate lessonDate = LocalDate.parse(lesson.getLessonDate());
                if (lessonDate.isAfter(date)
                        && !LessonPlan.STATUS_COMPLETED.equals(lesson.getStatus())) {
                    subsequentLessons.add(lesson);
                }
            }
        }
        
        subsequentLessons.sort(Comparator
            .comparing((LessonPlan lesson) -> LocalDate.parse(lesson.getLessonDate()))
            .thenComparing(LessonPlan::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        
        return subsequentLessons;
    }
    
    /**
     * Get school events for an academic year
     */
    private List<SchoolEvent> getSchoolEvents(int academicYear) {
        List<SchoolEvent> allEvents = schoolEventDAO.getAllSchoolEvents();
        List<SchoolEvent> yearEvents = new ArrayList<>();
        
        for (SchoolEvent event : allEvents) {
            if (event.getAcademicYear() == academicYear) {
                yearEvents.add(event);
            }
        }
        
        return yearEvents;
    }
    
    /**
     * Handle event deletion - check for affected lessons and notify teachers
     */
    public void handleEventDeletion(String eventId) {
        try {
            SchoolEvent deletedEvent = schoolEventDAO.getSchoolEventById(eventId);
            if (deletedEvent == null) {
                return;
            }

            // Check for lessons that were rescheduled due to this event
            String eventDate = deletedEvent.getEventDate();
            List<LessonPlan> affectedLessons = findLessonsRescheduledDueToEvent(eventDate);

            if (!affectedLessons.isEmpty()) {
                errorLogger.logInfo("SchedulingEngine", "handleEventDeletion",
                    "Event deletion affected " + affectedLessons.size() + " lessons");

                // Notify each teacher whose lessons were affected
                notifyTeachersAboutEventDeletion(deletedEvent, affectedLessons);
            }

        } catch (Exception e) {
            errorLogger.logError("SchedulingEngine", "handleEventDeletion",
                "Failed to handle event deletion: " + eventId, e);
        }
    }

    /**
     * Notify teachers about event deletion
     */
    private void notifyTeachersAboutEventDeletion(SchoolEvent deletedEvent, List<LessonPlan> affectedLessons) {
        // Group lessons by teacher
        Map<String, List<LessonPlan>> lessonsByTeacher = new HashMap<>();
        for (LessonPlan lesson : affectedLessons) {
            lessonsByTeacher.computeIfAbsent(lesson.getTeacherId(), k -> new ArrayList<>()).add(lesson);
        }

        // Send notification to each affected teacher
        for (Map.Entry<String, List<LessonPlan>> entry : lessonsByTeacher.entrySet()) {
            String teacherId = entry.getKey();
            List<LessonPlan> teacherLessons = entry.getValue();

            User teacher = userDAO.findById(teacherId);
            if (teacher == null) continue;

            String notificationId = UUID.randomUUID().toString();
            String title = "School Event Deleted";
            String message = String.format(
                "The event '%s' on %s has been deleted. %d lesson(s) that were previously " +
                "rescheduled due to this event may need to be moved back to %s.",
                deletedEvent.getEventName(),
                deletedEvent.getEventDate(),
                teacherLessons.size(),
                deletedEvent.getEventDate()
            );

            Notification notification = new Notification(
                notificationId,
                teacherId,
                "EVENT_DELETED",
                title,
                message,
                deletedEvent.getEventId()
            );

            notificationDAO.createNotification(notification);
            errorLogger.logInfo("SchedulingEngine", "notifyTeachersAboutEventDeletion",
                "Sent notification to teacher: " + teacher.getUsername());
        }
    }
    
    /**
     * Find lessons that were rescheduled due to a specific event
     */
    private List<LessonPlan> findLessonsRescheduledDueToEvent(String eventDate) {
        List<LessonPlan> affectedLessons = new ArrayList<>();
        List<LessonPlan> allLessons = lessonPlanDAO.getAllLessonPlans();

        // Search for lessons with rescheduling notes containing the event date
        for (LessonPlan lesson : allLessons) {
            String note = lesson.getReschedulingNote();
            if (note != null && note.contains(eventDate)) {
                affectedLessons.add(lesson);
            }
        }

        return affectedLessons;
    }

    /**
     * Calculate the number of teaching days remaining in the academic year
     * @return Number of teaching days remaining
     */
    public int getTeachingDaysRemaining() {
        try {
            SchoolCalendar calendar = schoolCalendarDAO.getCurrentOrDefaultCalendar();
            if (calendar == null) {
                return 180; // Default fallback
            }

            LocalDate today = LocalDate.now();
            LocalDate endDate = LocalDate.parse(calendar.getEndDate());

            if (today.isAfter(endDate)) {
                return 0;
            }

            // Get all school events for the current year
            List<SchoolEvent> schoolEvents = getSchoolEvents(calendar.getAcademicYear());

            // Count available teaching days from today to end date
            int teachingDaysRemaining = 0;
            LocalDate currentDate = today;

            while (!currentDate.isAfter(endDate)) {
                if (isAvailableTeachingDay(currentDate, schoolEvents)) {
                    teachingDaysRemaining++;
                }
                currentDate = currentDate.plusDays(1);
            }

            return teachingDaysRemaining;

        } catch (Exception e) {
            errorLogger.logError("SchedulingEngine", "getTeachingDaysRemaining", "Failed to calculate teaching days remaining", e);
            return 180; // Default fallback
        }
    }
}