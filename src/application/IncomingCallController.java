// 전화 수신 페이지 컨트롤러

package application;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
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

public class IncomingCallController {
	private static final String BOUNDARY = "Boundary-" + System.currentTimeMillis();
	
	@FXML
	// 통화수락 버튼을 누르면
	private void onActionCall(ActionEvent event) throws URISyntaxException {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("call_page.fxml"));
            Parent callRoot = loader.load();
			
			// 신고접수 컨트롤러 객체
            EmergencyCallController controller = loader.getController();
			
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
	        URL wavFileUrl = getClass().getResource("/wav_files/광주_화재.wav");
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
            .uri(URI.create("https://c0e4-35-204-127-207.ngrok-free.app/stt_result"))
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
