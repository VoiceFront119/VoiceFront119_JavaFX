package application;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;

public class SigninController {

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField idField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private DatePicker birthDatePicker;
    @FXML private ImageView profileImageView;
    @FXML private Label errorLabel;

    private File selectedImageFile;

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
        String profilePath = selectedImageFile != null ? selectedImageFile.getAbsolutePath() : null;

        if (userId.isEmpty() || userName.isEmpty() || userPhone.isEmpty() || password.isEmpty()) {
            showError("모든 필드를 입력해주세요.");
            return;
        }

        if (userPhone.length() < 11) {
            showError("전화번호를 확인해주세요. (11자리 이상)");
            return;
        }

        if (password.length() < 6) {
            showError("비밀번호는 6자리 이상 입력해주세요.");
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

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/new_schema", "root", "1234");
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, userId);
            stmt.setString(2, userName);
            stmt.setString(3, password); // 암호화 없이 저장
            stmt.setString(4, userPhone);
            stmt.setDate(5, birthDate != null ? Date.valueOf(birthDate) : null);
            stmt.setString(6, profilePath);

            stmt.executeUpdate();
            errorLabel.setText("회원가입 성공!");
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setVisible(true);

        } catch (SQLException e) {
            showError("DB 오류: " + e.getMessage());
        }
    }

    private boolean isUserIdDuplicate(String userId) {
        String query = "SELECT COUNT(*) FROM users WHERE user_ID = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/new_schema", "root", "1234");
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(true);
    }
}
