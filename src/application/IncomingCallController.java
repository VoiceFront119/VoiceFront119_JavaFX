// 전화 수신 페이지 컨트롤러

package application;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class IncomingCallController {
	@FXML private Text reportHistoryText;
	@FXML private Label incomingCallLabel;
	@FXML private Label incomingTeleLabel;
	@FXML private Button acceptCallButton;
	
	@FXML private TableView<Reception> historyTableView;
	@FXML private TableColumn<Reception, String> historyDateColumn;
    @FXML private TableColumn<Reception, String> historyResultColumn;
    @FXML private TableColumn<Reception, String> historyTypeColumn;
	
	private static final String BOUNDARY = "Boundary-" + System.currentTimeMillis();
	private ObservableList<Reception> dataList = FXCollections.observableArrayList();
	Connection connection;
	private String reporterName;
	
	
	// 접수 번호 받아오기
    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

	// 초기화 메서드
    @FXML
    public void initialize() {
    	// 폰트 설정
    	Font TheJamsil2_l = Font.loadFont(getClass().getResource("/fonts/TheJamsil2Light.ttf").toExternalForm(), 18);
    	Font TheJamsil2_m = Font.loadFont(getClass().getResource("/fonts/TheJamsil2Light.ttf").toExternalForm(), 14);
    	Font TheJamsil2 = Font.loadFont(getClass().getResource("/fonts/TheJamsil2Light.ttf").toExternalForm(), 13);
    	Font TheJamsil4 = Font.loadFont(getClass().getResource("/fonts/TheJamsil4Medium.ttf").toExternalForm(), 18);
    	
    	incomingCallLabel.setFont(TheJamsil2_l);
    	incomingTeleLabel.setFont(TheJamsil4);
    	reportHistoryText.setFont(TheJamsil2);
    	acceptCallButton.setFont(TheJamsil2_m);
    	
    	// DB 연결 초기화
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/emergency_system", "root", "123456");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // 동일한 전화번호로 접수된 이전 신고 내역
        historyDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        historyTypeColumn.setCellValueFactory(new PropertyValueFactory<>("subCategory"));
        historyResultColumn.setCellValueFactory(new PropertyValueFactory<>("result"));
        
        incomingTeleLabel.setText(String.format("신고자 번호: %s", AppConfig.INCOMING_PHONENUMBER));
        loadHistoryByPhoneNumber(AppConfig.INCOMING_PHONENUMBER);
        historyTableView.setItems(dataList);
    }
    
    // 알림창
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
	
	@FXML
	// 통화수락 버튼을 누르면
	private void onActionCall(ActionEvent event) throws URISyntaxException {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("call_page.fxml"));
            Parent callRoot = loader.load();
			
			// 신고접수 컨트롤러 객체
            EmergencyCallController controller = loader.getController();
            controller.setReporterName(reporterName);  // 신고접수자 이름 넘기기
			
            // 현재 Stage에 메인 페이지를 설정
	        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
	        
	        stage.setScene(new Scene(callRoot));
	        stage.setTitle("VoiceFront119 - 신고 전화 접수 페이지");

	        // 아이콘 설정
	        Image image = new Image(getClass().getResource("/images/119 Logo-01.png").toExternalForm());
	        stage.getIcons().add(image);

	        // 새 화면을 표시
	        stage.show();
	        
	        // API로 WAV 파일 보내서 STT 결과 얻기
	        URL wavFileUrl = getClass().getResource(AppConfig.WAVFILE_PATH);
            if (wavFileUrl == null) {
                throw new FileNotFoundException("WAV 파일을 찾을 수 없습니다.");
            }
	     
            sendWavFileAsync(wavFileUrl)
                .thenAccept(text -> {
                    // UI 업데이트는 FX Application Thread에서
                    Platform.runLater(() -> {
                        controller.setDialogueandSummaryTextArea(text);  // STT 결과 신고접수 컨트롤러로 전달
                    });
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		
	}
	
	// 전화번호로 신고기록 검색
    public void loadHistoryByPhoneNumber(String phoneNumber) {
    	// 데이터리스트 초기화
        dataList.clear();

        String query = "SELECT report_date, accident_type, processing_result FROM reports WHERE phone_number = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, phoneNumber);
            ResultSet rs = pstmt.executeQuery();  // 쿼리 실행 결과

            while (rs.next()) {
                String date = rs.getString("report_date");
                String accident_type = rs.getString("accident_type");
                String result = rs.getString("processing_result");
                
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
                    null, date, "", "", historyMajorCategory, historySubCategory, "", "", "", "", result, "", "");

                dataList.add(reception);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("조회 실패", "데이터를 불러오는 중 오류가 발생했습니다.");
        }
     }
	
	// API 요청 - STT
    private CompletableFuture<String> sendWavFileAsync(URL wavFileUrl) throws IOException, URISyntaxException {
    	// url -> path
    	Path wavFilePath = Paths.get(wavFileUrl.toURI());
    	
    	HttpClient client = HttpClient.newHttpClient();

        byte[] fileBytes = Files.readAllBytes(wavFilePath);

        // multipart/form-data 바디 만들기
        String LINE_FEED = "\r\n";
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(byteStream, StandardCharsets.UTF_8);

        writer.write("--" + BOUNDARY + LINE_FEED);
        writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"" + wavFilePath.getFileName() + "\"" + LINE_FEED);
        writer.write("Content-Type: audio/wav" + LINE_FEED);
        writer.write(LINE_FEED);
        writer.flush();

        byteStream.write(fileBytes);

        writer.write(LINE_FEED);
        writer.write("--" + BOUNDARY + "--" + LINE_FEED);
        writer.flush();

        byte[] multipartBody = byteStream.toByteArray();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(AppConfig.API_BASE_URL + "stt_result"))
            .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
            .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        	    .thenApply(response -> {
        	        String body = response.body();
        	        // {"text":"변환된 텍스트"} 형식
        	        int start = body.indexOf(":\"") + 2;
        	        int end = body.lastIndexOf("\"");
        	        if (start > 1 && end > start) {
        	            String fullText = body.substring(start, end);

        	            // 문장 단위로 분리 (., ?, !를 기준으로)
        	            String[] sentences = fullText.split("(?<=[.?!])\\s*");

        	            return String.join("\n", sentences);
        	        } else {
        	            return "텍스트 변환 실패";
        	        }
        	    });

    }
    
    
}
