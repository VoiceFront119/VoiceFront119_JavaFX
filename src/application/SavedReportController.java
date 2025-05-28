package application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.control.ButtonBar;

public class SavedReportController {
	// TextArea
    @FXML private TextArea historyDialogueTextArea;
    @FXML private TextArea historySummaryTextArea;
    @FXML private TextArea historyMemoTextArea;
    @FXML private TextArea resultTextArea;

    // 번호 및 시간 관련 label
    @FXML private Label teleLabel;
    @FXML private Label dateLabel;
    @FXML private Label callStartTimeLabel;
    @FXML private Label durationLabel;
    
    // 버튼
    @FXML private Button deleteButton;
    @FXML private Button editButton;
    @FXML private Button saveButton;
    
    // 콤보박스
    @FXML private Text caseTypeText;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> typeComboBox;
    
    // 테이블뷰
    @FXML private TableView<Reception> reportHistoryTableView;
    @FXML private TableColumn<Reception, String> historyCaseNumColumn;
    @FXML private TableColumn<Reception, String> historyDateColumn;
    @FXML private TableColumn<Reception, String> historyReporterColumn;
    @FXML private TableColumn<Reception, String> historyTypeColumn;
    
    private ObservableList<Reception> dataList = FXCollections.observableArrayList();
    
    private Connection connection; // 전역 DB 연결 객체
    private String majorCategory;  // 선택한 대분류
    private String subCategory;  // 선택한 중분류
    private Map<String, List<String>> categoryToTypes = new HashMap<>();  // 대분류에 맞는 중분류만 선택할 수 있도록하기 위한
    
    private Reception lastSavedReception = null; // 마지막으로 저장한 내용
    
    private Integer caseId;
    private String phoneNum;
    private String date;
    private String reportTime;
    private String duration;
    private String summary;
    private String memo;
    private String callDuration;
    private String result;
    private String reporter;
    
    // 접수 번호 받아오기
    public void setCaseId(Integer caseId) {
        this.caseId = caseId;
        loadData();
    }
    
