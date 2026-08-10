
package util;

import controller.ClassFormController;
import controller.LessonPlanFormController;
import model.LessonPlan;
import model.SchoolClass;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Builds custom modal "cards" (navy header + white scrollable body + footer) that match
 * the DLP design system, using a plain Stage rather than javafx.scene.control.Dialog.
 *
 * NOTE: an earlier version of this class used Dialog + StageStyle.TRANSPARENT, which has a
 * known reliability problem on some JDK/Windows combinations where the dialog fails to
 * actually close - and since these are APPLICATION_MODAL, a stuck dialog freezes the whole
 * app (including things like the logout button). A plain Stage does not have this problem.
 */
public class DialogHelper {

    private static final String THEME_CSS =
        DialogHelper.class.getResource("/view/styles/dlp-theme.css").toExternalForm();

    public static Optional<LessonPlan> showLessonPlanDialog(Window owner, LessonPlan existingPlan, String teacherId) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/view/fragments/LessonPlanFormView.fxml"));
            Parent content = loader.load();
            LessonPlanFormController formController = loader.getController();
            if (existingPlan != null) {
                formController.populate(existingPlan);
            }

            String primaryLabel = existingPlan == null ? "Create Lesson Plan" : "Save Changes";
            ModalChrome chrome = buildChrome("\uD83D\uDCD6",
                existingPlan == null ? "Create Lesson Plan" : "Edit Lesson Plan",
                "CAPS-Aligned Planning", content, primaryLabel);

            Stage stage = buildModalStage(owner, chrome.root);
            LessonPlan[] resultHolder = new LessonPlan[1];

            chrome.cancelButton.setOnAction(e -> stage.close());
            chrome.closeButton.setOnAction(e -> stage.close());
            chrome.primaryButton.setOnAction(e -> {
                if (formController.isValid()) {
                    resultHolder[0] = formController.toLessonPlan(teacherId);
                    stage.close();
                }
            });

            stage.showAndWait();
            return Optional.ofNullable(resultHolder[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static Optional<SchoolClass> showClassDialog(Window owner, SchoolClass existingClass, String teacherId) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource("/view/fragments/ClassFormView.fxml"));
            Parent content = loader.load();
            ClassFormController formController = loader.getController();
            if (existingClass != null) {
                formController.populate(existingClass);
            }

            String primaryLabel = existingClass == null ? "Add Class" : "Save Changes";
            ModalChrome chrome = buildChrome("\uD83C\uDFEB",
                existingClass == null ? "Add Class" : "Edit Class",
                "Manage the classes you teach", content, primaryLabel);

            Stage stage = buildModalStage(owner, chrome.root);
            SchoolClass[] resultHolder = new SchoolClass[1];

            chrome.cancelButton.setOnAction(e -> stage.close());
            chrome.closeButton.setOnAction(e -> stage.close());
            chrome.primaryButton.setOnAction(e -> {
                if (formController.isValid()) {
                    resultHolder[0] = formController.toSchoolClass(teacherId);
                    stage.close();
                }
            });

            stage.showAndWait();
            return Optional.ofNullable(resultHolder[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /** Opens a modal with arbitrary content and no footer buttons - just the close (X). Used for the Lesson Calendar. */
    public static void showInfoDialog(Window owner, String iconGlyph, String title, String subtitle, Node bodyContent,
                                       double width, double height) {
        ModalChrome chrome = buildChrome(iconGlyph, title, subtitle, bodyContent, null);
        chrome.root.setPrefWidth(width);
        chrome.root.setPrefHeight(height);

        Stage stage = buildModalStage(owner, chrome.root);
        chrome.closeButton.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    private static Stage buildModalStage(Window owner, VBox root) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(THEME_CSS);
        stage.setScene(scene);

        // Safety net: if the OS sends a close request (e.g. Alt+F4), make sure it actually
        // closes rather than silently doing nothing.
        stage.setOnCloseRequest(e -> stage.close());

        return stage;
    }

    private static class ModalChrome {
        VBox root;
        Button primaryButton;
        Button cancelButton;
        Button closeButton;
    }

    /** primaryLabel == null means "no footer" (info-style modal, e.g. the calendar). */
    private static ModalChrome buildChrome(String iconGlyph, String title, String subtitle,
                                            Node bodyContent, String primaryLabel) {
        ModalChrome chrome = new ModalChrome();

        Label iconLabel = new Label(iconGlyph);
        iconLabel.getStyleClass().add("modal-header-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("modal-header-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("modal-header-subtitle");
        VBox titleBox = new VBox(2, titleLabel, subtitleLabel);

        Button closeButton = new Button("\u2715");
        closeButton.getStyleClass().add("modal-close-button");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox header = new HBox(14, iconLabel, titleBox, headerSpacer, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("modal-header");
        header.setPadding(new Insets(20, 24, 20, 24));

        VBox paddedBody = new VBox(bodyContent);
        paddedBody.setPadding(new Insets(24));
        ScrollPane scrollPane = new ScrollPane(paddedBody);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("modal-body-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox root = new VBox(header, scrollPane);
        root.getStyleClass().add("modal-root");
        root.setPrefWidth(680);
        root.setPrefHeight(640);
        root.setMaxHeight(760);

        if (primaryLabel != null) {
            Button cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().add("modal-cancel-button");

            Button primaryButton = new Button("\u2713  " + primaryLabel);
            primaryButton.getStyleClass().add("modal-primary-button");
            primaryButton.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(primaryButton, Priority.ALWAYS);

            HBox footer = new HBox(12, primaryButton, cancelButton);
            footer.setAlignment(Pos.CENTER);
            footer.getStyleClass().add("modal-footer");
            footer.setPadding(new Insets(16, 24, 16, 24));

            root.getChildren().add(footer);
            chrome.primaryButton = primaryButton;
            chrome.cancelButton = cancelButton;
        }

        chrome.root = root;
        chrome.closeButton = closeButton;
        return chrome;
    }
}
