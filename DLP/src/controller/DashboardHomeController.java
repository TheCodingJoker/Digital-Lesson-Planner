
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
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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
        String name = currentUser != null ? currentUser.getUsername() : "Teacher";
        welcomeLabel.setText("Welcome back, " + name + "!");

        loadDashboardData();
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

        int percent = LESSONS_GOAL == 0 ? 0 : Math.min(100, Math.round((completed * 100f) / LESSONS_GOAL));
        lessonsProgressFill.setPrefWidth(2.4 * percent); // track is ~240px wide
        lessonsProgressLabel.setText(percent + "% complete");

        // Upcoming School Events: illustrative placeholder - this app doesn't yet have a
        // school-events feature, so this mirrors the reference design without real data behind it.
        schoolEventsValue.setText("3");
        nextEventNameLabel.setText("Sports Day");
        nextEventDateLabel.setText("15 May");

        teachingDaysLeftValue.setText(String.valueOf(countTeachingDaysLeft(today, TERM_END_DATE)));

        renderLessonRows(upcomingLessonsBox, plans.stream()
            .filter(p -> !LessonPlan.STATUS_COMPLETED.equals(p.getStatus()))
            .filter(p -> p.getLessonDate() != null && !isBeforeToday(p.getLessonDate(), today))
            .sorted((a, b) -> a.getLessonDate().compareTo(b.getLessonDate()))
            .limit(5)
            .toList(), true);

        renderLessonRows(recentLessonsBox, plans.stream()
            .filter(p -> p.getLessonDate() != null && isBeforeToday(p.getLessonDate(), today.plusDays(1)))
            .sorted((a, b) -> b.getLessonDate().compareTo(a.getLessonDate()))
            .limit(5)
            .toList(), false);
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/fragments/LessonCalendarView.fxml"));
            Parent calendarContent = loader.load();
            LessonCalendarController calendarController = loader.getController();
            calendarController.setTeacherId(currentUser.getUserId());

            DialogHelper.showInfoDialog(welcomeLabel.getScene().getWindow(),
                "\uD83D\uDCC5", "Lesson Calendar", "View all scheduled lessons",
                calendarContent, 820, 640);

            loadDashboardData(); // refresh in case anything changed while the calendar was open
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderLessonRows(VBox container, List<LessonPlan> plans, boolean showLongDate) {
        container.getChildren().clear();

        if (plans.isEmpty()) {
            Label empty = new Label("Nothing to show here yet.");
            empty.getStyleClass().add("lesson-row-empty");
            container.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < plans.size(); i++) {
            LessonPlan plan = plans.get(i);

            Label titleLabel = new Label(plan.getTitle());
            titleLabel.getStyleClass().add("lesson-row-title");

            Label subtitleLabel = new Label(plan.getSubject() + " \u2022 " + plan.getGradeLevel());
            subtitleLabel.getStyleClass().add("lesson-row-subtitle");

            Label dateLabel = new Label(formatDate(plan.getLessonDate(), showLongDate));
            dateLabel.getStyleClass().add("lesson-row-date");

            VBox textBox = new VBox(2, titleLabel, subtitleLabel, dateLabel);

            Label badge = new Label(displayStatus(plan.getStatus()));
            badge.getStyleClass().addAll("status-badge", statusBadgeClass(plan.getStatus()));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(12, textBox, spacer, badge);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("lesson-row");
            row.setPadding(new Insets(12, 4, 12, 4));

            container.getChildren().add(row);
        }
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
