
package model;

public class LessonPlan {

    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_EXTENDED = "EXTENDED";

    private String lessonPlanId;
    private String teacherId;
    private String title;       // mirrors topic - the mockup has no separate title field
    private String subject;
    private String gradeLevel;
    private String term;        // Term 1-4 (stored in the curriculumReference column)
    private String topic;
    private int durationMinutes;
    private String objectives;
    private String teachingActivities;
    private String assessmentMethod;
    private String resources;
    private String lessonDate;  // ISO format yyyy-MM-dd
    private String status;
    private String createdAt;
    private String updatedAt;

    public LessonPlan() {}

    public String getLessonPlanId() { return lessonPlanId; }
    public void setLessonPlanId(String lessonPlanId) { this.lessonPlanId = lessonPlanId; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getObjectives() { return objectives; }
    public void setObjectives(String objectives) { this.objectives = objectives; }

    public String getTeachingActivities() { return teachingActivities; }
    public void setTeachingActivities(String teachingActivities) { this.teachingActivities = teachingActivities; }

    public String getAssessmentMethod() { return assessmentMethod; }
    public void setAssessmentMethod(String assessmentMethod) { this.assessmentMethod = assessmentMethod; }

    public String getResources() { return resources; }
    public void setResources(String resources) { this.resources = resources; }

    public String getLessonDate() { return lessonDate; }
    public void setLessonDate(String lessonDate) { this.lessonDate = lessonDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
