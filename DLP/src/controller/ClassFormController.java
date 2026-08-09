
package controller;

import model.SchoolClass;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.UUID;

public class ClassFormController {

    @FXML private Label errorLabel;
    @FXML private TextField classNameField;
    @FXML private ComboBox<String> subjectCombo;
    @FXML private ComboBox<String> gradeCombo;
    @FXML private TextField studentCountField;
    @FXML private TextArea notesArea;

    private String editingClassId; // null when creating a new class

    @FXML
    public void initialize() {
        subjectCombo.setItems(FXCollections.observableArrayList(
            "Mathematics", "English Home Language", "Life Skills",
            "Natural Sciences", "Social Sciences", "Economic and Management Sciences",
            "Technology", "Life Orientation", "Afrikaans First Additional Language"
        ));

        gradeCombo.setItems(FXCollections.observableArrayList(
            "Grade R", "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5",
            "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"
        ));
    }

    public void populate(SchoolClass schoolClass) {
        this.editingClassId = schoolClass.getClassId();
        classNameField.setText(schoolClass.getClassName());
        subjectCombo.setValue(schoolClass.getSubject());
        gradeCombo.setValue(schoolClass.getGradeLevel());
        studentCountField.setText(String.valueOf(schoolClass.getStudentCount()));
        notesArea.setText(schoolClass.getNotes());
    }

    public boolean isValid() {
        errorLabel.setText("");

        if (classNameField.getText() == null || classNameField.getText().trim().isEmpty()) {
            errorLabel.setText("Please enter a class name.");
            return false;
        }
        if (subjectCombo.getValue() == null || subjectCombo.getValue().trim().isEmpty()) {
            errorLabel.setText("Please select or enter a subject.");
            return false;
        }
        if (gradeCombo.getValue() == null) {
            errorLabel.setText("Please select a grade.");
            return false;
        }
        if (studentCountField.getText() != null && !studentCountField.getText().trim().isEmpty()) {
            try {
                int count = Integer.parseInt(studentCountField.getText().trim());
                if (count < 0) {
                    errorLabel.setText("Number of students can't be negative.");
                    return false;
                }
            } catch (NumberFormatException e) {
                errorLabel.setText("Number of students must be a whole number.");
                return false;
            }
        }
        return true;
    }

    public SchoolClass toSchoolClass(String teacherId) {
        String id = editingClassId != null ? editingClassId : UUID.randomUUID().toString();
        int studentCount = 0;
        if (studentCountField.getText() != null && !studentCountField.getText().trim().isEmpty()) {
            studentCount = Integer.parseInt(studentCountField.getText().trim());
        }

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(id);
        schoolClass.setTeacherId(teacherId);
        schoolClass.setClassName(classNameField.getText().trim());
        schoolClass.setSubject(subjectCombo.getValue().trim());
        schoolClass.setGradeLevel(gradeCombo.getValue());
        schoolClass.setStudentCount(studentCount);
        String notes = notesArea.getText();
        schoolClass.setNotes(notes == null || notes.trim().isEmpty() ? null : notes.trim());
        return schoolClass;
    }
}
