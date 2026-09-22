package model;

public class CAPSSnapshot {
    private String snapshotId;
    private String capturedAt;
    private String originalData; // JSON string of CAPS entry data
    
    public CAPSSnapshot() {
    }
    
    public CAPSSnapshot(String snapshotId, String capturedAt, String originalData) {
        this.snapshotId = snapshotId;
        this.capturedAt = capturedAt;
        this.originalData = originalData;
    }
    
    // Getters and Setters
    public String getSnapshotId() {
        return snapshotId;
    }
    
    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }
    
    public String getCapturedAt() {
        return capturedAt;
    }
    
    public void setCapturedAt(String capturedAt) {
        this.capturedAt = capturedAt;
    }
    
    public String getOriginalData() {
        return originalData;
    }
    
    public void setOriginalData(String originalData) {
        this.originalData = originalData;
    }
}