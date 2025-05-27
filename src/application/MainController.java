// 메인 페이지 컨트롤러

package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;


public class MainController {
	// 폰트 적용
    @FXML private Text nameText;   // 사용자 이름
    @FXML private Text affiliationText;   // 소속
    @FXML private Text priorityTitleText;  // 우선순위 리스트 설명 텍스트
    @FXML private TextField searchText;  // 검색 텍스트
    @FXML private RadioButton caseNumRadio;
    @FXML private RadioButton addressRadio;
    @FXML private RadioButton typeRadio;
    @FXML private Button incomingcallButton;
    @FXML private Button myPageButton;

	// 접수 목록
	@FXML private TableView<Reception> receptionTableView;
    @FXML private TableColumn<Reception, Number> caseNumColumn;
    @FXML private TableColumn<Reception, String> dateColumn;
    @FXML private TableColumn<Reception, String> timeColumn;
    @FXML private TableColumn<Reception, String> addressColumn;
    @FXML private TableColumn<Reception, String> accidentTypeColumn;
    @FXML private TableColumn<Reception, String> urgencyColumn;
    @FXML private TableColumn<Reception, String> statusColumn;
    
    // 상세 정보
    @FXML private AnchorPane detailAnchorPane;  // 상세정보를 표시할 앵커 패널
    @FXML private Label detailLabel;  // 상세 정보 표시용 라벨
    @FXML private Button detailButton;
    
    // 우선순위 리스트뷰
    @FXML private ListView<String> priorityListView;
    
    // 변수 설정
    private final ObservableList<Reception> dataList = FXCollections.observableArrayList();   // TableView에 들어갈 데이터 목록
    private Integer lastSelectedCaseNumber = null;   // 마지막으로 클릭된 행 저장용
    double tableViewY = 200;   // 테이블 뷰 Y 좌표
    private Connection connection; // 전역 DB 연결 객체
    private Reception selectedReport;
    private String userName;
    private String userId;
    
    // 메인 페이지 사용자 이름 표시
    public void setLoggedInUserName(String userName) {
    	this.userName = userName;
        nameText.setText(userName + " 님");
    }
    
    // 로그인 된 사용자의 id
    public void setLoggedInUserId(String id) {
        this.userId = id;
    }
    
    // 초기화 메서드
    @FXML
    public void initialize() {
    	// 폰트 설정
    	Font TheJamsil2 = Font.loadFont(getClass().getResource("/fonts/TheJamsil2Light.ttf").toExternalForm(), 15);
    	Font TheJamsil3 = Font.loadFont(getClass().getResource("/fonts/TheJamsil3Regular.ttf").toExternalForm(), 15);
    	Font TheJamsil4 = Font.loadFont(getClass().getResource("/fonts/TheJamsil4Medium.ttf").toExternalForm(), 25);
    	
    	nameText.setFont(TheJamsil4);
        affiliationText.setFont(TheJamsil3);
        priorityTitleText.setFont(TheJamsil2);
       
        // DB 연결 초기화
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/emergency_system", "root", "123456");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("DB 연결 실패", "데이터베이스 연결 중 오류가 발생했습니다.");
        }
        
