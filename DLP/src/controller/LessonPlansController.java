
package controller;

import dao.LessonPlanDAO;
import service.SchedulingEngine;
import model.LessonPlan;
import model.User;
import util.DialogHelper;
import util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class LessonPlansController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterSubjectCombo;
    @FXML private ComboBox<String> filterGradeCombo;
    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private DatePicker filterStartDate;
    @FXML private DatePicker filterEndDate;
    @FXML private TableView<LessonPlan> lessonPlansTable;
    @FXML private TableColumn<LessonPlan, String> titleColumn;
    @FXML private TableColumn<LessonPlan, String> subjectColumn;
    @FXML private TableColumn<LessonPlan, String> gradeColumn;
    @FXML private TableColumn<LessonPlan, String> termColumn;
    @FXML private TableColumn<LessonPlan, String> capsCodeColumn;
    @FXML private TableColumn<LessonPlan, String> dateColumn;
    @FXML private TableColumn<LessonPlan, String> statusColumn;
    @FXML private TableColumn<LessonPlan, String> reschedulingNoteColumn;
    @FXML private TableColumn<LessonPlan, Void> actionsColumn;

    private final LessonPlanDAO lessonPlanDAO = new LessonPlanDAO();
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getTitle()));
        subjectColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getSubject()));
        gradeColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getGradeLevel()));
        termColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getTerm() == null ? "" : data.getValue().getTerm()));
        capsCodeColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getCapsCode() == null ? "" : data.getValue().getCapsCode()));
        dateColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(formatDate(data.getValue().getLessonDate())));
        statusColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(displayStatus(data.getValue().getStatus())));
        reschedulingNoteColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getReschedulingNote() == null
                    ? "" : data.getValue().getReschedulingNote()));
        reschedulingNoteColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String note, boolean empty) {
                super.updateItem(note, empty);
                setText(empty ? null : note);
                setTooltip(empty || note == null || note.isEmpty() ? null : new Tooltip(note));
            }
        });

        actionsColumn.setCellFactory(buildActionsCellFactory());

        // Populate filter combos
        filterSubjectCombo.setItems(FXCollections.observableArrayList(
            "All Subjects", "Mathematics", "English Home Language", "Life Skills",
            "Natural Sciences", "Life Sciences", "Social Sciences",
            "Economic and Management Sciences", "Technology", "History",
            "Life Orientation", "Afrikaans First Additional Language"
        ));
        filterSubjectCombo.getSelectionModel().selectFirst();

        filterGradeCombo.setItems(FXCollections.observableArrayList(
            "All Grades", "Grade R", "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5",
            "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"
        ));
        filterGradeCombo.getSelectionModel().selectFirst();

        filterStatusCombo.setItems(FXCollections.observableArrayList(
            "All Status", "Scheduled", "Completed", "Extended", "Incomplete", "Rescheduled"));
        filterStatusCombo.getSelectionModel().selectFirst();

        // Enable real-time search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        lessonPlansTable.setPlaceholder(new Label("No lesson plans match your filters."));

        loadLessonPlans();
    }

    private void loadLessonPlans() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        List<LessonPlan> plans = lessonPlanDAO.findByTeacher(currentUser.getUserId());
        ObservableList<LessonPlan> items = FXCollections.observableArrayList(plans);
        lessonPlansTable.setItems(items);
    }

    @FXML
    private void handleApplyFilters() {
        applyFilters();
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        filterSubjectCombo.getSelectionModel().selectFirst();
        filterGradeCombo.getSelectionModel().selectFirst();
        filterStatusCombo.getSelectionModel().selectFirst();
        filterStartDate.setValue(null);
        filterEndDate.setValue(null);
        loadLessonPlans();
    }

    private void applyFilters() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        List<LessonPlan> plans = lessonPlanDAO.findByTeacher(currentUser.getUserId());

        // Search text filter
        String searchText = searchField.getText() == null
            ? "" : searchField.getText().trim().toLowerCase();
        if (!searchText.isEmpty()) {
            plans = plans.stream()
                .filter(p -> containsIgnoreCase(p.getTitle(), searchText)
                    || containsIgnoreCase(p.getTopic(), searchText)
                    || containsIgnoreCase(p.getCapsCode(), searchText))
                .toList();
        }

        // Subject filter
        String subjectFilter = filterSubjectCombo.getValue();
        if (subjectFilter != null && !subjectFilter.equals("All Subjects")) {
            plans = plans.stream()
                .filter(p -> subjectFilter.equals(p.getSubject()))
                .toList();
        }

        // Grade filter
        String gradeFilter = filterGradeCombo.getValue();
        if (gradeFilter != null && !gradeFilter.equals("All Grades")) {
            plans = plans.stream()
                .filter(p -> gradeFilter.equals(p.getGradeLevel()))
                .toList();
        }

        // Status filter
        String statusFilter = filterStatusCombo.getValue();
        if (statusFilter != null && !statusFilter.equals("All Status")) {
            String storedStatus = toStoredStatus(statusFilter);
            plans = plans.stream()
                .filter(p -> storedStatus.equals(p.getStatus()))
                .toList();
        }

        // Date range filter
        LocalDate startDate = filterStartDate.getValue();
        LocalDate endDate = filterEndDate.getValue();
        if (startDate != null || endDate != null) {
            plans = plans.stream()
                .filter(p -> {
                    if (p.getLessonDate() == null || p.getLessonDate().isEmpty()) return false;
                    try {
                        LocalDate lessonDate = LocalDate.parse(p.getLessonDate());
                        if (startDate != null && lessonDate.isBefore(startDate)) return false;
                        if (endDate != null && lessonDate.isAfter(endDate)) return false;
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();
        }

        ObservableList<LessonPlan> items = FXCollections.observableArrayList(plans);
        lessonPlansTable.setItems(items);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    @FXML
    private void handleNewLessonPlan() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Optional<LessonPlan> result = DialogHelper.showLessonPlanDialog(
            lessonPlansTable.getScene().getWindow(), null, currentUser.getUserId());

        result.ifPresent(plan -> {
            if (lessonPlanDAO.createLessonPlan(plan)) {
                loadLessonPlans();
            } else {
                showError("Failed to create the lesson plan.");
            }
        });
    }

    private void handleEdit(LessonPlan plan) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Optional<LessonPlan> result = DialogHelper.showLessonPlanDialog(
            lessonPlansTable.getScene().getWindow(), plan, currentUser.getUserId());

        result.ifPresent(updatedPlan -> {
            if (lessonPlanDAO.updateLessonPlan(updatedPlan)) {
                loadLessonPlans();
            } else {
                showError("Failed to save changes to the lesson plan.");
            }
        });
    }

    private void handleMarkStatus(LessonPlan plan, String newStatus) {
        if (SchedulingEngine.getInstance().handleLessonStatusChange(
                plan.getLessonPlanId(), newStatus)) {
            loadLessonPlans();
        } else {
            showError("The lesson status could not be saved or the remaining lessons "
                + "could not be fully rescheduled. Check the lesson dates and try again.");
        }
    }

    private void handleDelete(LessonPlan plan) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Lesson Plan");
        confirm.setHeaderText("Delete \"" + plan.getTitle() + "\"?");
        confirm.setContentText("This can't be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (lessonPlanDAO.deleteLessonPlan(plan.getLessonPlanId())) {
                loadLessonPlans();
            } else {
                showError("Failed to delete the lesson plan.");
            }
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lesson Plan Action Failed");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Callback<TableColumn<LessonPlan, Void>, TableCell<LessonPlan, Void>> buildActionsCellFactory() {
        return column -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button completeButton = new Button("Complete");
            private final Button extendButton = new Button("Extend");
            private final Button incompleteButton = new Button("Incomplete");
            private final Button deleteButton = new Button("Delete");
            private final HBox container = new HBox(
                6, editButton, completeButton, extendButton, incompleteButton, deleteButton);

            {
                editButton.getStyleClass().add("table-action-button");
                completeButton.getStyleClass().add("table-action-button");
                extendButton.getStyleClass().add("table-action-button");
                incompleteButton.getStyleClass().add("table-action-button");
                deleteButton.getStyleClass().add("table-action-button-danger");
                editButton.setOnAction(e -> handleEdit(getTableView().getItems().get(getIndex())));
                completeButton.setOnAction(e -> handleMarkStatus(getTableView().getItems().get(getIndex()), LessonPlan.STATUS_COMPLETED));
                extendButton.setOnAction(e -> handleMarkStatus(getTableView().getItems().get(getIndex()), LessonPlan.STATUS_EXTENDED));
                incompleteButton.setOnAction(e -> handleMarkStatus(getTableView().getItems().get(getIndex()), LessonPlan.STATUS_INCOMPLETE));
                deleteButton.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
                if (!empty) {
                    boolean completed = LessonPlan.STATUS_COMPLETED.equals(
                        getTableView().getItems().get(getIndex()).getStatus());
                    completeButton.setDisable(completed);
                    extendButton.setDisable(completed);
                    incompleteButton.setDisable(completed);
                }
            }
        };
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            return LocalDate.parse(isoDate).format(DISPLAY_DATE);
        } catch (Exception e) {
            return isoDate;
        }
    }

    private String displayStatus(String status) {
        if (status == null) return "Scheduled";
        switch (status) {
            case LessonPlan.STATUS_COMPLETED: return "Completed";
            case LessonPlan.STATUS_EXTENDED: return "Extended";
            case LessonPlan.STATUS_INCOMPLETE: return "Incomplete";
            case LessonPlan.STATUS_RESCHEDULED: return "Rescheduled";
            default: return "Scheduled";
        }
    }

    private String toStoredStatus(String display) {
        if (display == null || display.equals("All Status")) return null;
        switch (display) {
            case "Completed": return LessonPlan.STATUS_COMPLETED;
            case "Extended": return LessonPlan.STATUS_EXTENDED;
            case "Incomplete": return LessonPlan.STATUS_INCOMPLETE;
            case "Rescheduled": return LessonPlan.STATUS_RESCHEDULED;
            default: return LessonPlan.STATUS_SCHEDULED;
        }
    }
}
