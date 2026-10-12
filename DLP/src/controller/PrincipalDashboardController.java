package controller;

import dao.LessonPlanDAO;
import dao.UserDAO;
import dao.SchoolCalendarDAO;
import dao.SchoolEventDAO;
import util.FeedbackDialog;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import model.LessonPlan;
import model.TeacherProgress;
import model.User;
import model.SchoolCalendar;
import model.SchoolEvent;
import util.SessionManager;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class PrincipalDashboardController {

    // =========================================================
    // FXML FIELDS
    // =========================================================

    @FXML
    private Label userNameLabel;
    @FXML
    private Button logoutButton;
    @FXML
    private Button feedbackButton;
    @FXML
    private Button navDashboardButton;
    @FXML
    private Button navCalendarButton;
    @FXML
    private Button navReportsButton;
    @FXML
    private StackPane contentArea;
    @FXML
    private FlowPane principalKpiContainer;

    // KPI Labels
    @FXML
    private Label totalTeachersLabel;
    @FXML
    private Label averageProgressLabel;
    @FXML
    private Label onTrackLabel;
    @FXML
    private Label needSupportLabel;

    // Teacher status pie KPI
    @FXML
    private PieChart completionBandPieChart;
    @FXML
    private Label completionBandEmptyLabel;
    @FXML
    private Label completionBandTotalLabel;
    @FXML
    private Label onTrackLegendLabel;
    @FXML
    private Label behindLegendLabel;

    @FXML
    private BarChart<String, Number> subjectProgressChart;
    @FXML
    private CategoryAxis subjectAxis;
    @FXML
    private NumberAxis subjectProgressAxis;
    // Teacher Progress Table
    @FXML
    private TableView<TeacherProgress> teacherProgressTable;
    @FXML
    private TableColumn<TeacherProgress, String> teacherNameColumn;
    @FXML
    private TableColumn<TeacherProgress, String> subjectColumn;
    @FXML
    private TableColumn<TeacherProgress, String> gradeColumn;
    @FXML
    private TableColumn<TeacherProgress, String> progressColumn;
    @FXML
    private TableColumn<TeacherProgress, String> lessonsColumn;
    @FXML
    private TableColumn<TeacherProgress, String> actionsColumn;

    // =========================================================
    // DATABASE ACCESS
    // =========================================================

    private final UserDAO userDAO = new UserDAO();
    private final LessonPlanDAO lessonPlanDAO = new LessonPlanDAO();
    private final SchoolCalendarDAO schoolCalendarDAO = new SchoolCalendarDAO();
    private final SchoolEventDAO schoolEventDAO = new SchoolEventDAO();
    private List<TeacherProgress> currentTeacherProgress = new ArrayList<>();
    private Node dashboardView;
    private boolean dashboardDataLoaded;

    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {
        dashboardView = contentArea.getChildren().get(0);
        loadPrincipalName();
        feedbackButton.setTooltip(new Tooltip("Submit feedback"));
        setupTeacherProgressTable();
        setupSubjectProgressChart();
        loadDashboardData();
        setupActivityMonitoring();
        setActiveNavigation(navDashboardButton);
        principalKpiContainer.widthProperty().addListener(
            (observable, oldWidth, newWidth) -> resizeKpiCards(newWidth.doubleValue()));
        resizeKpiCards(principalKpiContainer.getWidth());
    }

    private void setupSubjectProgressChart() {
        subjectProgressAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(subjectProgressAxis, "", "%"));
        subjectAxis.setTickLabelRotation(35);
        subjectProgressChart.setLegendVisible(false);
        subjectProgressChart.setAnimated(false);
        subjectProgressChart.setVerticalGridLinesVisible(false);
        subjectProgressChart.setHorizontalGridLinesVisible(true);
    }

    private void resizeKpiCards(double availableWidth) {
        if (availableWidth <= 0) return;
        int columns = availableWidth >= 4 * 220 + 3 * 16 ? 4 : availableWidth >= 2 * 200 + 16 ? 2 : 1;
        double cardWidth = Math.max(0, (availableWidth - (columns - 1) * 16) / columns);
        for (Node card : principalKpiContainer.getChildren()) {
            if (card instanceof Region) {
                ((Region) card).setPrefWidth(cardWidth);
            }
        }
    }

    private String formatStatus(String status) {
        if (status == null) return "";
        switch (status) {
            case "ON_TRACK":
                return "On Track";
            case "BEHIND":
                return "Behind";
            case "NO_PLANS":
                return "No Plans";
            default:
                return status;
        }
    }

    private String getStatusStyle(String status) {
        if (status == null) return "";
        switch (status) {
            case "ON_TRACK":
                return "-fx-text-fill: #16a34a; -fx-font-weight: bold;";
            case "BEHIND":
                return "-fx-text-fill: #dc2626; -fx-font-weight: bold;";
            default:
                return "";
        }
    }

    private void applyStatusCellFormatting(TableColumn<TeacherProgress, String> column) {
        column.setCellFactory(tableColumn -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                setText(empty ? null : formatStatus(status));
                setStyle(empty ? "" : getStatusStyle(status));
            }
        });
    }

    // =========================================================
    // LOAD PRINCIPAL NAME
    // =========================================================

    private void loadPrincipalName() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null && userNameLabel != null) {
                String username = currentUser.getUsername();
                userNameLabel.setText(username);
            } else if (userNameLabel != null) {
                userNameLabel.setText("Principal");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (userNameLabel != null) {
                userNameLabel.setText("Principal");
            }
        }
    }

    // =========================================================
    // SETUP TEACHER PROGRESS TABLE
    // =========================================================

    private void setupTeacherProgressTable() {
        teacherNameColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progressDisplay"));
        lessonsColumn.setCellValueFactory(new PropertyValueFactory<>("lessonCountDisplay"));
        teacherNameColumn.setCellFactory(column -> new TableCell<>() {
            private final Circle avatar = new Circle(9);
            private final Label initials = new Label();
            private final Label name = new Label();
            private final HBox content = new HBox(8, new StackPane(avatar, initials), name);

            {
                avatar.getStyleClass().add("principal-teacher-avatar");
                initials.getStyleClass().add("principal-teacher-initials");
                name.getStyleClass().add("principal-teacher-name");
                content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String teacherName, boolean empty) {
                super.updateItem(teacherName, empty);
                if (empty || teacherName == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    initials.setText(getTeacherInitials(teacherName));
                    name.setText(teacherName);
                    setGraphic(content);
                    setText(null);
                }
            }
        });

        // Custom cell factory for progress column with progress bar
        progressColumn.setCellFactory(column -> new TableCell<TeacherProgress, String>() {
            private final ProgressBar progressBar = new ProgressBar();
            private final Label progressLabel = new Label();
            private final HBox container = new HBox(8);
            
            {
                progressBar.setPrefWidth(86);
                progressBar.setPrefHeight(7);
                container.getChildren().addAll(progressBar, progressLabel);
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                progressLabel.getStyleClass().add("principal-progress-value");
            }

            @Override
            protected void updateItem(String progressDisplay, boolean empty) {
                super.updateItem(progressDisplay, empty);
                if (empty || progressDisplay == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    try {
                        double progress = Double.parseDouble(progressDisplay.replace("%", ""));
                        progressBar.setProgress(progress / 100);
                        progressBar.getStyleClass().removeAll(
                            "principal-progress-low", "principal-progress-mid", "principal-progress-high");
                        progressBar.getStyleClass().add(progress >= 70
                            ? "principal-progress-high" : progress >= 50
                                ? "principal-progress-mid" : "principal-progress-low");
                        progressLabel.setText(progressDisplay);
                        setText(null);
                        setGraphic(container);
                    } catch (NumberFormatException e) {
                        setText(progressDisplay);
                        setGraphic(null);
                    }
                }
            }
        });

        // Actions column with View Details button
        actionsColumn.setCellFactory(column -> new TableCell<TeacherProgress, String>() {
            private final Button viewButton = new Button("View Details");
            
            {
                viewButton.getStyleClass().add("principal-details-button");
                viewButton.setOnAction(event -> {
                    TeacherProgress progress = getTableView().getItems().get(getIndex());
                    showTeacherDetails(progress);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(viewButton);
                }
            }
        });
    }

    // =========================================================
    // LOAD DASHBOARD DATA
    // =========================================================

    private void loadDashboardData() {
        dashboardDataLoaded = false;
        currentTeacherProgress.clear();
        teacherProgressTable.getItems().clear();
        try {
            // Get all teachers
            List<User> teachers = userDAO.getTeachers();
            List<TeacherProgress> progressList = new ArrayList<>();

            int totalTeachers = teachers.size();
            double totalProgress = 0;
            int teachersWithLessons = 0;
            int behindCount = 0;

            List<LessonPlan> allLessons = new ArrayList<>();

            // Calculate progress for each teacher
            for (User teacher : teachers) {
                List<LessonPlan> lessons = lessonPlanDAO.getLessonsByUserId(teacher.getUserId());
                allLessons.addAll(lessons);
                
                int totalLessons = lessons.size();
                int completedLessons = 0;
                
                for (LessonPlan lesson : lessons) {
                    if (LessonPlan.STATUS_COMPLETED.equals(lesson.getStatus())) {
                        completedLessons++;
                    }
                }

                double progressPercentage = totalLessons > 0
                    ? (completedLessons * 100.0 / totalLessons) : 0;

                boolean hasExtendedLesson = lessons.stream()
                    .anyMatch(lesson -> LessonPlan.STATUS_EXTENDED.equals(lesson.getStatus()));
                String status = totalLessons == 0
                    ? "NO_PLANS" : hasExtendedLesson ? "BEHIND" : "ON_TRACK";
                if (hasExtendedLesson) behindCount++;

                if (totalLessons > 0) teachersWithLessons++;
                if (totalLessons > 0) totalProgress += progressPercentage;

                String subject = getSingleOrMultipleValue(
                    getDistinctNonEmptyValues(lessons, true));
                String grade = getSingleOrMultipleValue(
                    getDistinctNonEmptyValues(lessons, false));

                TeacherProgress progress = new TeacherProgress(
                    teacher.getUserId(),
                    teacher.getUsername(),
                    subject,
                    grade,
                    progressPercentage,
                    completedLessons,
                    totalLessons,
                    status
                );
                progressList.add(progress);
            }

            // Update KPI labels
            totalTeachersLabel.setText(String.valueOf(totalTeachers));
            
            double averageProgress = teachersWithLessons > 0 ? totalProgress / teachersWithLessons : 0;
            averageProgressLabel.setText(String.format(Locale.ROOT, "%.1f%%", averageProgress));
            
            int onTrackCount = teachersWithLessons - behindCount;
            onTrackLabel.setText(String.valueOf(onTrackCount));
            needSupportLabel.setText(String.valueOf(behindCount));

            // Aggregate every teacher's lessons by subject area.
            updateSubjectProgress(allLessons);
            
            updateTeacherStatusPie(teachersWithLessons, onTrackCount, behindCount);

            // Populate table
            teacherProgressTable.getItems().clear();
            teacherProgressTable.getItems().addAll(progressList);
            currentTeacherProgress = new ArrayList<>(progressList);
            dashboardDataLoaded = true;

        } catch (Exception e) {
            e.printStackTrace();
            showError("Dashboard Error", "Unable to load dashboard data: " + e.getMessage());
        }
    }

    // =========================================================
    // AGGREGATE COMPLETION BY SUBJECT AREA
    // =========================================================

    private List<String> getDistinctNonEmptyValues(List<LessonPlan> lessons, boolean subjects) {
        List<String> values = new ArrayList<>();
        for (LessonPlan lesson : lessons) {
            String value = subjects ? lesson.getSubject() : lesson.getGradeLevel();
            if (value != null && !value.isBlank() && !values.contains(value)) values.add(value);
        }
        return values;
    }

    private String getSingleOrMultipleValue(List<String> values) {
        if (values.isEmpty()) return "N/A";
        return values.size() == 1 ? values.get(0) : "Multiple";
    }

    private void updateSubjectProgress(List<LessonPlan> lessons) {
        Map<String, SubjectProgressSummary> subjectProgress =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (LessonPlan lesson : lessons) {
            String subject = lesson.getSubject();
            if (subject == null || subject.isBlank()) continue;

            String subjectName = subject.trim();
            SubjectProgressSummary summary = subjectProgress.computeIfAbsent(
                subjectName, SubjectProgressSummary::new);
            summary.totalLessons++;
            if (LessonPlan.STATUS_COMPLETED.equals(lesson.getStatus())) summary.completedLessons++;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (SubjectProgressSummary summary : subjectProgress.values()) {
            double progressPercent = summary.completedLessons * 100.0 / summary.totalLessons;
            addSubjectProgressData(series, summary.subject, progressPercent);
        }
        subjectAxis.setCategories(FXCollections.observableArrayList(
            subjectProgress.keySet().stream().toList()));
        subjectProgressChart.setData(FXCollections.observableArrayList(series));
    }

    private static final class SubjectProgressSummary {
        private final String subject;
        private int completedLessons;
        private int totalLessons;

        private SubjectProgressSummary(String subject) {
            this.subject = subject;
        }
    }

    private void addSubjectProgressData(XYChart.Series<String, Number> series, String subject, double progress) {
        XYChart.Data<String, Number> data = new XYChart.Data<>(subject, progress);
        data.nodeProperty().addListener((observable, oldNode, node) -> {
            if (node != null) node.getStyleClass().add("principal-subject-bar");
        });
        series.getData().add(data);
    }
    
    private void updateTeacherStatusPie(int teachersWithPlans, int onTrackCount, int behindCount) {
        completionBandTotalLabel.setText(String.valueOf(teachersWithPlans));
        onTrackLegendLabel.setText(formatCompletionLegend(
            "On Track", onTrackCount, getSharePercent(onTrackCount, teachersWithPlans)));
        behindLegendLabel.setText(formatCompletionLegend(
            "Behind", behindCount, getSharePercent(behindCount, teachersWithPlans)));

        completionBandEmptyLabel.setVisible(teachersWithPlans == 0);
        completionBandEmptyLabel.setManaged(teachersWithPlans == 0);
        completionBandPieChart.setVisible(teachersWithPlans > 0);
        completionBandPieChart.setManaged(teachersWithPlans > 0);
        if (teachersWithPlans == 0) {
            completionBandPieChart.setData(FXCollections.observableArrayList());
            return;
        }

        List<PieChart.Data> slices = new ArrayList<>();
        addCompletionBandSlice(slices, "On Track", onTrackCount, "#16a34a");
        addCompletionBandSlice(slices, "Behind", behindCount, "#dc2626");
        completionBandPieChart.setData(FXCollections.observableArrayList(slices));
        completionBandPieChart.setAccessibleText(String.format(
            Locale.ROOT,
            "Teacher status: on track %d percent, behind %d percent",
            Math.round(getSharePercent(onTrackCount, teachersWithPlans)),
            Math.round(getSharePercent(behindCount, teachersWithPlans))));
    }

    private double getSharePercent(int count, int total) {
        return total == 0 ? 0 : count * 100.0 / total;
    }

    private String formatCompletionLegend(String title, int count, double percentage) {
        return String.format(Locale.ROOT, "%s: %d (%.0f%%)", title, count, percentage);
    }

    private String getTeacherInitials(String teacherName) {
        String normalizedName = teacherName == null ? "" : teacherName.trim();
        if (normalizedName.isEmpty()) return "?";
        String[] nameParts = normalizedName.split("\\s+");
        if (nameParts.length > 1) {
            return (nameParts[0].substring(0, 1) + nameParts[nameParts.length - 1].substring(0, 1))
                .toUpperCase(Locale.ROOT);
        }
        return teacherName.substring(0, Math.min(2, teacherName.length())).toUpperCase(Locale.ROOT);
    }

    private void addCompletionBandSlice(List<PieChart.Data> slices, String label, int count, String color) {
        if (count == 0) return;
        PieChart.Data slice = new PieChart.Data(label, count);
        slice.nodeProperty().addListener((observable, oldNode, node) -> {
            if (node != null) {
                node.setStyle("-fx-pie-color: " + color + "; -fx-border-color: transparent; -fx-border-width: 0;");
            }
        });
        slices.add(slice);
    }

    // =========================================================
    // SHOW TEACHER DETAILS
    // =========================================================

    private void showTeacherDetails(TeacherProgress progress) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Teacher Details");
        alert.setHeaderText(progress.getTeacherName());
        
        StringBuilder content = new StringBuilder();
        content.append("Subject: ").append(progress.getSubject()).append("\n");
        content.append("Grade: ").append(progress.getGrade()).append("\n");
        content.append("Progress: ").append(progress.getProgressDisplay()).append("\n");
        content.append("Lessons: ").append(progress.getLessonCountDisplay()).append("\n");
        content.append("Status: ").append(formatStatus(progress.getStatus())).append("\n");
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    // =========================================================
    // NAVIGATION HANDLERS
    // =========================================================

    @FXML
    private void showDashboard() {
        contentArea.getChildren().setAll(dashboardView);
        setActiveNavigation(navDashboardButton);
        loadDashboardData();
    }

    @FXML
    private void showCalendar() {
        try {
            SchoolCalendar calendar = schoolCalendarDAO.getCurrentOrDefaultCalendar();
            if (calendar == null) {
                throw new IllegalStateException("No school calendar is configured.");
            }
            List<SchoolEvent> events = schoolEventDAO.getAllSchoolEvents();
            TableView<SchoolEvent> table = new TableView<>();
            table.getStyleClass().add("data-table");
            table.setPlaceholder(new Label("No school events are scheduled for this academic year."));
            table.getColumns().add(textColumn("Event", "eventName"));
            table.getColumns().add(textColumn("Date", "eventDate"));
            table.getColumns().add(textColumn("Type", "eventType"));
            table.getColumns().add(textColumn("Description", "description"));
            for (SchoolEvent event : events) {
                if (event.getAcademicYear() == calendar.getAcademicYear()) table.getItems().add(event);
            }

            VBox page = createPage("School Calendar", "Academic year " + calendar.getAcademicYear()
                + "  |  " + calendar.getTotalTeachingDays() + " teaching days  |  "
                + calendar.getStartDate() + " to " + calendar.getEndDate());
            page.getChildren().add(table);
            VBox.setVgrow(table, Priority.ALWAYS);
            showPage(page, navCalendarButton);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Calendar Error", "Unable to load school calendar: " + e.getMessage());
        }
    }

    @FXML
    private void showReports() {
        loadDashboardData();
        if (!dashboardDataLoaded) return;
        int completed = currentTeacherProgress.stream().mapToInt(TeacherProgress::getCompletedLessons).sum();
        int planned = currentTeacherProgress.stream().mapToInt(TeacherProgress::getTotalLessons).sum();
        Label summary = new Label("Teachers: " + currentTeacherProgress.size()
            + "    |    Completed lessons: " + completed + " / " + planned);
        summary.getStyleClass().add("section-label");
        Label exportLabel = new Label("Export:");
        Button csvButton = createReportExportButton("CSV", "csv");
        Button pdfButton = createReportExportButton("PDF", "pdf");
        Button wordButton = createReportExportButton("Word", "docx");
        HBox toolbar = new HBox(10, summary, new Pane(), exportLabel,
            csvButton, pdfButton, wordButton);
        HBox.setHgrow(toolbar.getChildren().get(1), Priority.ALWAYS);

        TableView<TeacherProgress> table = createTeacherTable();
        VBox page = createPage("Progress Reports", "A current summary of lesson completion for all active teachers.");
        page.getChildren().addAll(toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        showPage(page, navReportsButton);
    }

    @FXML
    private void refreshDashboard() {
        loadDashboardData();
    }

    @FXML
    private void handleExportReport() {
        javafx.scene.control.ChoiceDialog<String> formatDialog =
            new javafx.scene.control.ChoiceDialog<>("CSV", List.of("CSV", "PDF", "Word"));
        formatDialog.setTitle("Export Principal Progress Report");
        formatDialog.setHeaderText("Choose a report format");
        formatDialog.setContentText("Format:");
        formatDialog.showAndWait().ifPresent(format -> exportReport(
            "CSV".equals(format) ? "csv" : "Word".equals(format) ? "docx" : "pdf"));
    }

    private Button createReportExportButton(String text, String format) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        button.setOnAction(event -> exportReport(format));
        return button;
    }

    private void exportReport(String format) {
        if (!dashboardDataLoaded) loadDashboardData();
        if (!dashboardDataLoaded) return;

        String extension;
        String description;
        switch (format) {
            case "pdf":
                extension = "pdf";
                description = "PDF files";
                break;
            case "docx":
                extension = "docx";
                description = "Word documents";
                break;
            default:
                extension = "csv";
                description = "CSV files";
                break;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Principal Progress Report");
        chooser.setInitialFileName("teacher-progress-report." + extension);
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(description, "*." + extension));
        File file = chooser.showSaveDialog(contentArea.getScene().getWindow());
        if (file == null) return;
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith("." + extension)) {
            file = new File(file.getParentFile(), file.getName() + "." + extension);
        }

        try {
            switch (extension) {
                case "pdf":
                    writePdfReport(file);
                    break;
                case "docx":
                    writeWordReport(file);
                    break;
                default:
                    writeCsvReport(file);
                    break;
            }
            showSuccessMessage("Progress report exported to " + file.getAbsolutePath());
        } catch (IOException | com.lowagie.text.DocumentException e) {
            showError("Export Error", "Unable to save the progress report: " + e.getMessage());
        }
    }

    private void writeCsvReport(File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Teacher,Subject,Grade,Completed Lessons,Total Lessons,Progress,Status");
            writer.newLine();
            for (TeacherProgress progress : currentTeacherProgress) writeReportRow(writer, progress);
        }
    }

    private void writeReportRow(BufferedWriter writer, TeacherProgress progress) throws IOException {
        writer.write(csvValue(progress.getTeacherName()));
        writer.write(",");
        writer.write(csvValue(progress.getSubject()));
        writer.write(",");
        writer.write(csvValue(progress.getGrade()));
        writer.write(",");
        writer.write(String.valueOf(progress.getCompletedLessons()));
        writer.write(",");
        writer.write(String.valueOf(progress.getTotalLessons()));
        writer.write(",");
        writer.write(csvValue(progress.getProgressDisplay()));
        writer.write(",");
        writer.write(csvValue(formatStatus(progress.getStatus())));
        writer.newLine();
    }

    private void writePdfReport(File file) throws IOException, com.lowagie.text.DocumentException {
        Document document = new Document(PageSize.A4.rotate());
        try (FileOutputStream output = new FileOutputStream(file)) {
            PdfWriter.getInstance(document, output);
            document.open();
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Principal Progress Report", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Generated: "
                + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
            document.add(new Paragraph(reportSummary()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[] {2.1f, 1.7f, 1.0f, 1.3f, 1.2f, 1.0f, 1.2f});
            for (String heading : reportHeadings()) table.addCell(heading);
            for (TeacherProgress progress : currentTeacherProgress) {
                for (String value : reportValues(progress)) table.addCell(value);
            }
            document.add(table);
            document.close();
        } finally {
            if (document.isOpen()) document.close();
        }
    }

    private void writeWordReport(File file) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("Principal Progress Report");
            titleRun.setBold(true);
            titleRun.setFontSize(18);

            XWPFParagraph generated = document.createParagraph();
            generated.createRun().setText("Generated: "
                + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            document.createParagraph().createRun().setText(reportSummary());

            XWPFTable table = document.createTable(currentTeacherProgress.size() + 1, 7);
            for (int column = 0; column < reportHeadings().length; column++) {
                XWPFRun header = table.getRow(0).getCell(column).getParagraphs().get(0).createRun();
                header.setText(reportHeadings()[column]);
                header.setBold(true);
            }
            for (int row = 0; row < currentTeacherProgress.size(); row++) {
                String[] values = reportValues(currentTeacherProgress.get(row));
                for (int column = 0; column < values.length; column++) {
                    table.getRow(row + 1).getCell(column).setText(values[column]);
                }
            }

            try (FileOutputStream output = new FileOutputStream(file)) {
                document.write(output);
            }
        }
    }

    private String reportSummary() {
        int completed = currentTeacherProgress.stream()
            .mapToInt(TeacherProgress::getCompletedLessons).sum();
        int planned = currentTeacherProgress.stream()
            .mapToInt(TeacherProgress::getTotalLessons).sum();
        return "Teachers: " + currentTeacherProgress.size()
            + " | Completed lessons: " + completed + " / " + planned;
    }

    private String[] reportHeadings() {
        return new String[] {
            "Teacher", "Subject", "Grade", "Completed Lessons",
            "Total Lessons", "Progress", "Status"
        };
    }

    private String[] reportValues(TeacherProgress progress) {
        return new String[] {
            safeText(progress.getTeacherName()), safeText(progress.getSubject()),
            safeText(progress.getGrade()), String.valueOf(progress.getCompletedLessons()),
            String.valueOf(progress.getTotalLessons()), progress.getProgressDisplay(),
            formatStatus(progress.getStatus())
        };
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    @FXML
    private void handleFeedback() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        FeedbackDialog.showFeedbackDialog(currentUser);
    }

    private String csvValue(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.matches("^[\\s]*[=+@-].*")) safeValue = "'" + safeValue;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private VBox createPage(String title, String description) {
        Label heading = new Label(title);
        heading.getStyleClass().add("app-header-title");
        Label subtitle = new Label(description);
        subtitle.getStyleClass().add("app-header-subtitle");
        VBox page = new VBox(16, heading, subtitle);
        page.getStyleClass().add("content-area");
        page.setFillWidth(true);
        return page;
    }

    private void showPage(Node page, Button activeButton) {
        contentArea.getChildren().setAll(page);
        setActiveNavigation(activeButton);
    }

    private void setActiveNavigation(Button activeButton) {
        for (Button button : List.of(navDashboardButton, navCalendarButton, navReportsButton)) {
            button.getStyleClass().remove("principal-nav-button-active");
        }
        activeButton.getStyleClass().add("principal-nav-button-active");
    }

    private TableView<TeacherProgress> createTeacherTable() {
        TableView<TeacherProgress> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPlaceholder(new Label("No active teachers found."));
        table.getColumns().add(textColumn("Teacher", "teacherName"));
        table.getColumns().add(textColumn("Subject", "subject"));
        table.getColumns().add(textColumn("Grade", "grade"));
        table.getColumns().add(textColumn("Lessons", "lessonCountDisplay"));
        table.getColumns().add(textColumn("Progress", "progressDisplay"));
        TableColumn<TeacherProgress, String> tableStatusColumn = textColumn("Status", "status");
        applyStatusCellFormatting(tableStatusColumn);
        table.getColumns().add(tableStatusColumn);
        TableColumn<TeacherProgress, String> detailsColumn = new TableColumn<>("Details");
        detailsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button detailsButton = new Button("View");
            {
                detailsButton.setOnAction(event -> showTeacherDetails(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(empty ? null : detailsButton);
            }
        });
        table.getColumns().add(detailsColumn);
        table.getItems().setAll(currentTeacherProgress);
        return table;
    }

    private <T> TableColumn<T, String> textColumn(String title, String property) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        return column;
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.getInstance().endSession();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Digital Lesson Planner - Login");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Logout Error", "Unable to return to login screen.");
        }
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupActivityMonitoring() {
        userNameLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_CLICKED,
                    e -> SessionManager.getInstance().resetInactivityTimer());
                newScene.addEventFilter(KeyEvent.KEY_TYPED,
                    e -> SessionManager.getInstance().resetInactivityTimer());
            }
        });
    }
}