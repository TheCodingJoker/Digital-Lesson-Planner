
package controller;

import dao.LessonPlanDAO;
import model.LessonPlan;
import util.DialogHelper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class LessonCalendarController {

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;

    private final LessonPlanDAO lessonPlanDAO = new LessonPlanDAO();
    private static final String[] DAY_NAMES = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy");

    private YearMonth currentMonth;
    private String teacherId;

    @FXML
    public void initialize() {
        currentMonth = YearMonth.now();
        addDayOfWeekHeaders();
    }

    /** Must be called by the opener right after loading this fragment. */
    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
        renderMonth();
    }

    @FXML
    private void handlePrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        renderMonth();
    }

    @FXML
    private void handleNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        renderMonth();
    }

    private void addDayOfWeekHeaders() {
        for (int i = 0; i < DAY_NAMES.length; i++) {
            Label label = new Label(DAY_NAMES[i]);
            label.getStyleClass().add("calendar-day-header");
            label.setMaxWidth(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER);
            calendarGrid.add(label, i, 0);
        }
    }

    private void renderMonth() {
        if (teacherId == null) return;

        monthYearLabel.setText(currentMonth.format(MONTH_YEAR));

        // Clear previously rendered day cells (keep row 0 = weekday headers)
        calendarGrid.getChildren().removeIf(node -> {
            Integer row = GridPane.getRowIndex(node);
            return row != null && row > 0;
        });

        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        List<LessonPlan> plans = lessonPlanDAO.findByTeacherAndDateRange(
            teacherId, monthStart.toString(), monthEnd.toString());

        Map<String, List<LessonPlan>> plansByDate = plans.stream()
            .filter(p -> p.getLessonDate() != null)
            .collect(Collectors.groupingBy(LessonPlan::getLessonDate));

        // DayOfWeek.getValue(): Mon=1..Sun=7. We want Sun=0..Sat=6.
        int startColumn = monthStart.getDayOfWeek().getValue() % 7;

        int column = startColumn;
        int row = 1;
        LocalDate today = LocalDate.now();

        for (int day = 1; day <= monthEnd.getDayOfMonth(); day++) {
            LocalDate cellDate = currentMonth.atDay(day);
            VBox cell = buildDayCell(day, cellDate, plansByDate.get(cellDate.toString()), cellDate.equals(today));
            calendarGrid.add(cell, column, row);

            column++;
            if (column > 6) {
                column = 0;
                row++;
            }
        }
    }

    private VBox buildDayCell(int dayNumber, LocalDate date, List<LessonPlan> lessonsOnDay, boolean isToday) {
        Label dayLabel = new Label(String.valueOf(dayNumber));
        dayLabel.getStyleClass().add("calendar-day-number");

        VBox cell = new VBox(4, dayLabel);
        cell.getStyleClass().add("calendar-day-cell");
        if (isToday) {
            cell.getStyleClass().add("calendar-day-cell-today");
        }
        cell.setMinHeight(78);
        VBox.setVgrow(cell, Priority.ALWAYS);

        if (lessonsOnDay != null) {
            for (LessonPlan plan : lessonsOnDay) {
                Label pill = new Label(plan.getSubject());
                pill.getStyleClass().addAll("calendar-event-pill", statusPillClass(plan.getStatus()));
                pill.setMaxWidth(Double.MAX_VALUE);
                pill.setTooltip(new Tooltip(plan.getTitle() + " \u2014 right-click for options"));
                pill.setContextMenu(buildPillContextMenu(plan));
                cell.getChildren().add(pill);
            }
        }

        return cell;
    }

    private ContextMenu buildPillContextMenu(LessonPlan plan) {
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> handleEdit(plan));

        MenuItem completeItem = new MenuItem("Mark Completed");
        completeItem.setOnAction(e -> handleMarkStatus(plan, LessonPlan.STATUS_COMPLETED));

        MenuItem extendItem = new MenuItem("Mark Extended");
        extendItem.setOnAction(e -> handleMarkStatus(plan, LessonPlan.STATUS_EXTENDED));

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> handleDelete(plan));

        return new ContextMenu(editItem, completeItem, extendItem, deleteItem);
    }

    private void handleEdit(LessonPlan plan) {
        Optional<LessonPlan> result = DialogHelper.showLessonPlanDialog(
            calendarGrid.getScene().getWindow(), plan, plan.getTeacherId());

        result.ifPresent(updatedPlan -> {
            lessonPlanDAO.updateLessonPlan(updatedPlan);
            renderMonth();
        });
    }

    private void handleMarkStatus(LessonPlan plan, String newStatus) {
        lessonPlanDAO.updateStatus(plan.getLessonPlanId(), newStatus);
        renderMonth();
    }

    private void handleDelete(LessonPlan plan) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Lesson Plan");
        confirm.setHeaderText("Delete \"" + plan.getTitle() + "\"?");
        confirm.setContentText("This can't be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            lessonPlanDAO.deleteLessonPlan(plan.getLessonPlanId());
            renderMonth();
        }
    }

    private String statusPillClass(String status) {
        if (status == null) return "calendar-event-scheduled";
        switch (status) {
            case LessonPlan.STATUS_COMPLETED: return "calendar-event-completed";
            case LessonPlan.STATUS_EXTENDED: return "calendar-event-extended";
            default: return "calendar-event-scheduled";
        }
    }
}
