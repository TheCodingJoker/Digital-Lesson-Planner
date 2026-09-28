package model;

public class CAPSEntry {
    private String capsCode;
    private String subject;
    private String gradeLevel;
    private int term;
    private String topic;
    private String outcomes;
    private String assessmentStandards;
    private String createdAt;

    public CAPSEntry() {}

    public String getCapsCode() { return capsCode; }
    public void setCapsCode(String capsCode) { this.capsCode = capsCode; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }

    public int getTerm() { return term; }
    public void setTerm(int term) { this.term = term; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getOutcomes() { return outcomes; }
    public void setOutcomes(String outcomes) { this.outcomes = outcomes; }

    public String getAssessmentStandards() { return assessmentStandards; }
    public void setAssessmentStandards(String assessmentStandards) { this.assessmentStandards = assessmentStandards; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}