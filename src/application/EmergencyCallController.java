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
import java.sql.SQLException;
import java.sql.Timestamp;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;



public class EmergencyCallController {

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
    
    private Connection connection; // 전역 DB 연결 객체
    private LocalDateTime callStartTime; // 통화 시작 시간
    private Timeline timeUpdater;  // 현재시각 갱신을 위한
    private String mainCategory;
    private String subCategory;
    private Map<String, List<String>> categoryToTypes = new HashMap<>();  // 대분류에 맞는 중분류만 선택할 수 있도록하기 위해
    private String API_URL = "https://c0e4-35-204-127-207.ngrok-free.app/";  // API 주소


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
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/emergency_system", "root", "pl,ko0987");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("DB 연결 실패", "데이터베이스 연결 중 오류가 발생했습니다.");
        }
        
        // 신고자 번호 및 시간 관련 정보 출력
        teleLabel.setText("신고자 번호: 010-8888-7777");
        LocalDate today = LocalDate.now();
        dateLabel.setText("날짜: " + today.format(DateTimeFormatter.ISO_DATE));
        callStartTime = LocalDateTime.now();
        callStartTimeLabel.setText("통화 시작 시간: " + callStartTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        startClock();
        
    	
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
    
    // 알림창
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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

        // 이후 DB 저장 로직 등에 사용 가능
    }
    
    // 신고 유형 콤보박스 선택 시 호출되는 메서드
    private void onCategorySelected() {
        // dialogueTextArea에 있는 텍스트 다시 받아옴
        String text = dialogueTextArea.getText();
        if (text == null || text.trim().isEmpty()) {
            return; // 텍스트가 없으면 return
        }

        // 두 콤보박스 값이 모두 선택되었다면 api 호출
        String mainCategory = categoryComboBox.getValue();
        String subCategory  = typeComboBox.getValue();
        if (mainCategory != null && subCategory != null) {
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
        
        mainCategory = categoryComboBox.getValue();
        subCategory = typeComboBox.getValue();

        if (mainCategory == null || subCategory == null) {
        	
            showAlert("재난 유형 선택 필요", "대분류와 중분류를 모두 선택해주세요.");
            
        } else { // api 호출
        	
	        // 재난유형 벡터화 (긴급도 분류 모델의 입력에 맞게)
	        int[] disasterType = AccidentGroupMapping.getGroupVector(mainCategory, subCategory);
	
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
    	String url = API_URL + "urgency_classification";
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
	    	
	    	String url = API_URL + "gpt_response";
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
	        try {
	            // 저장할 데이터 정리
	            String phoneNumber = teleLabel.getText().replace("신고자 번호: ", "").trim();   // 신고자 번호
	            String accidentType = mainCategory + " - " + subCategory;                    // 사고 유형
	            Timestamp reportTime = Timestamp.valueOf(callStartTime);                     // 신고 시간
	            String reportDate = dateLabel.getText().replace("날짜: ", "").trim();         // 신고 날짜
	            String reportSummary = summaryTextArea.getText();                            // 요약 및 정리 결과
	            String dialogue = dialogueTextArea.getText();                                // 전화 dialogue
	            // 대화 내용 파일로 저장
	            String dialoguePath = saveToFile(dialogue, "dialogue", reportDate);
	            String memo = memoTextArea.getText();                                        // 메모
	            boolean isFalseReport = false;                                               // 허위 신고 여부 변수 (false면 정상신고)
	            // boolean 정수 변환
	            int falseReportInt = isFalseReport ? 1 : 0;  
	            String address = extractAddress(reportSummary);                              // 상세 주소
	            String problemDescription = extractProblemDescription(reportSummary);        // 문제 상황 정의
	            String urgencyLevel = extractUrgency(reportSummary);                         // 긴급도

	            
	            // 쿼리 실행
	            String sql = "INSERT INTO reports (phone_number, accident_type, report_time, report_date, " +
                        "report_summary, dialogue_path, memo, false_report, address, problem_description, urgency_level)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	            PreparedStatement pstmt = connection.prepareStatement(sql);
	            pstmt.setString(1, phoneNumber);
	            pstmt.setString(2, accidentType);
	            pstmt.setTimestamp(3, reportTime);
	            pstmt.setString(4, reportDate);
	            pstmt.setString(5, reportSummary);
	            pstmt.setString(6, dialoguePath);
	            pstmt.setString(7, memo);
	            pstmt.setInt(8, falseReportInt);
	            pstmt.setString(9, address);
	            pstmt.setString(10, problemDescription);
	            pstmt.setString(11, urgencyLevel);

	            pstmt.executeUpdate();

	            showAlert("저장 완료", "신고 정보가 저장되었습니다.");
	        } catch (Exception e) {
	            e.printStackTrace();
	            showAlert("저장 오류", "저장 중 오류 발생");
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
	    
	    private String saveToFile(String content, String prefix, String date) throws IOException {
	    	date = date.replace("-", "");
	    	callStartTime.format(DateTimeFormatter.ofPattern("HHmmss"));
	        String filename = prefix + "_" + date + "_"  + ".txt";
	        String path = "saved_dialogues/" + filename;

	        // 디렉토리 없으면 생성
	        Files.createDirectories(Paths.get("saved_dialogues"));

	        Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8));
	        return path;
	    }
	    
	    
    

}
