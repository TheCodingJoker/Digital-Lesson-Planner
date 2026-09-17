
package controller;

import model.LessonPlan;
import model.CAPSEntry;
import dao.CAPSEntryDAO;
import util.ErrorLogger;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class LessonPlanFormController {

    @FXML private Label errorLabel;
    @FXML private ComboBox<String> gradeCombo;
    @FXML private ComboBox<String> subjectCombo;
    @FXML private ComboBox<String> termCombo;
    @FXML private TextField durationField;
    @FXML private DatePicker lessonDatePicker;
    @FXML private TextField topicField;
    @FXML private TextArea objectivesArea;
    @FXML private TextArea teachingActivitiesArea;
    @FXML private TextArea assessmentArea;
    @FXML private TextField resourcesField;

    private String editingLessonPlanId; // null when creating a new plan
    private String editingStatus;       // preserved as-is when editing; new plans default to SCHEDULED
    
    private CAPSEntryDAO capsEntryDAO;
    private ErrorLogger errorLogger;
    private boolean autoPopulateEnabled = true;

    @FXML
    public void initialize() {
        capsEntryDAO = new CAPSEntryDAO();
        errorLogger = ErrorLogger.getInstance();
        
        subjectCombo.setItems(FXCollections.observableArrayList(
            "Mathematics", "English Home Language", "Life Skills",
            "Natural Sciences", "Life Sciences", "Social Sciences",
            "Economic and Management Sciences", "Technology", "History",
            "Life Orientation", "Afrikaans First Additional Language"
        ));

        gradeCombo.setItems(FXCollections.observableArrayList(
            "Grade R", "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5",
            "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"
        ));

        termCombo.setItems(FXCollections.observableArrayList("Term 1", "Term 2", "Term 3", "Term 4"));

        lessonDatePicker.setValue(LocalDate.now());
        
        // Add listeners for CAPS auto-populate
        setupCAPSAutoPopulate();
    }

    /** Pre-fills the form when editing an existing lesson plan. */
    public void populate(LessonPlan plan) {
        this.editingLessonPlanId = plan.getLessonPlanId();
        this.editingStatus = plan.getStatus();
        gradeCombo.setValue(plan.getGradeLevel());
        subjectCombo.setValue(plan.getSubject());
        termCombo.setValue(plan.getTerm());
        durationField.setText(String.valueOf(plan.getDurationMinutes() > 0 ? plan.getDurationMinutes() : 60));
        topicField.setText(plan.getTopic());
        objectivesArea.setText(plan.getObjectives());
        teachingActivitiesArea.setText(plan.getTeachingActivities());
        assessmentArea.setText(plan.getAssessmentMethod());
        resourcesField.setText(plan.getResources());

        if (plan.getLessonDate() != null && !plan.getLessonDate().isEmpty()) {
            try {
                lessonDatePicker.setValue(LocalDate.parse(plan.getLessonDate()));
            } catch (Exception ignored) {
                // keep default date if stored value can't be parsed
            }
        }
    }

    /** Validates required fields, showing an inline error message if something's missing. */
    public boolean isValid() {
        errorLabel.setText("");

        if (gradeCombo.getValue() == null) {
            errorLabel.setText("Please select a grade.");
            return false;
        }
        if (subjectCombo.getValue() == null || subjectCombo.getValue().trim().isEmpty()) {
            errorLabel.setText("Please select or enter a subject.");
            return false;
        }
        if (termCombo.getValue() == null) {
            errorLabel.setText("Please select a term.");
            return false;
        }
        if (topicField.getText() == null || topicField.getText().trim().isEmpty()) {
            errorLabel.setText("Please enter a topic.");
            return false;
        }
        if (lessonDatePicker.getValue() == null) {
            errorLabel.setText("Please choose a lesson date.");
            return false;
        }
        if (durationField.getText() != null && !durationField.getText().trim().isEmpty()) {
            try {
                int duration = Integer.parseInt(durationField.getText().trim());
                if (duration <= 0) {
                    errorLabel.setText("Duration must be a positive number of minutes.");
                    return false;
                }
            } catch (NumberFormatException e) {
                errorLabel.setText("Duration must be a whole number of minutes.");
                return false;
            }
        }
        return true;
    }

    /** Builds a LessonPlan from the current field values. Call isValid() first. */
    public LessonPlan toLessonPlan(String teacherId) {
        String id = editingLessonPlanId != null ? editingLessonPlanId : UUID.randomUUID().toString();
        String topic = topicField.getText().trim();

        int duration = 60;
        if (durationField.getText() != null && !durationField.getText().trim().isEmpty()) {
            duration = Integer.parseInt(durationField.getText().trim());
        }

        LessonPlan plan = new LessonPlan();
        plan.setLessonPlanId(id);
        plan.setTeacherId(teacherId);
        plan.setTitle(topic);
        plan.setSubject(subjectCombo.getValue().trim());
        plan.setGradeLevel(gradeCombo.getValue());
        plan.setTerm(termCombo.getValue());
        plan.setTopic(topic);
        plan.setDurationMinutes(duration);
        plan.setObjectives(emptyToNull(objectivesArea.getText()));
        plan.setTeachingActivities(emptyToNull(teachingActivitiesArea.getText()));
        plan.setAssessmentMethod(emptyToNull(assessmentArea.getText()));
        plan.setResources(emptyToNull(resourcesField.getText()));
        plan.setLessonDate(lessonDatePicker.getValue().toString());
        plan.setStatus(editingStatus != null ? editingStatus : LessonPlan.STATUS_SCHEDULED);
        return plan;
    }

    private String emptyToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
    
    private void setupCAPSAutoPopulate() {
        // Add listeners to trigger CAPS auto-populate
        gradeCombo.setOnAction(event -> autoPopulateCAPS());
        subjectCombo.setOnAction(event -> autoPopulateCAPS());
        termCombo.setOnAction(event -> autoPopulateCAPS());
    }
    
    private void autoPopulateCAPS() {
        if (!autoPopulateEnabled) {
            return;
        }
        
        String grade = gradeCombo.getValue();
        String subject = subjectCombo.getValue();
        String term = termCombo.getValue();
        
        if (grade == null || subject == null || term == null) {
            return;
        }
        
        // Convert term string to integer
        int termNumber = 1;
        if (term.equals("Term 1")) termNumber = 1;
        else if (term.equals("Term 2")) termNumber = 2;
        else if (term.equals("Term 3")) termNumber = 3;
        else if (term.equals("Term 4")) termNumber = 4;
        
        try {
            // Search for CAPS entry matching the criteria
            List<CAPSEntry> capsEntries = capsEntryDAO.searchCAPSEntries(grade, subject, termNumber);
            
            if (!capsEntries.isEmpty()) {
                // Use the first matching CAPS entry
                CAPSEntry entry = capsEntries.get(0);
                
                // Auto-populate objectives and assessment
                if (entry.getOutcomes() != null && !entry.getOutcomes().isEmpty()) {
                    objectivesArea.setText(entry.getOutcomes());
                }
                
                if (entry.getAssessmentStandards() != null && !entry.getAssessmentStandards().isEmpty()) {
                    assessmentArea.setText(entry.getAssessmentStandards());
                }
                
                errorLogger.logInfo("LessonPlanFormController", "autoPopulateCAPS", 
                    "Auto-populated CAPS data for " + grade + " " + subject + " " + term);
            }
        } catch (Exception e) {
            errorLogger.logError("LessonPlanFormController", "autoPopulateCAPS", 
                "Failed to auto-populate CAPS data", e);
        }
    }
    
    public void setAutoPopulateEnabled(boolean enabled) {
        this.autoPopulateEnabled = enabled;
    }
}
