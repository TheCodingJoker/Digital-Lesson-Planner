
package controller;

import dao.LessonPlanDAO;
import model.LessonPlan;
import model.User;
import util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportsController {

    @FXML private Label totalValue;
    @FXML private Label completedValue;
    @FXML private Label completionRateValue;
    @FXML private Label behindScheduleValue;

    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> subjectBarChart;

    private final LessonPlanDAO lessonPlanDAO = new LessonPlanDAO();

    @FXML
    public void initialize() {
        loadReportData();
    }

    private void loadReportData() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        List<LessonPlan> plans = lessonPlanDAO.findByTeacher(currentUser.getUserId());
        int total = plans.size();

        long scheduled = plans.stream().filter(p -> LessonPlan.STATUS_SCHEDULED.equals(p.getStatus())).count();
        long completed = plans.stream().filter(p -> LessonPlan.STATUS_COMPLETED.equals(p.getStatus())).count();
        long extended = plans.stream().filter(p -> LessonPlan.STATUS_EXTENDED.equals(p.getStatus())).count();

        LocalDate today = LocalDate.now();
        long behindSchedule = plans.stream()
            .filter(p -> !LessonPlan.STATUS_COMPLETED.equals(p.getStatus()))
            .filter(p -> isBeforeToday(p.getLessonDate(), today))
            .count();

        totalValue.setText(String.valueOf(total));
        completedValue.setText(String.valueOf(completed));
        behindScheduleValue.setText(String.valueOf(behindSchedule));

        int completionRate = total == 0 ? 0 : Math.round((completed * 100f) / total);
        completionRateValue.setText(completionRate + "%");

        statusPieChart.setData(FXCollections.observableArrayList(
            new PieChart.Data("Scheduled (" + scheduled + ")", scheduled),
            new PieChart.Data("Completed (" + completed + ")", completed),
            new PieChart.Data("Extended (" + extended + ")", extended)
        ));

        Map<String, Integer> countsBySubject = new LinkedHashMap<>();
        for (LessonPlan plan : plans) {
            String subject = plan.getSubject() == null ? "Unspecified" : plan.getSubject();
            countsBySubject.merge(subject, 1, Integer::sum);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Integer> entry : countsBySubject.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        subjectBarChart.setData(FXCollections.observableArrayList(series));
    }

    private boolean isBeforeToday(String isoDate, LocalDate today) {
        if (isoDate == null || isoDate.isEmpty()) return false;
        try {
            return LocalDate.parse(isoDate).isBefore(today);
        } catch (Exception e) {
            return false;
        }
    }
}
