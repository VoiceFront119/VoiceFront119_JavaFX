package application;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.layout.AnchorPane;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Optional;

public class MypageController {

    @FXML private TextField nameTextField;
    @FXML private Label idLabel;
    @FXML private Label birthLabel;
    @FXML private TextField phoneField;
    @FXML private ImageView profileImageView;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmNewPasswordField;
    @FXML private Label messageLabel;
    @FXML private AnchorPane rootPane;

    private File selectedImageFile;

    private Connection connection; // 전역 DB 연결 객체
    private String userId;  // 사용자id
    private Stage mainStage;  // 메인페이지 stage
    private boolean isDeleted = false;  // 회원탈퇴 여부
    
    // 로그인 된 사용자의 id (기본키)
    public void setLoggedInUserId(String id) {
        this.userId = id;
        setUserData();
    }
    
    // 메인페이지 stage
    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }
    
    // 초기화 메서드
    @FXML
    public void initialize() {
    	// DB 연결 초기화
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/emergency_system", "root", AppConfig.DB_PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "DB 연결 실패", "데이터베이스 연결 중 오류가 발생했습니다.");
        }
        
        // 폰트 설정
        Font TheJamsil2 = Font.loadFont(getClass().getResource("/fonts/TheJamsil2Light.ttf").toExternalForm(), 13);

        nameTextField.setFont(TheJamsil2);
        idLabel.setFont(TheJamsil2);
        birthLabel.setFont(TheJamsil2);
        phoneField.setFont(TheJamsil2);
        currentPasswordField.setFont(TheJamsil2);
        newPasswordField.setFont(TheJamsil2);
        confirmNewPasswordField.setFont(TheJamsil2);
        messageLabel.setFont(TheJamsil2);
        
        // Stage 접근 지연
        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if (stage != null) {
                stage.setOnCloseRequest(event -> {
                    event.consume();  // 창 닫힘 방지
                    
                    if (isDeleted) {
                        openloginpage();  // 회원 탈퇴시 로그인창 띄우기
                    } else {
                        stage.close();    // 단순 확인 혹은 수정 시에는 마이페이지만 닫기
                    }
                });
            }
        });
        
    }
    
    // 알림창 (AlertType : IMFORMATION, WARNING, ERROR, CONFIRMATION, NONE)
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // 개인정보 출력
    public void setUserData() {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE user_ID = ?")) {

            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
            	nameTextField.setText(rs.getString("user_name"));
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
        String newName = nameTextField.getText();
        String newPhone = phoneField.getText();
        String currentPwd = currentPasswordField.getText();
        String newPwd = newPasswordField.getText();
        String confirmPwd = confirmNewPasswordField.getText();

        if (!newPwd.isEmpty() || !confirmPwd.isEmpty() || !currentPwd.isEmpty()) {
            if (!newPwd.equals(confirmPwd)) {
                showMessage("새 비밀번호가 다릅니다.", Color.RED);
                return;
            }

            try (PreparedStatement checkPwdStmt = connection.prepareStatement("SELECT password FROM users WHERE user_ID = ?")) {
                checkPwdStmt.setString(1, userId);
                ResultSet rs = checkPwdStmt.executeQuery();

                if (rs.next() && rs.getString("password").equals(currentPwd)) {
                    if (selectedImageFile != null) {
                        // 사진 변경 + 비밀번호 변경
                        try (PreparedStatement updateStmt = connection.prepareStatement(
                                "UPDATE users SET user_name=?, user_phone=?, profile_image=?, password=? WHERE user_ID=?")) {
                            
                            try (FileInputStream fis = new FileInputStream(selectedImageFile)) {
                            	updateStmt.setString(1, newName);
                                updateStmt.setString(2, newPhone);
                                updateStmt.setBinaryStream(3, fis);
                                updateStmt.setString(4, newPwd);
                                updateStmt.setString(5, userId);
                                updateStmt.executeUpdate();
                            }
                        }
                    } else {
                        // 사진 변경 X + 비밀번호 변경
                        try (PreparedStatement updateStmt = connection.prepareStatement(
                                "UPDATE users SET user_name=?, user_phone=?, password=? WHERE user_ID=?")) {
                            updateStmt.setString(1, newName);
                            updateStmt.setString(2, newPhone);
                            updateStmt.setString(3, newPwd);
                            updateStmt.setString(4, userId);
                            updateStmt.executeUpdate();
                        }
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
            // 비밀번호 변경 없이 이름/전화번호/사진만 수정
            try {
                if (selectedImageFile != null) {
                    try (PreparedStatement updateStmt = connection.prepareStatement(
                            "UPDATE users SET user_name = ?, user_phone = ?, profile_image = ? WHERE user_ID = ?")) {
                        try (FileInputStream fis = new FileInputStream(selectedImageFile)) {
                        	updateStmt.setString(1, newName);
                            updateStmt.setString(2, newPhone);
                            updateStmt.setBinaryStream(3, fis);
                            updateStmt.setString(4, userId);
                            updateStmt.executeUpdate();
                        }
                        
                    }
                } else {
                    // 사진 변경 없음, 사진 필드 업데이트 쿼리에서 제외
                    try (PreparedStatement updateStmt = connection.prepareStatement(
                            "UPDATE users SET user_name = ?, user_phone = ? WHERE user_ID = ?")) {
                        updateStmt.setString(1, newName);
                        updateStmt.setString(2, newPhone);
                        updateStmt.setString(3, userId);
                        updateStmt.executeUpdate();
                    }
                }

                showMessage("정보가 수정되었습니다.", Color.GREEN);

            } catch (Exception e) {
                showMessage("오류 발생: " + e.getMessage(), Color.RED);
            }
        }
    }
    
    // 회원 데이터 삭제
    private void deleteUserFromDB() {
        try {
            String sql = "DELETE FROM users WHERE user_ID = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, userId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                isDeleted = true;
                showAlert(Alert.AlertType.INFORMATION, "삭제 완료", "신고 정보가 삭제되었습니다.");
            } else {
                showAlert(Alert.AlertType.WARNING, "삭제 실패", "해당 ID의 신고 정보가 없습니다.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "삭제 오류", "삭제 중 오류가 발생했습니다.");
        }
    }
    
    @FXML
    private void onDeleteButtonClicked(ActionEvent event) {
        if (userId == null) {
            showAlert(Alert.AlertType.WARNING, "삭제 불가", "삭제할 신고가 선택되지 않았습니다.");
            return;
        }

        // 확인 창 생성
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("삭제 확인");
        confirmAlert.setHeaderText("회원 탈퇴를 진행하시겠습니까?");
        confirmAlert.setContentText("삭제된 계정은 복구할 수 없습니다.");

        // 버튼 구성
        ButtonType yesButton = new ButtonType("예", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("아니오", ButtonBar.ButtonData.NO);
        confirmAlert.getButtonTypes().setAll(yesButton, noButton);

        // 결과 처리
        Optional<ButtonType> result = confirmAlert.showAndWait();
        // 예 버튼 클릭시 비밀번호 확인 후 계정 삭제
        if (result.isPresent() && result.get() == yesButton) {
        	String inputPassword = showPasswordInputDialog();
            if (inputPassword == null) {
                showAlert(Alert.AlertType.INFORMATION, "취소됨", "비밀번호 확인이 취소되었습니다.");
                return;
            }

            if (verifyPassword(inputPassword)) {
                deleteUserFromDB();
            } else {
                showAlert(Alert.AlertType.ERROR, "비밀번호 불일치", "입력한 비밀번호가 일치하지 않습니다.");
            }
        }
    }
    
    // 비밀번호 확인
    private boolean verifyPassword(String inputPassword) {
        try {
            String sql = "SELECT password FROM users WHERE user_ID = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbPassword = rs.getString("password");
                return inputPassword.equals(dbPassword); // 단순 비교 (암호화 시엔 별도 처리 필요)
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // 비밀번호 입력 창
    private String showPasswordInputDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("비밀번호 확인");
        dialog.setHeaderText("비밀번호를 입력하세요");
        dialog.setContentText("비밀번호:");

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void showMessage(String msg, Color color) {
    	System.out.println(msg);
        messageLabel.setText(msg);
        messageLabel.setTextFill(color);
        messageLabel.setVisible(true);
    }

    private void clearPasswordFields() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmNewPasswordField.clear();
    }
    
    // 마이페이지, 메인페이지 닫기 + 로그인 창 새로 열기
    @FXML
    private void openloginpage() {
        // 마이페이지 닫기
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();

        // 메인 창 닫기
        if (mainStage != null) {
            mainStage.close();
        }
        
        // 로그인 창 띄우기
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login_page.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(root));
            
            loginStage.setTitle("VoiceFront119 - 119 신고접수 보조시스템");
            Image image = new Image(getClass().getResource("/images/119 Logo-01.png").toExternalForm());
            loginStage.getIcons().add(image);
            
            loginStage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
