package application;

public class AppConfig {

    // API 서버 URL
    public static final String API_BASE_URL = "https://cbc8-34-90-247-73.ngrok-free.app/";
    // 수신 전화번호
    public static final String INCOMING_PHONENUMBER = "010-1234-5678";
    // wav 파일 경로
    public static final String WAVFILE_PATH = "/wav_files/인천_구급.wav";

    private AppConfig() {
        // 인스턴스 생성을 막기 위한 private 생성자 (AppConfig는 정적 변수 전용이므로)
        throw new UnsupportedOperationException("Utility class");
    }
}