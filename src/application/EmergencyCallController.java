package application;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javafx.util.Duration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;



public class EmergencyCallController {

	// TextArea
    @FXML private TextArea dialogueTextArea;
    @FXML private TextArea summaryTextArea;
    @FXML private TextArea memoTextArea;

    // 번호 및 시간 관련 label
    @FXML private Label teleLabel;
    @FXML private Label dateLabel;
    @FXML private Label currentTimeLabel;
    @FXML private Label callStartTimeLabel;
    @FXML private Label durationLabel;
    
    // 버튼
    @FXML private Button endCallButton;
    @FXML private Button saveButton;
    
    // 콤보박스
    @FXML private Text caseTypeText;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> typeComboBox;
    
    // 테이블뷰
    @FXML private TableView<Reception> reportHistoryTableView;
    @FXML private TableColumn<Reception, String> historyCaseNumColumn;
    @FXML private TableColumn<Reception, String> historyDateColumn;
    @FXML private TableColumn<Reception, String> historyTypeColumn;
    @FXML private TableColumn<Reception, String> historyReporterColumn;
    
    private ObservableList<Reception> dataList = FXCollections.observableArrayList();
    
    private Connection connection; // 전역 DB 연결 객체
    private LocalDateTime callStartTime; // 통화 시작 시간
    private Timeline timeUpdater;  // 현재시각 갱신을 위한
    private String majorCategory;  // 선택한 대분류
    private String subCategory;  // 선택한 중분류
    private Map<String, List<String>> categoryToTypes = new HashMap<>();  // 대분류에 맞는 중분류만 선택할 수 있도록하기 위한
    
    private boolean isCallEnded = false; // 통화 종료 여부
    private Reception lastSavedReception = null; // 마지막으로 저장한 내용


    private String reporterName;
	
