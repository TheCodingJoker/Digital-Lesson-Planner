package util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Tracks unsaved changes and provides warning dialogs.
 */
public class UnsavedChangesTracker {

    private static UnsavedChangesTracker instance;
    private boolean hasUnsavedChanges;
    private Runnable onSaveCallback;

    private UnsavedChangesTracker() {
        this.hasUnsavedChanges = false;
    }

    public static synchronized UnsavedChangesTracker getInstance() {
        if (instance == null) {
            instance = new UnsavedChangesTracker();
        }
        return instance;
    }

    /**
     * Marks that there are unsaved changes.
     */
    public void markUnsavedChanges() {
        this.hasUnsavedChanges = true;
    }

    /**
     * Marks that all changes have been saved.
     */
    public void markAsSaved() {
        this.hasUnsavedChanges = false;
    }

    /**
     * Checks if there are unsaved changes.
     */
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    /**
     * Sets a callback to be executed when user chooses to save.
     */
    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    /**
     * Shows a warning dialog if there are unsaved changes.
     * Returns true if the user wants to proceed (either saved or discarded),
     * false if the user wants to cancel.
     */
    public boolean confirmProceedIfUnsaved() {
        if (!hasUnsavedChanges) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes");
        alert.setContentText("Do you want to save your changes before proceeding?");

        ButtonType saveButton = new ButtonType("Save");
        ButtonType discardButton = new ButtonType("Don't Save");
        ButtonType cancelButton = new ButtonType("Cancel");

        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        alert.showAndWait().ifPresent(result -> {
            if (result == saveButton) {
                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }
                markAsSaved();
            } else if (result == discardButton) {
                markAsSaved(); // Consider it "saved" by discarding
            }
            // If cancel, we'll return false
        });

        // Return true if user chose save or discard, false if cancel
        return alert.getResult() != cancelButton;
    }
}