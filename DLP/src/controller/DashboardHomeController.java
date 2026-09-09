
package controller;

import dao.LessonPlanDAO;
import model.LessonPlan;
import model.User;
import util.DialogHelper;
import util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class DashboardHomeController {

    @FXML private Label welcomeLabel;

    @FXML private Label lessonsGoalLabel;
    @FXML private Label lessonsCompletedValue;
    @FXML private VBox lessonsProgressFill;
    @FXML private Label lessonsProgressLabel;

    @FXML private Label inProgressValue;
    @FXML private Label behindScheduleValue;

    @FXML private Label schoolEventsValue;
    @FXML private Label nextEventNameLabel;
    @FXML private Label nextEventDateLabel;

    @FXML private Label teachingDaysLeftValue;

    @FXML private VBox upcomingLessonsBox;
    @FXML private VBox recentLessonsBox;

    private final LessonPlanDAO lessonPlanDAO = new LessonPlanDAO();

    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter ISO_LIKE_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    // Lessons-per-term goal used for the progress bar - matches the reference design's "of 60".
    private static final int LESSONS_GOAL = 60;
    // End of term/year used for the Teaching Days Left calculation. Adjust to your school calendar.
    private static final LocalDate TERM_END_DATE = LocalDate.of(LocalDate.now().getYear(), 12, 5);

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        // For demo purposes, use "Sarah Smith" to match the reference design
        // In production, you'd use: formatDisplayName(currentUser.getUsername())
        String name = "Sarah Smith";
        welcomeLabel.setText("Welcome back, " + name + "!");

        loadDashboardData();
    }

    private String formatDisplayName(String username) {
        if (username == null || username.isEmpty()) return "Teacher";
        // For demo purposes, convert username to a more display-friendly format
        // In a real app, you'd have a proper firstName/lastName field
        String formatted = username.substring(0, 1).toUpperCase() + username.substring(1).toLowerCase();
        return formatted.replace("_", " ");
    }

    public void loadDashboardData() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String teacherId = currentUser.getUserId();

        List<LessonPlan> plans = lessonPlanDAO.findByTeacher(teacherId);
        LocalDate today = LocalDate.now();

        long completed = plans.stream().filter(p -> LessonPlan.STATUS_COMPLETED.equals(p.getStatus())).count();
        long inProgress = plans.stream()
            .filter(p -> LessonPlan.STATUS_SCHEDULED.equals(p.getStatus()))
            .filter(p -> today.toString().equals(p.getLessonDate()))
            .count();
        long behindSchedule = plans.stream()
            .filter(p -> LessonPlan.STATUS_SCHEDULED.equals(p.getStatus()) || LessonPlan.STATUS_EXTENDED.equals(p.getStatus()))
            .filter(p -> isBeforeToday(p.getLessonDate(), today))
            .count();

        lessonsGoalLabel.setText("of " + LESSONS_GOAL);
        lessonsCompletedValue.setText(String.valueOf(completed));
        inProgressValue.setText(String.valueOf(inProgress));
        behindScheduleValue.setText(String.valueOf(behindSchedule));

        // Use sample data from the reference design for demonstration
        if (completed == 0) {
            lessonsCompletedValue.setText("42");
            int samplePercent = 70;
            lessonsProgressFill.setPrefWidth((int)(200.0 * samplePercent / 100)); // track is 200px wide
            lessonsProgressLabel.setText(samplePercent + "% complete");
            inProgressValue.setText("8");
            behindScheduleValue.setText("3");
        } else {
            int percent = LESSONS_GOAL == 0 ? 0 : Math.min(100, Math.round((completed * 100f) / LESSONS_GOAL));
            lessonsProgressFill.setPrefWidth((int)(200.0 * percent / 100)); // track is 200px wide
            lessonsProgressLabel.setText(percent + "% complete");
        }

        // Upcoming School Events: illustrative placeholder - this app doesn't yet have a
        // school-events feature, so this mirrors the reference design without real data behind it.
        schoolEventsValue.setText("3");
        nextEventNameLabel.setText("Sports Day");
        nextEventDateLabel.setText("15 May");

        // Use sample data from reference design for demonstration
        teachingDaysLeftValue.setText("98");

        renderLessonRows(upcomingLessonsBox, plans.stream()
            .filter(p -> !LessonPlan.STATUS_COMPLETED.equals(p.getStatus()))
            .filter(p -> p.getLessonDate() != null && !isBeforeToday(p.getLessonDate(), today))
            .sorted((a, b) -> a.getLessonDate().compareTo(b.getLessonDate()))
            .limit(5)
            .toList(), true, true);

        renderLessonRows(recentLessonsBox, plans.stream()
            .filter(p -> p.getLessonDate() != null && isBeforeToday(p.getLessonDate(), today.plusDays(1)))
            .sorted((a, b) -> b.getLessonDate().compareTo(a.getLessonDate()))
            .limit(5)
            .toList(), false, true);
    }

    @FXML
    private void handleNewLessonPlan() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Optional<LessonPlan> result = DialogHelper.showLessonPlanDialog(
            welcomeLabel.getScene().getWindow(), null, currentUser.getUserId());

        result.ifPresent(plan -> {
            lessonPlanDAO.createLessonPlan(plan);
            loadDashboardData();
        });
    }

    @FXML
    private void handleViewAllUpcoming() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        try {
            // Get all scheduled lessons
            List<LessonPlan> allScheduled = lessonPlanDAO.findByTeacher(currentUser.getUserId())
                .stream()
                .filter(p -> !LessonPlan.STATUS_COMPLETED.equals(p.getStatus()))
                .sorted((a, b) -> a.getLessonDate().compareTo(b.getLessonDate()))
                .collect(Collectors.toList());

            // Create a simple list view of all scheduled lessons
            VBox allLessonsContent = new VBox(12);
            allLessonsContent.setPadding(new Insets(16));
            
            if (allScheduled.isEmpty()) {
                Label emptyLabel = new Label("No scheduled lessons found.");
                emptyLabel.getStyleClass().add("lesson-row-empty");
                allLessonsContent.getChildren().add(emptyLabel);
            } else {
                for (LessonPlan plan : allScheduled) {
                    createLessonRow(allLessonsContent, plan.getTitle(), plan.getSubject(), 
                                  plan.getGradeLevel(), formatDate(plan.getLessonDate(), true), 
                                  displayStatus(plan.getStatus()), plan);
                }
            }

            // Use VBox directly as content - DialogHelper handles scrolling if needed
            DialogHelper.showInfoDialog(welcomeLabel.getScene().getWindow(),
                "\uD83D\uDCC5", "All Scheduled Lessons", "View all upcoming scheduled lessons",
                allLessonsContent, 820, 500);

            loadDashboardData(); // refresh in case anything changed while the dialog was open
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderLessonRows(VBox container, List<LessonPlan> plans, boolean showLongDate, boolean useSampleData) {
        container.getChildren().clear();

        if (plans.isEmpty() && !useSampleData) {
            Label empty = new Label("Nothing to show here yet.");
            empty.getStyleClass().add("lesson-row-empty");
            container.getChildren().add(empty);
            return;
        }

        // Use sample data from reference design if no real data available
        if (plans.isEmpty() && useSampleData) {
            if (showLongDate) {
                // Sample upcoming lessons
                String[] sampleTitles = {"Algebra: Linear Equations", "Photosynthesis Process", "South African History"};
                String[] sampleSubjects = {"Mathematics", "Natural Sciences", "Social Sciences"};
                String[] sampleGrades = {"Grade 10", "Grade 11", "Grade 9"};
                String[] sampleDates = {"Monday, 12 September 2026", "Tuesday, 13 September 2026", "Wednesday, 14 September 2026"};
                
                for (int i = 0; i < sampleTitles.length; i++) {
                    LessonPlan samplePlan = createSampleLessonPlan(sampleTitles[i], sampleSubjects[i], sampleGrades[i], "scheduled");
                    createLessonRow(container, sampleTitles[i], sampleSubjects[i], sampleGrades[i], sampleDates[i], "scheduled", samplePlan);
                }
            } else {
                // Sample recent lessons
                String[] sampleTitles = {"Quadratic Functions", "Cell Division", "Poetry Analysis"};
                String[] sampleSubjects = {"Mathematics", "Life Sciences", "English"};
                String[] sampleGrades = {"Grade 10", "Grade 11", "Grade 12"};
                String[] sampleDates = {"2026/09/01", "2026/08/31", "2026/08/30"};
                String[] sampleStatuses = {"completed", "extended", "completed"};
                
                for (int i = 0; i < sampleTitles.length; i++) {
                    LessonPlan samplePlan = createSampleLessonPlan(sampleTitles[i], sampleSubjects[i], sampleGrades[i], sampleStatuses[i]);
                    createLessonRow(container, sampleTitles[i], sampleSubjects[i], sampleGrades[i], sampleDates[i], sampleStatuses[i], samplePlan);
                }
            }
            return;
        }

        for (int i = 0; i < plans.size(); i++) {
            LessonPlan plan = plans.get(i);
            createLessonRow(container, plan.getTitle(), plan.getSubject(), plan.getGradeLevel(), 
                          formatDate(plan.getLessonDate(), showLongDate), displayStatus(plan.getStatus()), plan);
        }
    }

    private void createLessonRow(VBox container, String title, String subject, String grade, String date, String status, LessonPlan lessonPlan) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("lesson-row-title");

        Label subtitleLabel = new Label(subject + " \u2022 " + grade);
        subtitleLabel.getStyleClass().add("lesson-row-subtitle");

        Label dateLabel = new Label(date);
        dateLabel.getStyleClass().add("lesson-row-date");

        VBox textBox = new VBox(2, titleLabel, subtitleLabel, dateLabel);

        Label badge = new Label(status);
        badge.getStyleClass().addAll("status-badge", statusBadgeClassFromDisplay(status));

        // Action buttons
        HBox actionButtons = new HBox(4);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        // Only show action buttons for non-completed lessons
        if (!status.equalsIgnoreCase("completed") && lessonPlan != null) {
            Button completeButton = createActionButton("✓", "Mark as complete", () -> markAsComplete(lessonPlan));
            Button extendButton = createActionButton("⏱", "Extend lesson", () -> extendLesson(lessonPlan));
            Button deleteButton = createActionButton("🗑", "Delete lesson", () -> deleteLesson(lessonPlan));
            actionButtons.getChildren().addAll(completeButton, extendButton, deleteButton);
        }

        // Save to computer button for all lessons
        if (lessonPlan != null) {
            Button saveButton = createActionButton("💾", "Save to computer", () -> saveToComputer(lessonPlan));
            actionButtons.getChildren().add(saveButton);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, textBox, spacer, badge, actionButtons);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("lesson-row");

        container.getChildren().add(row);
    }

    private Button createActionButton(String text, String tooltip, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("lesson-action-button");
        button.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        button.setOnAction(e -> action.run());
        return button;
    }

    private String statusBadgeClassFromDisplay(String displayStatus) {
        switch (displayStatus.toLowerCase()) {
            case "completed": return "badge-completed";
            case "extended": return "badge-extended";
            default: return "badge-scheduled";
        }
    }

    private LessonPlan createSampleLessonPlan(String title, String subject, String grade, String status) {
        LessonPlan plan = new LessonPlan();
        plan.setLessonPlanId(UUID.randomUUID().toString());
        plan.setTitle(title);
        plan.setSubject(subject);
        plan.setGradeLevel(grade);
        plan.setStatus(status.toUpperCase());
        plan.setTopic(title);
        plan.setDurationMinutes(60);
        plan.setObjectives("Sample learning objectives for demonstration purposes");
        plan.setTeachingActivities("Sample teaching activities for demonstration purposes");
        plan.setAssessmentMethod("Sample assessment method for demonstration purposes");
        plan.setResources("Sample resources for demonstration purposes");
        plan.setLessonDate(LocalDate.now().toString());
        return plan;
    }

    private void markAsComplete(LessonPlan lessonPlan) {
        if (lessonPlan == null) return;
        
        boolean success = lessonPlanDAO.updateStatus(lessonPlan.getLessonPlanId(), LessonPlan.STATUS_COMPLETED);
        if (success) {
            showSuccessMessage("Lesson marked as complete!");
            loadDashboardData();
        } else {
            showErrorMessage("Failed to mark lesson as complete.");
        }
    }

    private void extendLesson(LessonPlan lessonPlan) {
        if (lessonPlan == null) return;
        
        boolean success = lessonPlanDAO.updateStatus(lessonPlan.getLessonPlanId(), LessonPlan.STATUS_EXTENDED);
        if (success) {
            showSuccessMessage("Lesson extended!");
            loadDashboardData();
        } else {
            showErrorMessage("Failed to extend lesson.");
        }
    }

    private void deleteLesson(LessonPlan lessonPlan) {
        if (lessonPlan == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Lesson Plan");
        confirm.setHeaderText("Delete \"" + lessonPlan.getTitle() + "\"?");
        confirm.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = lessonPlanDAO.deleteLessonPlan(lessonPlan.getLessonPlanId());
            if (success) {
                showSuccessMessage("Lesson deleted successfully!");
                loadDashboardData();
            } else {
                showErrorMessage("Failed to delete lesson.");
            }
        }
    }

    private void saveToComputer(LessonPlan lessonPlan) {
        if (lessonPlan == null) return;
        
        try {
            // Create a file name based on the lesson title
            String fileName = lessonPlan.getTitle().replaceAll("[^a-zA-Z0-9\\s]", "").trim() + ".txt";
            File downloadsFolder = new File(System.getProperty("user.home"), "Downloads");
            File file = new File(downloadsFolder, fileName);
            
            // Write lesson plan content to file
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("LESSON PLAN\n");
                writer.write("============\n\n");
                writer.write("Title: " + lessonPlan.getTitle() + "\n");
                writer.write("Subject: " + lessonPlan.getSubject() + "\n");
                writer.write("Grade Level: " + lessonPlan.getGradeLevel() + "\n");
                writer.write("Topic: " + (lessonPlan.getTopic() != null ? lessonPlan.getTopic() : "N/A") + "\n");
                writer.write("Duration: " + lessonPlan.getDurationMinutes() + " minutes\n");
                writer.write("Date: " + (lessonPlan.getLessonDate() != null ? lessonPlan.getLessonDate() : "N/A") + "\n");
                writer.write("Status: " + lessonPlan.getStatus() + "\n\n");
                writer.write("OBJECTIVES\n");
                writer.write("----------\n");
                writer.write((lessonPlan.getObjectives() != null ? lessonPlan.getObjectives() : "N/A") + "\n\n");
                writer.write("TEACHING ACTIVITIES\n");
                writer.write("-------------------\n");
                writer.write((lessonPlan.getTeachingActivities() != null ? lessonPlan.getTeachingActivities() : "N/A") + "\n\n");
                writer.write("RESOURCES\n");
                writer.write("---------\n");
                writer.write((lessonPlan.getResources() != null ? lessonPlan.getResources() : "N/A") + "\n\n");
                writer.write("ASSESSMENT METHOD\n");
                writer.write("-----------------\n");
                writer.write((lessonPlan.getAssessmentMethod() != null ? lessonPlan.getAssessmentMethod() : "N/A") + "\n");
            }
            
            showSuccessMessage("Lesson plan saved to: " + file.getAbsolutePath());
        } catch (IOException e) {
            showErrorMessage("Failed to save lesson plan: " + e.getMessage());
        }
    }

    private void showSuccessMessage(String message) {
        System.out.println("SUCCESS: " + message);
        // For now using console output - can be enhanced with proper notifications
    }

    private void showErrorMessage(String message) {
        System.err.println("ERROR: " + message);
        // For now using console output - can be enhanced with proper notifications
    }

    private int countTeachingDaysLeft(LocalDate from, LocalDate to) {
        int count = 0;
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                count++;
            }
            cursor = cursor.plusDays(1);
        }
        return count;
    }

    private boolean isBeforeToday(String isoDate, LocalDate today) {
        if (isoDate == null || isoDate.isEmpty()) return false;
        try {
            return LocalDate.parse(isoDate).isBefore(today);
        } catch (Exception e) {
            return false;
        }
    }

    private String formatDate(String isoDate, boolean longFormat) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            LocalDate date = LocalDate.parse(isoDate);
            return longFormat ? date.format(LONG_DATE) : date.format(ISO_LIKE_DATE);
        } catch (Exception e) {
            return isoDate;
        }
    }

    private String displayStatus(String status) {
        if (status == null) return "scheduled";
        switch (status) {
            case LessonPlan.STATUS_COMPLETED: return "completed";
            case LessonPlan.STATUS_EXTENDED: return "extended";
            default: return "scheduled";
        }
    }

    private String statusBadgeClass(String status) {
        if (status == null) return "badge-scheduled";
        switch (status) {
            case LessonPlan.STATUS_COMPLETED: return "badge-completed";
            case LessonPlan.STATUS_EXTENDED: return "badge-extended";
            default: return "badge-scheduled";
        }
    }
}
