package model;

public class SchoolCalendar {

    private int academicYear;
    private int totalTeachingDays;
    private String startDate; // ISO format yyyy-MM-dd
    private String endDate;   // ISO format yyyy-MM-dd

    public SchoolCalendar() {}

    public SchoolCalendar(int academicYear, int totalTeachingDays, String startDate, String endDate) {
        this.academicYear = academicYear;
        this.totalTeachingDays = totalTeachingDays;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public int getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(int academicYear) {
        this.academicYear = academicYear;
    }

    public int getTotalTeachingDays() {
        return totalTeachingDays;
    }

    public void setTotalTeachingDays(int totalTeachingDays) {
        this.totalTeachingDays = totalTeachingDays;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}
