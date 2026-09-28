package controller;

import dao.LessonPlanDAO;
import dao.SchoolEventDAO;
import model.LessonPlan;
import model.SchoolEvent;
import model.User;
import util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;

public class LessonCalendarController {

    @FXML private Label monthYearLabel;
    @FXML private Button prevMonthButton;
    @FXML private Button nextMonthButton;
    @FXML private Button todayButton;
    @FXML private GridPane calendarGrid;
    @FXML private VBox selectedDateLessons;

    private final LessonPlanDAO lessonPlanDAO = new LessonPlanDAO();
    private final SchoolEventDAO schoolEventDAO = new SchoolEventDAO();
    private YearMonth currentMonth;
    private LocalDate selectedDate;
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");

    @FXML
    public void initialize() {
        currentMonth = YearMonth.now();
        selectedDate = LocalDate.now();

        prevMonthButton.setOnAction(e -> navigateMonth(-1));
        nextMonthButton.setOnAction(e -> navigateMonth(1));
        todayButton.setOnAction(e -> goToToday());

        loadCalendar();
    }

    private void navigateMonth(int delta) {
        currentMonth = currentMonth.plusMonths(delta);
        loadCalendar();
    }

    private void goToToday() {
        currentMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        loadCalendar();
    }

    private void loadCalendar() {
        monthYearLabel.setText(currentMonth.format(MONTH_YEAR_FORMATTER));
        calendarGrid.getChildren().clear();

        // Add day headers
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
            calendarGrid.add(dayLabel, i, 0);
        }

        // Get first day of month and number of days
        LocalDate firstOfMonth = currentMonth.atDay(1);
        int daysInMonth = currentMonth.lengthOfMonth();
        int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; // 0 = Sunday

        // Get user's lessons for the month
        User currentUser = SessionManager.getInstance().getCurrentUser();
        List<LessonPlan> lessons = currentUser != null ?
            lessonPlanDAO.findByTeacher(currentUser.getUserId()) : List.of();

        // Get school events for the month
        List<SchoolEvent> events = schoolEventDAO.getAllSchoolEvents();

        // Add empty cells for days before the first of the month
        int row = 1;
        int col = startDayOfWeek;

        // Add day cells
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);

            VBox dayCell = new VBox(2);
            dayCell.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 4; -fx-padding: 8;");
            dayCell.setPrefSize(100, 80);

            Label dayLabel = new Label(String.valueOf(day));
            dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            // Check if this is today
            if (date.equals(LocalDate.now())) {
                dayCell.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 4; -fx-padding: 8; -fx-border-color: #2196f3; -fx-border-width: 2;");
            }

            // Check if this is selected
            if (date.equals(selectedDate)) {
                dayCell.setStyle("-fx-background-color: #bbdefb; -fx-background-radius: 4; -fx-padding: 8; -fx-border-color: #1976d2; -fx-border-width: 2;");
            }

            // Check for events
            for (SchoolEvent event : events) {
                try {
                    LocalDate eventDate = LocalDate.parse(event.getEventDate());
                    if (eventDate.equals(date)) {
                        Label eventLabel = new Label(event.getEventType().equals("FULL_DAY") ? "📅" : "📌");
                        eventLabel.setStyle("-fx-font-size: 10px;");
                        dayCell.getChildren().add(eventLabel);
                    }
                } catch (Exception e) {
                    // Ignore date parse errors
                }
            }

            // Check for lessons
            int lessonCount = 0;
            for (LessonPlan lesson : lessons) {
                try {
                    LocalDate lessonDate = LocalDate.parse(lesson.getLessonDate());
                    if (lessonDate.equals(date)) {
                        lessonCount++;
                    }
                } catch (Exception e) {
                    // Ignore date parse errors
                }
            }

            if (lessonCount > 0) {
                Label lessonLabel = new Label(lessonCount + " lesson(s)");
                lessonLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
                dayCell.getChildren().add(lessonLabel);
            }

            dayCell.getChildren().add(0, dayLabel);

            // Make cell clickable
            final LocalDate clickedDate = date;
            dayCell.setOnMouseClicked(e -> {
                selectedDate = clickedDate;
                loadCalendar();
                loadSelectedDateLessons();
            });

            calendarGrid.add(dayCell, col, row);

            col++;
            if (col >= 7) {
                col = 0;
                row++;
            }
        }

        loadSelectedDateLessons();
    }

    private void loadSelectedDateLessons() {
        selectedDateLessons.getChildren().clear();

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        List<LessonPlan> lessons = lessonPlanDAO.findByTeacher(currentUser.getUserId());

        int count = 0;
        for (LessonPlan lesson : lessons) {
            try {
                LocalDate lessonDate = LocalDate.parse(lesson.getLessonDate());
                if (lessonDate.equals(selectedDate)) {
                    count++;
                    Label lessonLabel = new Label(
                        lesson.getSubject() + " - " + lesson.getTopic() + " (" + lesson.getStatus() + ")"
                    );
                    lessonLabel.setStyle("-fx-padding: 8; -fx-background-color: #f9f9f9; -fx-background-radius: 4;");
                    selectedDateLessons.getChildren().add(lessonLabel);
                }
            } catch (Exception e) {
                // Ignore date parse errors
            }
        }

        if (count == 0) {
            Label noLessons = new Label("No lessons scheduled for this date");
            noLessons.setStyle("-fx-text-fill: #999; -fx-padding: 8;");
            selectedDateLessons.getChildren().add(noLessons);
        }
    }
}