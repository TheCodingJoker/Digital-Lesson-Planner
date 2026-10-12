
package controller;

import model.LessonPlan;
import model.CAPSEntry;
import dao.CAPSEntryDAO;
import dao.LessonPlanDAO;
import service.SchedulingEngine;
import util.ErrorLogger;
import util.SessionManager;
import util.UnsavedChangesTracker;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LessonPlanFormController {

    @FXML private Label errorLabel;
    @FXML private ComboBox<String> gradeCombo;
    @FXML private ComboBox<String> subjectCombo;
    @FXML private ComboBox<String> termCombo;
    @FXML private TextField durationField;
    @FXML private DatePicker lessonDatePicker;
    @FXML private ComboBox<String> topicCombo;
    @FXML private TextArea objectivesArea;
    @FXML private TextArea teachingActivitiesArea;
    @FXML private TextArea assessmentArea;
    @FXML private TextField resourcesField;

    private String editingLessonPlanId; // null when creating a new plan
    private String editingStatus;       // preserved as-is when editing; new plans default to SCHEDULED
    
    private CAPSEntryDAO capsEntryDAO;
    private ErrorLogger errorLogger;
    private SchedulingEngine schedulingEngine;
    private LessonPlanDAO lessonPlanDAO;
    private UnsavedChangesTracker unsavedChangesTracker;
    private boolean autoPopulateEnabled = true;
    private boolean populatingForm;
    private CAPSEntry selectedCapsEntry;
    private final Map<String, CAPSEntry> capsTopicOptions = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        capsEntryDAO = new CAPSEntryDAO();
        errorLogger = ErrorLogger.getInstance();
        lessonPlanDAO = new LessonPlanDAO();
        schedulingEngine = SchedulingEngine.getInstance();
        unsavedChangesTracker = UnsavedChangesTracker.getInstance();

        subjectCombo.setItems(FXCollections.observableArrayList(
            "Mathematics", "English Home Language", "English First Additional Language", "Life Skills",
            "Natural Sciences", "Life Sciences", "Social Sciences",
            "Economic and Management Sciences", "Technology", "History",
            "Life Orientation", "Afrikaans First Additional Language"
        ));

        gradeCombo.setItems(FXCollections.observableArrayList(
            "Grade R", "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5",
            "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"
        ));

        termCombo.setItems(FXCollections.observableArrayList("Term 1", "Term 2", "Term 3", "Term 4"));
        topicCombo.setEditable(true);
        topicCombo.setPromptText("Choose a CAPS topic or enter one");

        lessonDatePicker.setValue(LocalDate.now());

        // Add listeners for CAPS auto-populate
        setupCAPSAutoPopulate();
        setupActivityMonitoring();
        setupUnsavedChangesTracking();
    }

    /** Pre-fills the form when editing an existing lesson plan. */
    public void populate(LessonPlan plan) {
        this.editingLessonPlanId = plan.getLessonPlanId();
        this.editingStatus = plan.getStatus();
        populatingForm = true;
        gradeCombo.setValue(plan.getGradeLevel());
        subjectCombo.setValue(plan.getSubject());
        termCombo.setValue(plan.getTerm());
        durationField.setText(String.valueOf(plan.getDurationMinutes() > 0 ? plan.getDurationMinutes() : 60));
        objectivesArea.setText(plan.getObjectives());
        teachingActivitiesArea.setText(plan.getTeachingActivities());
        assessmentArea.setText(plan.getAssessmentMethod());
        resourcesField.setText(plan.getResources());
        populatingForm = false;
        refreshCAPSTopics();
        populatingForm = true;
        selectedCapsEntry = plan.getCapsCode() == null ? null : capsEntryDAO.findByCapsCode(plan.getCapsCode());
        if (selectedCapsEntry != null && capsTopicOptions.values().stream()
                .anyMatch(option -> option.getCapsCode().equals(selectedCapsEntry.getCapsCode()))) {
            String option = capsTopicOptions.entrySet().stream()
                .filter(item -> item.getValue().getCapsCode().equals(selectedCapsEntry.getCapsCode()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
            topicCombo.setValue(option);
            topicCombo.getEditor().setText(option);
        } else {
            selectedCapsEntry = null;
            topicCombo.setValue(plan.getTopic());
            topicCombo.getEditor().setText(plan.getTopic());
        }
        populatingForm = false;

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
        if (getTopicText().isEmpty()) {
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
        String topic = selectedCapsEntry == null
            ? getTopicText()
            : selectedCapsEntry.getTopic();

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
        plan.setCapsCode(selectedCapsEntry == null ? null : selectedCapsEntry.getCapsCode());
        plan.setSelectedCapsEntry(selectedCapsEntry);
        plan.setCapsEntrySelectionChanged(true);
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
        gradeCombo.valueProperty().addListener((obs, oldValue, newValue) -> handleCAPSFilterChange());
        subjectCombo.valueProperty().addListener((obs, oldValue, newValue) -> handleCAPSFilterChange());
        termCombo.valueProperty().addListener((obs, oldValue, newValue) -> handleCAPSFilterChange());
        topicCombo.setOnAction(event -> applySelectedCAPSTopic());
        topicCombo.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (!populatingForm && selectedCapsEntry != null
                    && !newValue.equals(topicOption(selectedCapsEntry))) {
                selectedCapsEntry = null;
            }
        });
    }

    private void handleCAPSFilterChange() {
        if (!populatingForm) {
            objectivesArea.clear();
            assessmentArea.clear();
        }
        refreshCAPSTopics();
    }

    private void refreshCAPSTopics() {
        if (!autoPopulateEnabled || populatingForm) {
            return;
        }

        String grade = gradeCombo.getValue();
        String subject = subjectCombo.getValue();
        String term = termCombo.getValue();

        selectedCapsEntry = null;
        capsTopicOptions.clear();
        topicCombo.getItems().clear();
        topicCombo.setValue(null);
        topicCombo.getEditor().clear();

        if (grade == null || subject == null || term == null) {
            topicCombo.setPromptText("Choose grade, subject, and term first");
            return;
        }

        try {
            int termNumber = Integer.parseInt(term.substring("Term ".length()));
            List<CAPSEntry> entries = capsEntryDAO.searchCAPSEntries(grade, subject.trim(), termNumber);
            for (CAPSEntry entry : entries) {
                String option = topicOption(entry);
                capsTopicOptions.put(option, entry);
                topicCombo.getItems().add(option);
            }
            topicCombo.setPromptText(entries.isEmpty()
                ? "No CAPS topics for this selection; enter a topic"
                : "Select a CAPS topic");
        } catch (NumberFormatException e) {
            errorLogger.logError("LessonPlanFormController", "refreshCAPSTopics", "Invalid term selection", e);
        } catch (Exception e) {
            errorLogger.logError("LessonPlanFormController", "refreshCAPSTopics", "Failed to load CAPS topics", e);
        }
    }

    private String topicOption(CAPSEntry entry) {
        return entry.getTopic() + " [" + entry.getCapsCode() + "]";
    }

    private void applySelectedCAPSTopic() {
        if (populatingForm) {
            return;
        }
        CAPSEntry entry = capsTopicOptions.get(topicCombo.getValue());
        if (entry == null) {
            selectedCapsEntry = null;
            return;
        }

        selectedCapsEntry = entry;
        populatingForm = true;
        topicCombo.getEditor().setText(topicOption(entry));
        if (entry.getOutcomes() != null) {
            objectivesArea.setText(entry.getOutcomes());
        }
        if (entry.getAssessmentStandards() != null) {
            assessmentArea.setText(entry.getAssessmentStandards());
        }
        populatingForm = false;
        errorLogger.logInfo("LessonPlanFormController", "applySelectedCAPSTopic",
            "Applied CAPS entry " + entry.getCapsCode());
    }

    private String getTopicText() {
        String text = topicCombo.isEditable()
            ? topicCombo.getEditor().getText()
            : topicCombo.getValue();
        return text == null ? "" : text.trim();
    }

    public void setAutoPopulateEnabled(boolean enabled) {
        this.autoPopulateEnabled = enabled;
    }

    private void setupActivityMonitoring() {
        // Monitor key presses and mouse clicks on form fields
        gradeCombo.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_CLICKED,
                    e -> SessionManager.getInstance().resetInactivityTimer());
                newScene.addEventFilter(KeyEvent.KEY_TYPED,
                    e -> SessionManager.getInstance().resetInactivityTimer());
            }
        });
    }

    private void setupUnsavedChangesTracking() {
        // Mark as unsaved when any field changes
        gradeCombo.setOnAction(e -> unsavedChangesTracker.markUnsavedChanges());
        subjectCombo.setOnAction(e -> unsavedChangesTracker.markUnsavedChanges());
        termCombo.setOnAction(e -> unsavedChangesTracker.markUnsavedChanges());
        durationField.textProperty().addListener((obs, old, newVal) -> unsavedChangesTracker.markUnsavedChanges());
        lessonDatePicker.setOnAction(e -> unsavedChangesTracker.markUnsavedChanges());
        topicCombo.valueProperty().addListener((obs, old, newVal) -> unsavedChangesTracker.markUnsavedChanges());
        topicCombo.getEditor().textProperty().addListener((obs, old, newVal) -> unsavedChangesTracker.markUnsavedChanges());
        objectivesArea.textProperty().addListener((obs, old, newVal) -> unsavedChangesTracker.markUnsavedChanges());
        teachingActivitiesArea.textProperty().addListener((obs, old, newVal) -> unsavedChangesTracker.markUnsavedChanges());
        assessmentArea.textProperty().addListener((obs, old, newVal) -> unsavedChangesTracker.markUnsavedChanges());
        resourcesField.textProperty().addListener((obs, old, newVal) -> unsavedChangesTracker.markUnsavedChanges());

        // Set save callback
        unsavedChangesTracker.setOnSaveCallback(() -> {
            // Trigger save logic
            saveLessonPlan();
        });
    }

    private void saveLessonPlan() {
        if (!isValid()) {
            return;
        }

        try {
            String teacherId = SessionManager.getInstance().getCurrentUser().getUserId();
            LessonPlan plan = toLessonPlan(teacherId);

            if (editingLessonPlanId == null) {
                lessonPlanDAO.createLessonPlan(plan);
            } else {
                lessonPlanDAO.updateLessonPlan(plan);
            }

            unsavedChangesTracker.markAsSaved();
            errorLabel.setText("Saved successfully!");
            errorLabel.setStyle("-fx-text-fill: green;");
        } catch (Exception e) {
            errorLabel.setText("Failed to save: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red;");
        }
    }
}
