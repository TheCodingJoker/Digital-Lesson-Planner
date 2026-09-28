package service;

import dao.LessonPlanDAO;
import dao.SchoolEventDAO;
import model.LessonPlan;
import model.SchoolEvent;
import util.ErrorLogger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SchedulingEngine {
    
    private static SchedulingEngine instance;
    private LessonPlanDAO lessonPlanDAO;
    private SchoolEventDAO schoolEventDAO;
    private ErrorLogger errorLogger;
    private static final int TOTAL_TEACHING_DAYS = 180;
    
    private SchedulingEngine() {
        this.lessonPlanDAO = new LessonPlanDAO();
        this.schoolEventDAO = new SchoolEventDAO();
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
            List<LocalDate> availableDays = calculateAvailableTeachingDays(startDate, schoolEvents);
            
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
     */
    public void handleLessonStatusChange(String lessonId, String newStatus) {
        try {
            LessonPlan lesson = lessonPlanDAO.findById(lessonId);
            if (lesson == null) {
                errorLogger.logWarning("SchedulingEngine", "handleLessonStatusChange", 
                    "Lesson not found: " + lessonId);
                return;
            }
            
            // Update the lesson status
            lesson.setStatus(newStatus);
            lessonPlanDAO.updateStatus(lessonId, newStatus);
            
            // If lesson is incomplete or extended, apply Push-Back Protocol
            if (LessonPlan.STATUS_INCOMPLETE.equals(newStatus) || 
                LessonPlan.STATUS_EXTENDED.equals(newStatus)) {
                applyPushBackProtocol(lesson);
            }
            
            errorLogger.logInfo("SchedulingEngine", "handleLessonStatusChange", 
                "Lesson status updated: " + lessonId + " -> " + newStatus);
                
        } catch (Exception e) {
            errorLogger.logError("SchedulingEngine", "handleLessonStatusChange", 
                "Failed to handle status change for lesson: " + lessonId, e);
        }
    }
    
    /**
     * Apply Push-Back Protocol - reschedule subsequent lessons
     */
    private void applyPushBackProtocol(LessonPlan affectedLesson) {
        try {
            String teacherId = affectedLesson.getTeacherId();
            LocalDate affectedDate = LocalDate.parse(affectedLesson.getLessonDate());
            
            // Get all lessons for the teacher after the affected date
            List<LessonPlan> subsequentLessons = getLessonsAfterDate(teacherId, affectedDate);
            
            // Get school events to determine available days
            List<SchoolEvent> schoolEvents = schoolEventDAO.getAllSchoolEvents();
            
            // Reschedule each subsequent lesson to the next available teaching day
            LocalDate nextAvailableDate = affectedDate.plusDays(1);
            
            for (LessonPlan lesson : subsequentLessons) {
                nextAvailableDate = findNextAvailableTeachingDay(nextAvailableDate, schoolEvents);
                
                if (nextAvailableDate != null) {
                    String oldDate = lesson.getLessonDate();
                    lesson.setLessonDate(nextAvailableDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    lesson.setStatus(LessonPlan.STATUS_RESCHEDULED);
                    
                    // Add rescheduling note
                    String rescheduleNote = "Rescheduled due to lesson change on " + 
                        affectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    
                    lessonPlanDAO.updateLessonPlan(lesson);
                    
                    errorLogger.logInfo("SchedulingEngine", "applyPushBackProtocol", 
                        "Rescheduled lesson " + lesson.getLessonPlanId() + " from " + oldDate + 
                        " to " + lesson.getLessonDate());
                    
                    nextAvailableDate = nextAvailableDate.plusDays(1);
                } else {
                    // Term boundary reached
                    errorLogger.logWarning("SchedulingEngine", "applyPushBackProtocol", 
                        "Term boundary reached - cannot reschedule lesson: " + lesson.getLessonPlanId());
                    lesson.setStatus("CONSOLIDATION_REQUIRED");
                    lessonPlanDAO.updateStatus(lesson.getLessonPlanId(), "CONSOLIDATION_REQUIRED");
                }
            }
            
        } catch (Exception e) {
            errorLogger.logError("SchedulingEngine", "applyPushBackProtocol", 
                "Failed to apply push-back protocol", e);
        }
    }
    
    /**
     * Find the next available teaching day considering school events
     */
    private LocalDate findNextAvailableTeachingDay(LocalDate startDate, List<SchoolEvent> schoolEvents) {
        LocalDate currentDate = startDate;
        int maxDaysToCheck = 365; // Search up to one year ahead
        
        for (int i = 0; i < maxDaysToCheck; i++) {
            if (isAvailableTeachingDay(currentDate, schoolEvents)) {
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
    private List<LocalDate> calculateAvailableTeachingDays(LocalDate startDate, List<SchoolEvent> schoolEvents) {
        List<LocalDate> availableDays = new ArrayList<>();
        LocalDate currentDate = startDate;
        int daysChecked = 0;
        
        while (availableDays.size() < TOTAL_TEACHING_DAYS && daysChecked < 365) {
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
                if (lessonDate.isAfter(date)) {
                    subsequentLessons.add(lesson);
                }
            }
        }
        
        // Sort by date
        subsequentLessons.sort((a, b) -> {
            LocalDate dateA = LocalDate.parse(a.getLessonDate());
            LocalDate dateB = LocalDate.parse(b.getLessonDate());
            return dateA.compareTo(dateB);
        });
        
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
     * Handle event deletion - check for affected lessons
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
                // In a real implementation, this would trigger a notification to teachers
            }
            
        } catch (Exception e) {
            errorLogger.logError("SchedulingEngine", "handleEventDeletion", 
                "Failed to handle event deletion: " + eventId, e);
        }
    }
    
    /**
     * Find lessons that were rescheduled due to a specific event
     */
    private List<LessonPlan> findLessonsRescheduledDueToEvent(String eventDate) {
        // This would need to search lesson notes for rescheduling references
        // For now, return empty list as notes aren't fully implemented
        return new ArrayList<>();
    }
}