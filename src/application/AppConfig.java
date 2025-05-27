package application;

public class AppConfig {

    // API 서버 URL
    public static final String API_BASE_URL = "https://b250-35-186-182-103.ngrok-free.app/";
    // 수신 전화번호
    public static final String INCOMING_PHONENUMBER = "010-8888-7777";
    // wav 파일 경로
    public static final String WAVFILE_PATH = "/wav_files/광주_화재.wav";

    private AppConfig() {
        // 인스턴스 생성을 막기 위한 private 생성자 (AppConfig는 정적 변수 전용이므로)
        throw new UnsupportedOperationException("Utility class");
    }
}