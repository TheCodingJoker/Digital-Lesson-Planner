package model;

public class ProgressReport {

    private String reportId;
    private String teacherId;
    private String subject;
    private double completionPercentage;
    private int totalLessons;
    private int completedLessons;
    private String generatedAt;
    private String term;

    public ProgressReport() {}

    public ProgressReport(String reportId, String teacherId, String subject,
                         double completionPercentage, int totalLessons,
                         int completedLessons, String generatedAt, String term) {
        this.reportId = reportId;
        this.teacherId = teacherId;
        this.subject = subject;
        this.completionPercentage = completionPercentage;
        this.totalLessons = totalLessons;
        this.completedLessons = completedLessons;
        this.generatedAt = generatedAt;
        this.term = term;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(int totalLessons) {
        this.totalLessons = totalLessons;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public void setCompletedLessons(int completedLessons) {
        this.completedLessons = completedLessons;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getCompletionDisplay() {
        return String.format("%.1f%%", completionPercentage);
    }

    public String getLessonCountDisplay() {
        return completedLessons + "/" + totalLessons;
    }
}