        // 각 컬럼에 데이터 바인딩
        caseNumColumn.setCellValueFactory(cellData -> cellData.getValue().caseNumberProperty());
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        timeColumn.setCellValueFactory(cellData -> cellData.getValue().timeProperty());
        addressColumn.setCellValueFactory(cellData -> cellData.getValue().addressProperty());
        // 줄여서 중분류만 보여주기
        accidentTypeColumn.setCellValueFactory(cellData -> cellData.getValue().subCategoryProperty());
        urgencyColumn.setCellValueFactory(cellData -> cellData.getValue().urgencyProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // TableView에 데이터 리스트 설정
        receptionTableView.setItems(dataList);

        // 검색 그룹 중복 안되게
        ToggleGroup searchGroup = new ToggleGroup();
        caseNumRadio.setToggleGroup(searchGroup);
        addressRadio.setToggleGroup(searchGroup);
        typeRadio.setToggleGroup(searchGroup);
        
        loadDataFromDatabase();
        
        
	    // 행 클릭 시 상세정보 창 펼치기
        receptionTableView.setRowFactory(tv -> {
            TableRow<Reception> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    // 클릭된 행의 데이터 가져오기
                    Reception clickedReception = row.getItem();
                    Integer clickedCaseNumber = clickedReception.getCaseNumber();
                    
                    // 같은 행을 다시 클릭 → 숨기기, 다른 행을 클릭 → 갱신 + 보이기
                    if (clickedCaseNumber.equals(lastSelectedCaseNumber) && detailAnchorPane.isVisible()) {
                        
                        detailAnchorPane.setVisible(false);
                        lastSelectedCaseNumber = null;
                    } else {
                        displayDetails(clickedReception);
                        lastSelectedCaseNumber = clickedCaseNumber;
                        
                        // 상세정보 패널 위치 지정
                        double clickY = event.getSceneY();  // 마우스 클릭 시 마우스의 y좌표
                        int rowIndex = (int) ((clickY - tableViewY) / 35);  // 행 번호 계산, 몇 번쨰 행인지 (행 높이 35px)
                        double anchorPanelY = rowIndex * 35 + tableViewY;  // 행 위치 계산 (행 번호 * 행 높이 + 테이블 뷰 Y 좌표)
                        detailAnchorPane.setLayoutY(anchorPanelY + 37);  // 앵커 패널을 클릭된 행 바로 아래에 위치시키기
                        
                        
                        // 상세정보 앵커 패널 화면에 표시
                        detailAnchorPane.setVisible(true);
                    }
                }
            });
            return row;
        });
        
        // 사건 우선순위 리스트
        updatePriorityList();
	}
    
    // 알림창
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // 검색 기능
    @FXML
	private void onSearchClicked(ActionEvent event) {
	    String searchValue = searchText.getText().trim();
	    if (searchValue.isEmpty()) {
	    	dataList.clear();
	    	loadDataFromDatabase();  // 전체 데이터 로드
	        return;
	    }
	
	    String column = "";
	    if (caseNumRadio.isSelected()) {
	        column = "id";
	    } else if (addressRadio.isSelected()) {
	        column = "address";
	    } else if (typeRadio.isSelected()) {
	        column = "accident_type"; 
	    } else {
	        showAlert("선택 오류", "검색 기준을 선택해주세요.");
	        return;
	    }
	
	    String query = "SELECT * FROM reports WHERE " + column + " LIKE ?";
	    dataList.clear();
	
	    try (PreparedStatement stmt = connection.prepareStatement(query)) {
	        
	        stmt.setString(1, "%" + searchValue + "%");
	        ResultSet rs = stmt.executeQuery();
	
	        while (rs.next()) {
            	int id = rs.getInt("id");
                String date = rs.getString("report_date");
                String time = rs.getString("report_time");
                String address = rs.getString("address");
                
                String accidentType = rs.getString("accident_type");
                String[] typeParts = accidentType.split(" - ");
                String major = typeParts.length > 0 ? typeParts[0] : "";
                String sub = typeParts.length > 1 ? typeParts[1] : "";
                
                String urgency = rs.getString("urgency_level");
                String status = rs.getString("processing_status");
                String reportDetail = rs.getString("report_summary");
                String problemDescription = rs. getString("problem_description");
                String result = rs.getString("processing_result");
                String phoneNumber = rs.getString("phone_number");
                

                Reception reception = new Reception(id, date, time, address, major, sub, urgency, status, reportDetail, problemDescription, result, phoneNumber, "");
                dataList.add(reception);
	        }
	   } catch (Exception e) {
	        e.printStackTrace();
	        showAlert("DB 오류", "검색 중 오류 발생");
	    }
	}
    
    //DB 데이터 로드
    private void loadDataFromDatabase() {
        String query = "SELECT * FROM reports";

        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
            	int id = rs.getInt("id");
                String date = rs.getString("report_date");
                String time = rs.getString("report_time");
                String address = rs.getString("address");
                
                String accidentType = rs.getString("accident_type");
                String[] typeParts = accidentType.split(" - ");
                String major = typeParts.length > 0 ? typeParts[0] : "";
                String sub = typeParts.length > 1 ? typeParts[1] : "";
                
                String urgency = rs.getString("urgency_level");
                String status = rs.getString("processing_status");
                String reportDetail = rs.getString("report_summary");
                String problemDescription = rs. getString("problem_description");
                String result = rs.getString("processing_result");
                String phoneNumber = rs.getString("phone_number");
                

                Reception reception = new Reception(id, date, time, address, major, sub, urgency, status, reportDetail, problemDescription, result, phoneNumber, "");
                dataList.add(reception);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 사건유형 텍스트 분리
    private String[] splitCategory(String full) {
        if (full.contains("-")) return full.split("-", 2);
        else if (full.contains("(")) {
            String main = full.substring(full.indexOf("(") + 1, full.indexOf(")"));
            String sub = full.substring(0, full.indexOf("("));
            return new String[]{main, sub};
        }
        return new String[]{"기타", full};
    }
    
    // 전화 신고 수신 페이지 로드
    @FXML
	private void openincomingcallpage(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("incoming_call_page.fxml"));
	        Parent root = loader.load();
			
			// 컨트롤러 얻기
	        IncomingCallController controller = loader.getController();

	        // 신고접수자 이름 넘기기
	        String reporterName = userName;
	        controller.setReporterName(reporterName);
			
			Stage stage = new Stage(); // 새로운 창 생성
	        stage.setScene(new Scene(root));
	        stage.setTitle("VoiceFront119 - 신고 전화 수신");

	        // 아이콘 설정
	        Image image = new Image(getClass().getResource("/images/119 Logo-01.png").toExternalForm());
	        stage.getIcons().add(image);

	        // 새 화면을 표시
	        stage.show();
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	}
    
    // 상세보기 페이지 로드
    @FXML
	private void opensavedreportpage(ActionEvent event) {
    	// 선택된 행의 ID 가져오기
        Reception selectedReport = receptionTableView.getSelectionModel().getSelectedItem();
        
    	if (selectedReport == null) {
            System.out.println("선택된 신고가 없습니다.");
            return;
        }
    	
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("saved_report_page.fxml"));
	        Parent root = loader.load();
			
			// 컨트롤러 얻기
	        SavedReportController controller = loader.getController();
	        Integer selectedId = selectedReport.getCaseNumber();  // 접수 번호
	        controller.setCaseId(selectedId);  // ID 넘기기
	        
			
			Stage stage = new Stage(); // 새로운 창 생성
	        stage.setScene(new Scene(root));
	        stage.setTitle("VoiceFront119 - 접수 상세보기");

	        // 아이콘 설정
	        Image image = new Image(getClass().getResource("/images/119 Logo-01.png").toExternalForm());
	        stage.getIcons().add(image);

	        // 새 화면을 표시
	        stage.show();
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	}
    
    // 마이 페이지 로드
    @FXML
	private void openmypage(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("my_page.fxml"));
	        Parent root = loader.load();
			
			// 마이페이지 컨트롤러 얻기
	        MypageController controller = loader.getController();
	        controller.setLoggedInUserId(userId);  //사용자 아이디 넘기기
			
			Stage stage = new Stage(); // 새로운 창 생성
	        
	        stage.setScene(new Scene(root));
	        stage.setTitle("VoiceFront119 - 마이페이지");

	        // 아이콘 설정
	        Image image = new Image(getClass().getResource("/images/119 Logo-01.png").toExternalForm());
	        stage.getIcons().add(image);

	        // 새 화면을 표시
	        stage.show();
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	}
    
    
 	// 클릭된 행의 상세 정보 구성
    private void displayDetails(Reception reception) {
        String detailText = String.format(
        		"접수번호: %s\n"
        		+ "접수 시각 : %s %s\n"
        		+ "신고 위치 : %s\n"
        		+ "신고 내용 : %s\n"
        		+ "사고 유형 : %s - %s\n"
        		+ "긴급도 : %s\n"
        		+ "처리 결과 : %s\n"
        		+ "연락처 : %s\n",
                reception.getCaseNumber(), reception.getDate(), reception.getTime(), reception.getAddress(), reception.getproblemDescription(),
                reception.getMajorCategory(), reception.getSubCategory(), reception.getUrgency(), reception.getResult(), reception.getPhoneNumber());

        // 상세 정보 라벨에 내용 설정
        detailLabel.setText(detailText);
    }
    
    // 우선 순위 점수 계산
    public class PriorityScore {
        private static final double URGENCY_WEIGHT = 0.5;
        private static final double DISASTER_WEIGHT = 0.5;

        private static int getUrgencyScore(String urgency) {
            return switch (urgency) {
                case "상" -> 5;
                case "중" -> 3;
                case "하" -> 1;
                default -> 0;
            };
        }

        private static int getDisasterGroupScore(int groupIndex) {
            return switch (groupIndex) {
                case 0 -> 5;
                case 1 -> 3;
                case 2 -> 1;
                default -> 0;
            };
        }

        public static double calculatePriority(String mainCategory, String subCategory, String urgencyLevel) {
        	int groupIndex = AccidentGroupMapping.getGroupIndex(mainCategory, subCategory);
            int urgencyScore = getUrgencyScore(urgencyLevel);
            int disasterScore = getDisasterGroupScore(groupIndex);
            return urgencyScore * URGENCY_WEIGHT + disasterScore * DISASTER_WEIGHT;
        }
    }
    
    // 우선순위 목록 업데이트
    private void updatePriorityList() {
        priorityListView.getItems().clear();  // 기존 항목 비우기

        // 우선순위 점수를 계산하고, 우선순위 점수와 접수 번호를 String 형태로 리스트에 추가
        dataList.stream()
                .map(reception -> {
                    Integer caseNum = reception.getCaseNumber();  // 접수 번호 가져오기
                    double score = PriorityScore.calculatePriority(reception.getMajorCategory(), reception.getSubCategory(), reception.getUrgency());  // 우선순위 점수 계산
                    return String.format("No. %s - 우선순위 점수 %.2f / 5", caseNum, score);
                })
                // 우선순위 점수를 기준으로 내림차순 정렬
                .sorted((text1, text2) -> {
                    
                	double score1 = Double.parseDouble(text1.split(" ")[5]);  // "No. x - 우선순위 점수 y" 형태에서 점수 y 추출
                	double score2 = Double.parseDouble(text2.split(" ")[5]);  // 점수 추출
                    return Double.compare(score2, score1);  // 내림차순 정렬
                })
                .forEach(priorityListView.getItems()::add);  // 리스트에 추가
    }
    
}

