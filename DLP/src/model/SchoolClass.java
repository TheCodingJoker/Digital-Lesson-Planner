
package model;

public class SchoolClass {

    private String classId;
    private String teacherId;
    private String className;   // e.g. "Grade 7A"
    private String subject;
    private String gradeLevel;
    private int studentCount;
    private String notes;
    private String createdAt;

    public SchoolClass() {}

    public SchoolClass(String classId, String teacherId, String className, String subject,
                        String gradeLevel, int studentCount, String notes) {
        this.classId = classId;
        this.teacherId = teacherId;
        this.className = className;
        this.subject = subject;
        this.gradeLevel = gradeLevel;
        this.studentCount = studentCount;
        this.notes = notes;
    }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }

    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