	// 접수 번호 받아오기
    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }
    
    public void initialize() {
    	// 폰트 설정
    	Font TheJamsil2 = Font.loadFont(getClass().getResource("/fonts/TheJamsil2Light.ttf").toExternalForm(), 13);
    	
    	teleLabel.setFont(TheJamsil2);
    	dateLabel.setFont(TheJamsil2);
    	currentTimeLabel.setFont(TheJamsil2);
    	callStartTimeLabel.setFont(TheJamsil2);
    	caseTypeText.setFont(TheJamsil2);
    	
    	// DB 연결 초기화
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/emergency_system", "root", AppConfig.DB_PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "DB 연결 실패", "데이터베이스 연결 중 오류가 발생했습니다.");
        }
        
        // 신고자 번호 및 시간 관련 정보 출력
        teleLabel.setText(String.format("신고자 번호: %s", AppConfig.INCOMING_PHONENUMBER));
        LocalDate today = LocalDate.now();
        dateLabel.setText("날짜: " + today.format(DateTimeFormatter.ISO_DATE));
        callStartTime = LocalDateTime.now();
        callStartTimeLabel.setText("통화 시작 시간: " + callStartTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        startClock();
        
        // 동일한 전화번호로 접수된 이전 신고 내역
        historyCaseNumColumn.setCellValueFactory(new PropertyValueFactory<>("caseNumber"));
        historyDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        historyTypeColumn.setCellValueFactory(cellData -> cellData.getValue().accidentTypeCombinedProperty());
        historyReporterColumn.setCellValueFactory(new PropertyValueFactory<>("reporter"));
        
        String phoneNumber = AppConfig.INCOMING_PHONENUMBER;
        loadHistoryByPhoneNumber(phoneNumber);
        reportHistoryTableView.setItems(dataList);
        
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
        
        // 콤보박스가 바뀔 때마다 onCategorySelected() 호출
        categoryComboBox.setOnAction(e -> onCategorySelected());
        typeComboBox.setOnAction(e -> onCategorySelected());

    }
    
    // 알림창 (AlertType : IMFORMATION, WARNING, ERROR, CONFIRMATION, NONE)
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // 현재 시각 (1초마다 갱신)
    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        timeUpdater = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDateTime now = LocalDateTime.now();
            currentTimeLabel.setText("현재시간: " + now.format(formatter));
        }));
        timeUpdater.setCycleCount(Timeline.INDEFINITE);
        timeUpdater.play();
    }
    
    // 전화번호로 신고기록 검색하여 TableView에 출력
    public void loadHistoryByPhoneNumber(String phoneNumber) {
    	// 데이터리스트 초기화
        dataList.clear();

        String query = "SELECT id, report_date, report_time, accident_type, reporter FROM reports WHERE phone_number = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, phoneNumber);
            ResultSet rs = pstmt.executeQuery();  // 쿼리 실행 결과

            while (rs.next()) {
            	int caseNum = rs.getInt("id");
                String date = rs.getString("report_date");
                String time = rs.getString("report_time");
                String accident_type = rs.getString("accident_type");
                String reporter = rs.getString("reporter");
                
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
                	caseNum, date, time, "", historyMajorCategory, historySubCategory, "", "", "", "", "", "", reporter);

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
    
    // 통화 종료 버튼 클릭시
    @FXML
    private void endCall() {
        LocalDateTime callEndTime = LocalDateTime.now();
        
        // 경과 시간 계산
        java.time.Duration elapsed = java.time.Duration.between(callStartTime, callEndTime);
        
        long minutes = elapsed.toMinutes();
        long seconds = elapsed.getSeconds() % 60;

        // 출력
        String elapsedTimeFormatted = String.format("%d분 %02d초", minutes, seconds);
        durationLabel.setText("경과 시간: " + elapsedTimeFormatted);
        
        isCallEnded = true;
    }
    
    // 신고 유형 콤보박스 선택 시 호출되는 메서드
    private void onCategorySelected() {
        // dialogueTextArea에 있는 텍스트 다시 받아옴
        String text = dialogueTextArea.getText();
        if (text == null || text.trim().isEmpty()) {
            return; // 텍스트가 없으면 return
        }

        // 두 콤보박스 값이 모두 선택되었다면 api 호출
        String majorCategory = categoryComboBox.getValue();
        String subCategory  = typeComboBox.getValue();
        if (majorCategory != null && subCategory != null) {
            // 두 분류가 모두 선택된 경우에만 한 번 호출
            setDialogueandSummaryTextArea(text);
        }
    }
    
    // 신고유형 벡터 int[] -> JsonArray / {0, 0, 1} -> [0, 0, 1]
    private String convertArrayToJsonArray(int[] arr) {
        return Arrays.stream(arr)
                     .mapToObj(String::valueOf)
                     .collect(Collectors.joining(",", "[", "]"));
    }
    
    // Dialog 창과 summary 창에 내용 출력
    public void setDialogueandSummaryTextArea(String text) {
    	
        dialogueTextArea.setText(text);
        
        majorCategory = categoryComboBox.getValue();
        subCategory = typeComboBox.getValue();

        if (majorCategory == null || subCategory == null) {
        	
            showAlert(Alert.AlertType.WARNING, "재난 유형 선택 필요", "대분류와 중분류를 모두 선택해주세요.");
            
        } else { // api 호출
        	
	        // 재난유형 벡터화 (긴급도 분류 모델의 입력에 맞게)
	        int[] disasterType = AccidentGroupMapping.getGroupVector(majorCategory, subCategory);
	
	        // API 결과
	        CompletableFuture<String> urgencyFuture = sendUrgencyClassification(text, disasterType);
	    	CompletableFuture<String> summaryFuture = sendSummaryAnalysis(text);
	    	String urgency = urgencyFuture.join();
	    	String summary = summaryFuture.join();
	    	
	    	summary = summary.replace("\\n", "\n");
	    	
	        // summaryTextArea에 출력
	    	String result = summary + "\n\n• 긴급도: " + urgency;
	    	summaryTextArea.setText(result);
        }
    }
    
    // API 요청 - 긴급도 분류 
    private CompletableFuture<String> sendUrgencyClassification(String text, int[] disasterType) {
    	
    	text = text.replaceAll("\\r?\\n", "");
    	
    	HttpClient client = HttpClient.newHttpClient();
    	String url = AppConfig.API_BASE_URL + "urgency_classification";
        String json = """
            {
                "text": "%s",
                "disaster_type": %s
            }
            """.formatted(text, convertArrayToJsonArray(disasterType));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        		.thenApply(response -> {
                    String body = response.body();
                    int start = body.indexOf(":\"") + 2;
        	        int end = body.lastIndexOf("\"");
        	        if (start > 1 && end > start) {
        	            return body.substring(start, end);
        	        } else {
                        return "파싱 오류";
                    }
                });
     	}

    // API 요청 - GPT 요약 
    private CompletableFuture<String> sendSummaryAnalysis(String text) {
	    	
    	text = text.replaceAll("\\r?\\n", "");
    	
    	String url = AppConfig.API_BASE_URL + "gpt_response";
        String json = """
            {
                "text": "%s"
            }
            """.formatted(text);
        
        HttpClient client = HttpClient.newHttpClient();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        		.thenApply(response -> {
                    String body = response.body();
                    int start = body.indexOf(":\"") + 2;
        	        int end = body.lastIndexOf("\"");
        	        if (start > 1 && end > start) {
        	        	return body.substring(start, end);
        	        } else {
                        return "파싱 오류";
                    }
                });
	    }
	    
    @FXML
    private void saveReportToDB() {
    	// 통화 종료 후에 저장 가능
    	if (!isCallEnded) { 
            showAlert(Alert.AlertType.WARNING, "저장 불가", "통화를 먼저 종료해주세요.");
            return;
        }
    	
    	Reception currentReception = createReceptionFromInput(); // 입력값을 Reception 객체로 변환
    	
    	// 저장 내용이 있는데, 내용 변경도 없다면 저장되지 않음
    	if (lastSavedReception != null && currentReception.equals(lastSavedReception)) {
            showAlert(Alert.AlertType.WARNING, "중복 저장 방지", "변경된 내용이 없습니다.");
            return;
        }
    	
        try {
            // 저장할 데이터 정리
            String phoneNumber = teleLabel.getText().replace("신고자 번호: ", "").trim();   // 신고자 번호
            String accidentType = majorCategory + " - " + subCategory;                   // 사고 유형
            LocalTime localTimeOnly = callStartTime.toLocalTime();                       // 신고 시각
            Time reportTime = Time.valueOf(localTimeOnly); // java.sql.Time 사용
            String reportDate = dateLabel.getText().replace("날짜: ", "").trim();         // 신고 날짜
            String reportSummary = summaryTextArea.getText();                            // 요약 및 정리 결과
            String dialogue = dialogueTextArea.getText();                                // 전화 dialogue
            // 대화 내용 파일로 저장
            String dialoguePath = saveToFile(dialogue, "dialogue", phoneNumber);
            String memo = memoTextArea.getText();                                        // 메모
            boolean isFalseReport = false;                                               // 허위 신고 여부 변수 (false면 정상신고)
            // boolean 정수 변환
            int falseReportInt = isFalseReport ? 1 : 0;  
            String address = extractAddress(reportSummary);                              // 상세 주소
            String problemDescription = extractProblemDescription(reportSummary);        // 문제 상황 정의
            String urgencyLevel = extractUrgency(reportSummary);                         // 긴급도
            String callDuration = durationLabel.getText().replace("경과 시간: ", "").trim();   // 통화 시간
            String reporter = reporterName;

            
            // 쿼리 실행
            String sql = "INSERT INTO reports (phone_number, accident_type, report_time, report_date, " +
                    "report_summary, dialogue_path, memo, false_report, address, problem_description, urgency_level, call_duration, reporter)" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, phoneNumber);
            pstmt.setString(2, accidentType);
            pstmt.setTime(3, reportTime);
            pstmt.setString(4, reportDate);
            pstmt.setString(5, reportSummary);
            pstmt.setString(6, dialoguePath);
            pstmt.setString(7, memo);
            pstmt.setInt(8, falseReportInt);
            pstmt.setString(9, address);
            pstmt.setString(10, problemDescription);
            pstmt.setString(11, urgencyLevel);
            pstmt.setString(12, callDuration);
            pstmt.setString(13, reporter);

            pstmt.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "저장 완료", "신고 정보가 저장되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "저장 오류", "저장 중 오류 발생");
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
    	String reportSummary = summaryTextArea.getText();
        return new Reception(
        		0, "", "", "", 
        		majorCategory, subCategory, 
        		extractUrgency(reportSummary), "", 
        		extractAddress(reportSummary),
        		"", "", 
        		teleLabel.getText().replace("신고자 번호: ", "").trim(), "");
    }
	    
	    

}
