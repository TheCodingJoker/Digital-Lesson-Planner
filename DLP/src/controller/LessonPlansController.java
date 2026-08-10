
package controller;

import dao.LessonPlanDAO;
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

    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private TableView<LessonPlan> lessonPlansTable;
    @FXML private TableColumn<LessonPlan, String> titleColumn;
    @FXML private TableColumn<LessonPlan, String> subjectColumn;
    @FXML private TableColumn<LessonPlan, String> gradeColumn;
    @FXML private TableColumn<LessonPlan, String> termColumn;
    @FXML private TableColumn<LessonPlan, String> dateColumn;
    @FXML private TableColumn<LessonPlan, String> statusColumn;
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
        dateColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(formatDate(data.getValue().getLessonDate())));
        statusColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(displayStatus(data.getValue().getStatus())));

        actionsColumn.setCellFactory(buildActionsCellFactory());

        filterStatusCombo.setItems(FXCollections.observableArrayList("All", "Scheduled", "Completed", "Extended"));
        filterStatusCombo.getSelectionModel().selectFirst();
        filterStatusCombo.valueProperty().addListener((obs, oldVal, newVal) -> loadLessonPlans());

        lessonPlansTable.setPlaceholder(new Label("No lesson plans yet. Click \"+ Create Lesson Plan\" to add one."));

        loadLessonPlans();
    }

    private void loadLessonPlans() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        List<LessonPlan> plans = lessonPlanDAO.findByTeacher(currentUser.getUserId());
        String filter = filterStatusCombo.getValue();

        if (filter != null && !"All".equals(filter)) {
            String statusFilter = toStoredStatus(filter);
            plans = plans.stream().filter(p -> statusFilter.equals(p.getStatus())).toList();
        }

        ObservableList<LessonPlan> items = FXCollections.observableArrayList(plans);
        lessonPlansTable.setItems(items);
    }

    @FXML
    private void handleNewLessonPlan() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Optional<LessonPlan> result = DialogHelper.showLessonPlanDialog(
            lessonPlansTable.getScene().getWindow(), null, currentUser.getUserId());

        result.ifPresent(plan -> {
            lessonPlanDAO.createLessonPlan(plan);
            loadLessonPlans();
        });
    }

    private void handleEdit(LessonPlan plan) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Optional<LessonPlan> result = DialogHelper.showLessonPlanDialog(
            lessonPlansTable.getScene().getWindow(), plan, currentUser.getUserId());

        result.ifPresent(updatedPlan -> {
            lessonPlanDAO.updateLessonPlan(updatedPlan);
            loadLessonPlans();
        });
    }

    private void handleMarkStatus(LessonPlan plan, String newStatus) {
        lessonPlanDAO.updateStatus(plan.getLessonPlanId(), newStatus);
        loadLessonPlans();
    }

    private void handleDelete(LessonPlan plan) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Lesson Plan");
        confirm.setHeaderText("Delete \"" + plan.getTitle() + "\"?");
        confirm.setContentText("This can't be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            lessonPlanDAO.deleteLessonPlan(plan.getLessonPlanId());
            loadLessonPlans();
        }
    }

    private Callback<TableColumn<LessonPlan, Void>, TableCell<LessonPlan, Void>> buildActionsCellFactory() {
        return column -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button completeButton = new Button("Complete");
            private final Button extendButton = new Button("Extend");
            private final Button deleteButton = new Button("Delete");
            private final HBox container = new HBox(6, editButton, completeButton, extendButton, deleteButton);

            {
                editButton.getStyleClass().add("table-action-button");
                completeButton.getStyleClass().add("table-action-button");
                extendButton.getStyleClass().add("table-action-button");
                deleteButton.getStyleClass().add("table-action-button-danger");
                editButton.setOnAction(e -> handleEdit(getTableView().getItems().get(getIndex())));
                completeButton.setOnAction(e -> handleMarkStatus(getTableView().getItems().get(getIndex()), LessonPlan.STATUS_COMPLETED));
                extendButton.setOnAction(e -> handleMarkStatus(getTableView().getItems().get(getIndex()), LessonPlan.STATUS_EXTENDED));
                deleteButton.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
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
            default: return "Scheduled";
        }
    }

    private String toStoredStatus(String display) {
        switch (display) {
            case "Completed": return LessonPlan.STATUS_COMPLETED;
            case "Extended": return LessonPlan.STATUS_EXTENDED;
            default: return LessonPlan.STATUS_SCHEDULED;
        }
    }
}
