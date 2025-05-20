package application;

public class AppConfig {

    // API 서버 URL
    public static final String API_BASE_URL = "https://3ccc-34-16-134-209.ngrok-free.app/";

    private AppConfig() {
        // 인스턴스 생성을 막기 위한 private 생성자 (AppConfig는 정적 변수 전용이므로)
        throw new UnsupportedOperationException("Utility class");
    }
}