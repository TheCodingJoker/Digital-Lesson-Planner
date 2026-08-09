
package controller;

import dao.ClassDAO;
import model.SchoolClass;
import model.User;
import util.DialogHelper;
import util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.util.List;
import java.util.Optional;

public class ClassesController {

    @FXML private TableView<SchoolClass> classesTable;
    @FXML private TableColumn<SchoolClass, String> classNameColumn;
    @FXML private TableColumn<SchoolClass, String> subjectColumn;
    @FXML private TableColumn<SchoolClass, String> gradeColumn;
    @FXML private TableColumn<SchoolClass, String> studentCountColumn;
    @FXML private TableColumn<SchoolClass, String> notesColumn;
    @FXML private TableColumn<SchoolClass, Void> actionsColumn;

    private final ClassDAO classDAO = new ClassDAO();

    @FXML
    public void initialize() {
        classNameColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getClassName()));
        subjectColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getSubject()));
        gradeColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getGradeLevel()));
        studentCountColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getStudentCount())));
        notesColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getNotes() == null ? "" : data.getValue().getNotes()));

        actionsColumn.setCellFactory(buildActionsCellFactory());
        classesTable.setPlaceholder(new Label("No classes yet. Click \"+ New Class\" to add one."));

        loadClasses();
    }

    private void loadClasses() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        List<SchoolClass> classes = classDAO.findByTeacher(currentUser.getUserId());
        ObservableList<SchoolClass> items = FXCollections.observableArrayList(classes);
        classesTable.setItems(items);
    }

    @FXML
    private void handleNewClass() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Optional<SchoolClass> result = DialogHelper.showClassDialog(
            classesTable.getScene().getWindow(), null, currentUser.getUserId());

        result.ifPresent(schoolClass -> {
            classDAO.createClass(schoolClass);
            loadClasses();
        });
    }

    private void handleEdit(SchoolClass schoolClass) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Optional<SchoolClass> result = DialogHelper.showClassDialog(
            classesTable.getScene().getWindow(), schoolClass, currentUser.getUserId());

        result.ifPresent(updatedClass -> {
            classDAO.updateClass(updatedClass);
            loadClasses();
        });
    }

    private void handleDelete(SchoolClass schoolClass) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Class");
        confirm.setHeaderText("Delete \"" + schoolClass.getClassName() + "\"?");
        confirm.setContentText("This can't be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            classDAO.deleteClass(schoolClass.getClassId());
            loadClasses();
        }
    }

    private Callback<TableColumn<SchoolClass, Void>, TableCell<SchoolClass, Void>> buildActionsCellFactory() {
        return column -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox container = new HBox(8, editButton, deleteButton);

            {
                editButton.getStyleClass().add("table-action-button");
                deleteButton.getStyleClass().add("table-action-button-danger");
                editButton.setOnAction(e -> handleEdit(getTableView().getItems().get(getIndex())));
                deleteButton.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        };
    }
}
