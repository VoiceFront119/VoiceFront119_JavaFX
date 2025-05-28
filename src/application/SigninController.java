package application;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.sql.*;
import java.time.LocalDate;

public class SigninController {

	@FXML private Text signInText;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField idField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private DatePicker birthDatePicker;
    @FXML private ImageView profileImageView;
    @FXML private Label errorLabel;
    
    @FXML private Button profileUploadButton;
    @FXML private Button signInButton;

    private File selectedImageFile;
    
    // 초기화 메서드
    @FXML
    public void initialize() {
        // 폰트 설정
    	Font TheJamsil4 = Font.loadFont(getClass().getResource("/fonts/TheJamsil4Medium.ttf").toExternalForm(), 20);
        Font TheJamsil2 = Font.loadFont(getClass().getResource("/fonts/TheJamsil2Light.ttf").toExternalForm(), 13);

        signInText.setFont(TheJamsil4);
        nameField.setFont(TheJamsil2);
        phoneField.setFont(TheJamsil2);
        idField.setFont(TheJamsil2);
        passwordField.setFont(TheJamsil2);
        confirmPasswordField.setFont(TheJamsil2);
        birthDatePicker.getEditor().setFont(TheJamsil2);
        
        profileUploadButton.setFont(TheJamsil2);
        signInButton.setFont(TheJamsil2);
    }

    @FXML
    private void onUploadImageClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("프로필 이미지 선택");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            selectedImageFile = file;
            profileImageView.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void onRegisterClicked() {
        String userId = idField.getText();
        String userName = nameField.getText();
        String userPhone = phoneField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        LocalDate birthDate = birthDatePicker.getValue();

        if (userId.isEmpty() || userName.isEmpty() || userPhone.isEmpty() || password.isEmpty()) {
            showError("모든 필드를 입력해주세요.");
            return;
        }

        if (userPhone.length() < 11) {
            showError("전화번호를 확인해주세요. (11자리 이상)");
            return;
        }

        if (password.length() < 4) {
            showError("비밀번호는 4자리 이상 입력해주세요.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("비밀번호가 일치하지 않습니다.");
            return;
        }

        if (isUserIdDuplicate(userId)) {
            showError("이미 존재하는 아이디입니다.");
            return;
        }

        String query = "INSERT INTO users (user_ID, user_name, password, user_phone, birth_date, profile_image) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/emergency_system", "root", AppConfig.DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, userId);
            stmt.setString(2, userName);
            stmt.setString(3, password);
            stmt.setString(4, userPhone);
            stmt.setDate(5, birthDate != null ? Date.valueOf(birthDate) : null);

            if (selectedImageFile != null) {
                try (FileInputStream fis = new FileInputStream(selectedImageFile)) {
                    stmt.setBinaryStream(6, fis, (int) selectedImageFile.length()); 
                    stmt.executeUpdate();
                }
            } else {
                stmt.setNull(6, java.sql.Types.BLOB);
                stmt.executeUpdate();
            }

            errorLabel.setText("회원가입 성공!");
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setVisible(true);

        } catch (SQLException e) {
            showError("DB 오류: " + e.getMessage());
        } catch (Exception e) {
            showError("파일 오류: " + e.getMessage());
        }
    }

    private boolean isUserIdDuplicate(String userId) {
        String query = "SELECT COUNT(*) FROM users WHERE user_ID = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/emergency_system", "root", AppConfig.DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true; // 오류 시 중복으로 간주
    }

    private void showError(String message) {
    	System.out.println(message);
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(true);
    }
}
