package model;

public class TeacherProgress {
    private String teacherId;
    private String teacherName;
    private String subject;
    private String grade;
    private double progressPercentage;
    private int completedLessons;
    private int totalLessons;
    private String status; // ON_TRACK, AT_RISK, BEHIND
    
    public TeacherProgress() {
    }
    
    public TeacherProgress(String teacherId, String teacherName, String subject, String grade, 
                          double progressPercentage, int completedLessons, int totalLessons, String status) {
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.subject = subject;
        this.grade = grade;
        this.progressPercentage = progressPercentage;
        this.completedLessons = completedLessons;
        this.totalLessons = totalLessons;
        this.status = status;
    }
    
    // Getters and Setters
    public String getTeacherId() {
        return teacherId;
    }
    
    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }
    
    public String getTeacherName() {
        return teacherName;
    }
    
    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getGrade() {
        return grade;
    }
    
    public void setGrade(String grade) {
        this.grade = grade;
    }
    
    public double getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public int getCompletedLessons() {
        return completedLessons;
    }
    
    public void setCompletedLessons(int completedLessons) {
        this.completedLessons = completedLessons;
    }
    
    public int getTotalLessons() {
        return totalLessons;
    }
    
    public void setTotalLessons(int totalLessons) {
        this.totalLessons = totalLessons;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getLessonCountDisplay() {
        return completedLessons + "/" + totalLessons;
    }
    
    public String getProgressDisplay() {
        return String.format("%.1f%%", progressPercentage);
    }
}