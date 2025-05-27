package application;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;

public class MypageController {

    @FXML private Label nameLabel;
    @FXML private Label idLabel;
    @FXML private Label birthLabel;
    @FXML private TextField phoneField;
    @FXML private ImageView profileImageView;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmNewPasswordField;
    @FXML private Label messageLabel;

    private String currentUserId;
    private File selectedImageFile;

    private Connection connection; // 전역 DB 연결 객체
    
    // 초기화 메서드
    @FXML
    public void initialize() {
    	// DB 연결 초기화
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/emergency_system", "root", "pl,ko0987");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("DB 연결 실패", "데이터베이스 연결 중 오류가 발생했습니다.");
        }
        
        // 폰트 설정
        Font TheJamsil2 = Font.loadFont(getClass().getResource("/fonts/TheJamsil2Light.ttf").toExternalForm(), 13);

        nameLabel.setFont(TheJamsil2);
        idLabel.setFont(TheJamsil2);
        birthLabel.setFont(TheJamsil2);
        phoneField.setFont(TheJamsil2);
        currentPasswordField.setFont(TheJamsil2);
        newPasswordField.setFont(TheJamsil2);
        confirmNewPasswordField.setFont(TheJamsil2);
        messageLabel.setFont(TheJamsil2);
        
    }
    
    // 알림창
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void setUserData(String userId) {
        this.currentUserId = userId;

        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE user_ID = ?")) {

            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                nameLabel.setText("이름: " + rs.getString("user_name"));
                idLabel.setText("아이디: " + rs.getString("user_ID"));
                birthLabel.setText("생년월일: " + rs.getString("birth_date"));
                phoneField.setText(rs.getString("user_phone"));

                // ✅ BLOB 이미지 불러오기
                InputStream imageStream = rs.getBinaryStream("profile_image");
                if (imageStream != null) {
                    profileImageView.setImage(new Image(imageStream));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onUploadImageClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("프로필 사진 선택");
        selectedImageFile = fileChooser.showOpenDialog(null);
        if (selectedImageFile != null) {
            profileImageView.setImage(new Image(selectedImageFile.toURI().toString()));
        }
    }

    @FXML
    private void onUpdateClicked() {
        String newPhone = phoneField.getText();
        String currentPwd = currentPasswordField.getText();
        String newPwd = newPasswordField.getText();
        String confirmPwd = confirmNewPasswordField.getText();

        if (!newPwd.isEmpty() || !confirmPwd.isEmpty() || !currentPwd.isEmpty()) {
            if (!newPwd.equals(confirmPwd)) {
                showMessage("새 비밀번호가 다릅니다.", Color.RED);
                return;
            }

            try (PreparedStatement checkPwdStmt = connection.prepareStatement("SELECT password FROM users WHERE user_ID = ?");
                 PreparedStatement updateStmt = connection.prepareStatement("UPDATE users SET user_phone = ?, profile_image = ?, password = ? WHERE user_ID = ?")) {

                checkPwdStmt.setString(1, currentUserId);
                ResultSet rs = checkPwdStmt.executeQuery();

                if (rs.next() && rs.getString("password").equals(currentPwd)) {
                    updateStmt.setString(1, newPhone);

                    
                    if (selectedImageFile != null) {
                        try (FileInputStream fis = new FileInputStream(selectedImageFile)) {
                            updateStmt.setBinaryStream(2, fis, (int) selectedImageFile.length());
                            updateStmt.setString(3, newPwd);
                            updateStmt.setString(4, currentUserId);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        updateStmt.setNull(2, Types.BLOB);
                        updateStmt.setString(3, newPwd);
                        updateStmt.setString(4, currentUserId);
                        updateStmt.executeUpdate();
                    }

                    showMessage("수정이 완료되었습니다.", Color.GREEN);
                    clearPasswordFields();
                } else {
                    showMessage("현재 비밀번호가 틀렸습니다.", Color.RED);
                }

            } catch (Exception e) {
                showMessage("오류 발생: " + e.getMessage(), Color.RED);
            }

        } else {
            // 비밀번호 변경 없이 전화번호/사진만 수정
            try (PreparedStatement updateStmt = connection.prepareStatement("UPDATE users SET user_phone = ?, profile_image = ? WHERE user_ID = ?")) {

                updateStmt.setString(1, newPhone);

                if (selectedImageFile != null) {
                    try (FileInputStream fis = new FileInputStream(selectedImageFile)) {
                        updateStmt.setBinaryStream(2, fis, (int) selectedImageFile.length());
                        updateStmt.setString(3, currentUserId);
                        updateStmt.executeUpdate();
                    }
                } else {
                    updateStmt.setNull(2, Types.BLOB);
                    updateStmt.setString(3, currentUserId);
                    updateStmt.executeUpdate();
                }

                showMessage("정보가 수정되었습니다.", Color.GREEN);

            } catch (Exception e) {
                showMessage("오류 발생: " + e.getMessage(), Color.RED);
            }
        }
    }

    private void showMessage(String msg, Color color) {
        messageLabel.setText(msg);
        messageLabel.setTextFill(color);
        messageLabel.setVisible(true);
    }

    private void clearPasswordFields() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmNewPasswordField.clear();
    }
}
