package util;

import dao.FeedbackDAO;
import dao.UserDAO;
import model.Feedback;
import model.User;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class FeedbackDialog {

    public static void showFeedbackDialog(User currentUser) {
        Dialog<Feedback> dialog = new Dialog<>();
        dialog.setTitle("Submit Feedback");
        dialog.setHeaderText("Report an issue or suggest an improvement");

        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType submitButtonType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Bug Report", "Suggestion", "Other");
        typeCombo.setValue("Bug Report");

        TextField titleField = new TextField();
        titleField.setPromptText("Brief title");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Please describe the issue or suggestion in detail...");
        descriptionArea.setPrefRowCount(5);

        ComboBox<String> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll("Low", "Medium", "High", "Critical");
        priorityCombo.setValue("Medium");

        grid.add(new Label("Type:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Title:"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(new Label("Priority:"), 0, 2);
        grid.add(priorityCombo, 1, 2);
        grid.add(new Label("Description:"), 0, 3);
        grid.add(descriptionArea, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Button submitButton = (Button) dialog.getDialogPane().lookupButton(submitButtonType);
        submitButton.setDisable(true);

        titleField.textProperty().addListener((obs, old, newVal) -> {
            submitButton.setDisable(newVal.trim().isEmpty() || descriptionArea.getText().trim().isEmpty());
        });

        descriptionArea.textProperty().addListener((obs, old, newVal) -> {
            submitButton.setDisable(newVal.trim().isEmpty() || titleField.getText().trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                Feedback feedback = new Feedback();
                feedback.setFeedbackId(UUID.randomUUID().toString());
                feedback.setUserId(currentUser.getUserId());
                feedback.setFeedbackType(typeCombo.getValue().toUpperCase().replace(" ", "_"));
                feedback.setTitle(titleField.getText());
                feedback.setDescription(descriptionArea.getText());
                feedback.setStatus("OPEN");
                feedback.setPriority(priorityCombo.getValue().toUpperCase());
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                feedback.setCreatedAt(now);
                feedback.setUpdatedAt(now);
                return feedback;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(feedback -> {
            FeedbackDAO feedbackDAO = new FeedbackDAO();
            if (feedbackDAO.createFeedback(feedback)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Feedback Submitted");
                alert.setHeaderText(null);
                alert.setContentText("Thank you for your feedback! It has been submitted successfully.");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Submission Failed");
                alert.setHeaderText(null);
                alert.setContentText("Failed to submit feedback. Please try again.");
                alert.showAndWait();
            }
        });
    }
}