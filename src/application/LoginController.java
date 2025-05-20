package application;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

//import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {
    // 폰트 적용
    @FXML private Text titleText;     // 프로그램 이름
    @FXML private Text signinText;    // sign in 텍스트
    @FXML private TextField idText;   // ID 텍스트
    @FXML private TextField passwordText;   // 비밀번호 텍스트
    @FXML private Button loginButton;   // 로그인 버튼

    @FXML
    private void openmainpage(ActionEvent event) {
        String inputId = idText.getText();
        String inputPw = passwordText.getText();

        // DB에서 사용자 인증 (비밀번호 다름)
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/emergency_system", "root", "pl,ko0987");
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {

            stmt.setString(1, inputId);
            stmt.setString(2, inputPw);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // 로그인 성공 시 → 메인 페이지로 이동
                Parent root = FXMLLoader.load(getClass().getResource("main_page.fxml"));
                Stage stage1 = (Stage) ((Button) event.getSource()).getScene().getWindow();

                stage1.setScene(new Scene(root));
                stage1.setTitle("VoiceFront119 - 메인 페이지");

                Image image = new Image(getClass().getResource("/images/119 Logo-01.png").toExternalForm());
                stage1.getIcons().add(image);

                stage1.show();
                
            } else {
                // 로그인 실패
                showAlert("로그인 실패", "아이디 또는 비밀번호가 잘못되었습니다.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("에러 발생", "로그인 처리 중 문제가 발생했습니다.");
        }
    }

    // 알림창
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // 초기화 메서드
    @FXML
    public void initialize() {
        // 폰트 설정
        Font BerlinSansFBLarge = Font.loadFont(getClass().getResource("/fonts/BRLNSR.ttf").toExternalForm(), 55);
        Font BerlinSansFBMedium = Font.loadFont(getClass().getResource("/fonts/BRLNSR.ttf").toExternalForm(), 33);
        Font BerlinSansFBSmall = Font.loadFont(getClass().getResource("/fonts/BRLNSR.ttf").toExternalForm(), 16);
        Font TheJamsil2 = Font.loadFont(getClass().getResource("/fonts/TheJamsil2Light.ttf").toExternalForm(), 14);

        titleText.setFont(BerlinSansFBLarge);
        signinText.setFont(BerlinSansFBMedium);
        idText.setFont(TheJamsil2);
        passwordText.setFont(TheJamsil2);
        loginButton.setFont(BerlinSansFBSmall);
    }
}