    // 접수번호로 DB 검색해서 데이터 불러오기
    private void loadData() {
        String query = "SELECT * FROM reports WHERE id = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, caseId);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                // DB 컬럼에서 값 추출
                phoneNum = rs.getString("phone_number");
                date = rs.getString("report_date");
                reportTime = rs.getString("report_time");
                duration = rs.getString("call_duration");
                summary = rs.getString("report_summary");
                memo = rs.getString("memo");
                callDuration = rs.getString("call_duration");
                result = rs.getString("processing_result");
                reporter = rs.getString("reporter");
                
                String accidentType = rs.getString("accident_type");
                String[] typeParts = accidentType.split(" - ");
                majorCategory = typeParts.length > 0 ? typeParts[0] : "";
                subCategory = typeParts.length > 1 ? typeParts[1] : "";
                
                String dialogue = "";
                String dialoguePath = rs.getString("dialogue_path");

                if (dialoguePath != null && !dialoguePath.isEmpty()) {
                    Path path = Paths.get(dialoguePath);
                    try {
                        dialogue = Files.readString(path, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        e.printStackTrace();
                        dialogue = "파일을 읽는 도중 오류가 발생했습니다.";
                    }
                } else {
                    dialogue = "";  // 경로가 없으면 빈 문자열
                }

                // 화면에 표시
                teleLabel.setText("신고자 번호: " + phoneNum);
                dateLabel.setText("날짜: " + date);
                callStartTimeLabel.setText("통화 시작 시간: " + reportTime);
                durationLabel.setText("통화 시간: " + duration);

                historyDialogueTextArea.setText(dialogue);
                historySummaryTextArea.setText(summary);
                historyMemoTextArea.setText(memo);
                resultTextArea.setText(result);
                
                // 동일한 전화번호로 접수된 이전 신고 내역
                historyCaseNumColumn.setCellValueFactory(new PropertyValueFactory<>("caseNumber"));
                historyDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
                historyReporterColumn.setCellValueFactory(new PropertyValueFactory<>("reporter"));
                historyTypeColumn.setCellValueFactory(new PropertyValueFactory<>("subCategory"));
                
                loadHistoryByPhoneNumber(phoneNum);
                reportHistoryTableView.setItems(dataList);
                
                // 콤보박스 설정
                categoryComboBox.setValue(majorCategory);
                List<String> types = categoryToTypes.get(majorCategory);
                if (types != null) {
                    typeComboBox.setItems(FXCollections.observableArrayList(types));
                    typeComboBox.setValue(subCategory);
                }

            } else {
                showAlert(Alert.AlertType.WARNING, "데이터 없음", "해당 ID에 대한 신고 데이터를 찾을 수 없습니다.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "DB 오류", "신고 데이터를 불러오는 중 오류가 발생했습니다.");
        }
        
        // 행 클릭 시 상세정보 창 로드
        reportHistoryTableView.setRowFactory(tv -> {
            TableRow<Reception> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) {
                	// 클릭된 행의 데이터 가져오기
                    Reception clickedReception = row.getItem();
                    Integer clickedCaseNumber = clickedReception.getCaseNumber();
                    opensavedreportpage(clickedCaseNumber);
                }
            });
            return row;
        });
            
        
        
    }
    
    public void initialize() {
    	// 폰트 설정
    	Font TheJamsil2 = Font.loadFont(getClass().getResource("/fonts/TheJamsil2Light.ttf").toExternalForm(), 13);
    	
    	teleLabel.setFont(TheJamsil2);
    	dateLabel.setFont(TheJamsil2);
    	callStartTimeLabel.setFont(TheJamsil2);
    	caseTypeText.setFont(TheJamsil2);
    	
    	// DB 연결 초기화
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/emergency_system", "root", AppConfig.DB_PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "DB 연결 실패", "데이터베이스 연결 중 오류가 발생했습니다.");
        }
        
        // 사고 유형 고르기 (콤보박스 형태)
        categoryComboBox.setItems(FXCollections.observableArrayList("구조", "구급", "화재", "기타"));
        typeComboBox.setItems(FXCollections.observableArrayList(
        		"기타","기타구급","기타구조","기타화재","대물사고","부상","사고","산불","심정지","안전사고","약물중독","일반화재","임산부","자살","질병(중증 외)","질병(중증)"));
        
        categoryToTypes.put("구조", Arrays.asList("기타구조", "대물사고", "사고", "안전사고", "자살"));
        categoryToTypes.put("구급", Arrays.asList("기타구급", "부상", "심정지", "임산부", "질병(중증 외)", "질병(중증)", "약물중독"));
        categoryToTypes.put("화재", Arrays.asList("기타화재", "일반화재", "산불"));
        categoryToTypes.put("기타", Arrays.asList("기타"));

        // 대분류 설정
        categoryComboBox.setItems(FXCollections.observableArrayList(categoryToTypes.keySet()));

        // 대분류 선택 시 중분류 갱신
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                List<String> types = categoryToTypes.get(newVal);
                typeComboBox.setItems(FXCollections.observableArrayList(types));
                typeComboBox.getSelectionModel().clearSelection(); // 기존 선택 초기화
            }
        });
        
    }
    
    // 알림창 (AlertType : IMFORMATION, WARNING, ERROR, CONFIRMATION, NONE)
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // 전화번호로 신고기록 검색하여 TableView에 출력
    public void loadHistoryByPhoneNumber(String phoneNumber) {
    	// 데이터리스트 초기화
        dataList.clear();

        String query = "SELECT id, report_date, accident_type, reporter FROM reports WHERE phone_number = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, phoneNumber);
            ResultSet rs = pstmt.executeQuery();  // 쿼리 실행 결과

            while (rs.next()) {
                String accident_type = rs.getString("accident_type");
                int caseNum = rs.getInt("id");
                
                // accident_type('대분류-중분류') 대분류랑 중분류로 split
                String historyMajorCategory = "";
                String historySubCategory = "";
                if (accident_type != null && accident_type.contains("-")) {
                    String[] parts = accident_type.split("-", 2);
                    historyMajorCategory = parts[0];
                    historySubCategory = parts[1];
                }
                
                // Reception 객체 생성
                Reception reception = new Reception(
                	caseNum, date, "", "", historyMajorCategory, historySubCategory, "", "", "", "", "", "", reporter
                );

                dataList.add(reception);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "조회 실패", "데이터를 불러오는 중 오류가 발생했습니다.");
        }
     }
    
    // 행 클릭시 상세보기 페이지 열기
    private void opensavedreportpage(int selectedId) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("saved_report_page.fxml"));
	        Parent root = loader.load();
			
			// 컨트롤러 얻기
	        SavedReportController controller = loader.getController();
	        controller.setCaseId(selectedId);  // ID 넘기기
			
			Stage stage = new Stage(); // 새로운 창 생성
	        stage.setScene(new Scene(root));
	        stage.setTitle("VoiceFront119 - 접수 상세보기");

	        // 아이콘 설정
	        Image image = new Image(getClass().getResource("/images/119 Logo-01.png").toExternalForm());
	        stage.getIcons().add(image);

	        // 새 화면을 표시
	        stage.show();
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	}
    
    // 수정 버튼 클릭 시 텍스트 영역을 수정 가능하게 만들고 스타일 변경
    @FXML
    private void onEditButtonClick(ActionEvent event) {
    	
    	historySummaryTextArea.setEditable(true);
    	historySummaryTextArea.setStyle("-fx-control-inner-background: #ffffff; -fx-text-fill: black;");
    	
    	historyMemoTextArea.setEditable(true);
    	historyMemoTextArea.setStyle("-fx-control-inner-background: #ffffff; -fx-text-fill: black;");
    	
    }
    
    @FXML
    private void saveReportToDB() {
    	Reception currentReception = createReceptionFromInput(); // 입력값을 Reception 객체로 변환
    	
    	// 저장 내용이 있는데, 내용 변경도 없다면 저장되지 않음
    	if (lastSavedReception != null && currentReception.equals(lastSavedReception)) {
            showAlert(Alert.AlertType.WARNING, "중복 저장 방지", "변경된 내용이 없습니다.");
            return;
        }
    	
        try {
            // 저장할 데이터 정리
        	majorCategory = categoryComboBox.getValue();
            subCategory  = typeComboBox.getValue();
            String accidentType = majorCategory + " - " + subCategory;                   // 사고 유형
            String reportSummary = historySummaryTextArea.getText();                            // 요약 및 정리 결과
            String dialogue = historyDialogueTextArea.getText();                                // 전화 dialogue
            // 대화 내용 파일로 저장
            String dialoguePath = saveToFile(dialogue, "dialogue", phoneNum);
            String memo = historyMemoTextArea.getText();                                        // 메모
            boolean isFalseReport = false;                                               // 허위 신고 여부 변수 (false면 정상신고)
            // boolean 정수 변환
            int falseReportInt = isFalseReport ? 1 : 0;  
            String address = extractAddress(reportSummary);                              // 주소
            String problemDescription = extractProblemDescription(reportSummary);        // 문제 상황 정의
            String urgencyLevel = extractUrgency(reportSummary);                         // 긴급도
            String processingResult = resultTextArea.getText();
            String processingStatus = (result != null && !result.isBlank() && !result.equals("처리중")) ? "처리 완료" : "처리중"; 

            
            // 쿼리: INSERT 또는 UPDATE
            String sql = "INSERT INTO reports " +
                    "(id, phone_number, accident_type, report_time, report_date, " +
                    "report_summary, dialogue_path, memo, false_report, address, " +
                    "problem_description, urgency_level, call_duration, processing_result, processing_status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "phone_number = VALUES(phone_number), " +
                    "accident_type = VALUES(accident_type), " +
                    "report_time = VALUES(report_time), " +
                    "report_date = VALUES(report_date), " +
                    "report_summary = VALUES(report_summary), " +
                    "dialogue_path = VALUES(dialogue_path), " +
                    "memo = VALUES(memo), " +
                    "false_report = VALUES(false_report), " +
                    "address = VALUES(address), " +
                    "problem_description = VALUES(problem_description), " +
                    "urgency_level = VALUES(urgency_level), " +
                    "call_duration = VALUES(call_duration)," +
                    "processing_result = VALUES(processing_result)," +
                    "processing_status = VALUES(processing_status)";

            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, caseId);
            pstmt.setString(2, phoneNum);
            pstmt.setString(3, accidentType);
            pstmt.setString(4, reportTime);
            pstmt.setString(5, date);
            pstmt.setString(6, reportSummary);
            pstmt.setString(7, dialoguePath);
            pstmt.setString(8, memo);
            pstmt.setInt(9, falseReportInt);
            pstmt.setString(10, address);
            pstmt.setString(11, problemDescription);
            pstmt.setString(12, urgencyLevel);
            pstmt.setString(13, callDuration);
            pstmt.setString(14, processingResult);
            pstmt.setString(15, processingStatus);

            pstmt.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "저장 완료", "신고 정보가 저장되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "저장 오류", "저장 중 오류 발생");
        }
    }
    
    // 접수 데이터 삭제
    private void deleteReportFromDB() {
        try {
            String sql = "DELETE FROM reports WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, caseId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert(Alert.AlertType.INFORMATION, "삭제 완료", "신고 정보가 삭제되었습니다.");
                lastSavedReception = null;
            } else {
                showAlert(Alert.AlertType.WARNING, "삭제 실패", "해당 ID의 신고 정보가 없습니다.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "삭제 오류", "삭제 중 오류가 발생했습니다.");
        }
    }
    
    @FXML
    private void onDeleteButtonClicked(ActionEvent event) {
        if (caseId == null) {
            showAlert(Alert.AlertType.WARNING, "삭제 불가", "삭제할 신고가 선택되지 않았습니다.");
            return;
        }

        // 확인 창 생성
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("삭제 확인");
        confirmAlert.setHeaderText("신고 정보를 삭제하시겠습니까?");
        confirmAlert.setContentText("삭제된 데이터는 복구할 수 없습니다.");

        // 버튼 구성
        ButtonType yesButton = new ButtonType("예", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("아니오", ButtonBar.ButtonData.NO);
        confirmAlert.getButtonTypes().setAll(yesButton, noButton);

        // 결과 처리
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == yesButton) {
            deleteReportFromDB();
        }
    }
    
    
    // summaryTextArea에서 주소 추출
    private String extractAddress(String summary) {
        int index = summary.indexOf("신고 위치:");
        if (index != -1) {
            int end = summary.indexOf("\n", index);
            return summary.substring(index + 6, end != -1 ? end : summary.length()).trim();
        }
        return "주소 미확인";
    }
    
    // summaryTextArea에서 문제 상황 추출
    private String extractProblemDescription(String summary) {
        int index = summary.indexOf("신고 내용:");
        if (index != -1) {
            int end = summary.indexOf("\n", index);
            return summary.substring(index + 6, end != -1 ? end : summary.length()).trim();
        }
        return "문제 미확인";
    }
    
    // summaryTextArea에서 긴급도 추출
    private String extractUrgency(String summary) {
        int index = summary.lastIndexOf("• 긴급도: ");
        if (index != -1) {
            return summary.substring(index + 7).trim();
        }
        return "미확인";
    }
    
    // 문자열 -> .txt로 바꿔서 파일 경로 리턴
    private String saveToFile(String content, String prefix, String phoneNumber) throws IOException {
    	phoneNumber = phoneNumber.replaceAll("-", "");
    	
    	LocalDateTime now = LocalDateTime.now();
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    	String timestamp = now.format(formatter);
    	
    	// dialogue_전화번호_날짜_시간.txt 형태로 저장
        String filename = prefix + "_" + phoneNumber + "_"  + timestamp + ".txt";
        String path = "saved_dialogues/" + filename;

        // 디렉토리 없으면 생성
        Files.createDirectories(Paths.get("saved_dialogues"));

        Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8));
        return path;
    }
    
    // 중복저장 방지를 위한 reception 객체 생성
    private Reception createReceptionFromInput() {
    	String reportSummary = historySummaryTextArea.getText();
        return new Reception(
        		caseId, "", "", "", 
        		majorCategory, subCategory, 
        		extractUrgency(reportSummary), "", 
        		extractAddress(reportSummary),
        		"", "", 
        		teleLabel.getText().replace("신고자 번호: ", "").trim(), "");
    }
    
    
    
}
