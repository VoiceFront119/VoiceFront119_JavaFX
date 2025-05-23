// 메인 페이지 컨트롤러

package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;

import java.io.IOException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.beans.property.SimpleStringProperty;


public class MainController {
	// 폰트 적용
    @FXML private Text nameText;   // 사용자 이름
    @FXML private Text affiliationText;   // 소속
    @FXML private Text priorityTitleText;  // 우선순위 리스트 설명 텍스트
//    @FXML private TextField searchText;  // 검색 텍스트
   
    
	// 접수 목록
	@FXML private TableView<Reception> receptionTableView;
    @FXML private TableColumn<Reception, String> caseNumColumn;
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
    private String lastSelectedCaseNumber = null;   // 마지막으로 클릭된 행 저장용
    double tableViewY = 200;   // 테이블 뷰 Y 좌표 (고정값 175)
    
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
        
        
        // 각 컬럼에 데이터 바인딩
        caseNumColumn.setCellValueFactory(cellData -> cellData.getValue().caseNumberProperty());
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        timeColumn.setCellValueFactory(cellData -> cellData.getValue().timeProperty());
        addressColumn.setCellValueFactory(cellData -> cellData.getValue().addressProperty());
//        // 대분류-중분류 구조로 보여주기
//        accidentTypeColumn.setCellValueFactory(cellData -> {
//            Reception reception = cellData.getValue();
//            String combined = reception.getMajorCategory() + "-" + reception.getSubCategory();
//            return new SimpleStringProperty(combined);
//        });
        // 줄여서 중분류만 보여주기
        accidentTypeColumn.setCellValueFactory(cellData -> cellData.getValue().subCategoryProperty());
        urgencyColumn.setCellValueFactory(cellData -> cellData.getValue().urgencyProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // TableView에 데이터 리스트 설정
        receptionTableView.setItems(dataList);

        // 초기 예시 데이터 추가 (DB에서 가져오는 걸로 바꿔야 함)
        dataList.add(new Reception("005", "2025-03-25", "14:08:21", "서울특별시 도봉구 방학동", "기타", "기타", "하", "처리중"));
        dataList.add(new Reception("004", "2025-03-25", "13:50:14", "서울특별시 강동구 둔촌동", "화재", "일반화재", "상", "처리중"));
        dataList.add(new Reception("003", "2025-03-25", "12:12:43", "서울특별시 성북구 정릉동", "구급", "질병(중증 외)", "중", "처리완료"));
        dataList.add(new Reception("002", "2025-03-25", "11:10:02", "서울특별시 송파구 마천동", "구조", "안전사고", "하", "처리완료"));
        dataList.add(new Reception("001", "2025-03-25", "11:46:58", "서울특별시 노원구 상계동", "구조", "기타구조", "하", "처리완료"));
        
        
	    // 행 클릭 시 상세정보 창 펼치기
        receptionTableView.setRowFactory(tv -> {
            TableRow<Reception> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    // 클릭된 행의 데이터 가져오기
                    Reception clickedReception = row.getItem();
                    String clickedCaseNumber = clickedReception.getCaseNumber();
                    
                    // 같은 행을 다시 클릭 → 숨기기, 다른 행을 클릭 → 갱신 + 보이기
                    if (clickedCaseNumber.equals(lastSelectedCaseNumber) && detailAnchorPane.isVisible()) {
                        
                        detailAnchorPane.setVisible(false);
                        lastSelectedCaseNumber = null;
                    } else {
                        displayDetails(clickedReception);
                        lastSelectedCaseNumber = clickedCaseNumber;
                        
                        // 상세정보 패널 위치 지정
                        double clickY = event.getSceneY();  // 마우스 클릭 시 마우스의 y좌표
                        int rowIndex = (int) ((clickY - tableViewY) / 40);  // 행 번호 계산, 몇 번쨰 행인지 (행 높이 40px)
                        double anchorPanelY = rowIndex * 40 + tableViewY;  // 행 위치 계산 (행 번호 * 행 높이 + 테이블 뷰 Y 좌표)
                        detailAnchorPane.setLayoutY(anchorPanelY + 42);  // 앵커 패널을 클릭된 행 바로 아래에 위치시키기
                        
                        
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
    
    // 전화 신고 수신 페이지 로드 (현재는 검색 버튼 클릭시)
    @FXML
	private void openincomingcallpage(ActionEvent event) {
    	Parent root;
		try {
			root = FXMLLoader.load(getClass().getResource("incoming_call_page.fxml"));
			
			Stage stage2 = new Stage(); // 새로운 창 생성
	        
	        stage2.setScene(new Scene(root));
	        stage2.setTitle("VoiceFront119 - 신고 전화 수신");

	        // 아이콘 설정
	        Image image = new Image(getClass().getResource("/images/119 Logo-01.png").toExternalForm());
	        stage2.getIcons().add(image);

	        // 새 화면을 표시
	        stage2.show();
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	}
    
    // 상세보기 페이지 로드
    @FXML
	private void opensavedreportpage(ActionEvent event) {
    	Parent root;
		try {
			root = FXMLLoader.load(getClass().getResource("saved_report_page.fxml"));
			
			Stage stage3 = new Stage(); // 새로운 창 생성
	        stage3.setScene(new Scene(root));
	        stage3.setTitle("VoiceFront119 - 접수 상세보기");

	        // 아이콘 설정
	        Image image = new Image(getClass().getResource("/images/119 Logo-01.png").toExternalForm());
	        stage3.getIcons().add(image);

	        // 새 화면을 표시
	        stage3.show();
	        
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
        		+ "신고 위치 : %s 한성아파트 8층\n"
        		+ "신고 내용 : 미닫이 창문이 밖으로 떨어질 것 같음.\n"
        		+ "사고 유형 : %s - %s\n"
        		+ "긴급도 : %s\n"
        		+ "처리 결과 : 현장 확인 후 위험 요소 제거\n"
        		+ "연락처 : 010 - 1234 - 5678\n"
        		+ "\'20231112/Seoul/2022/20221220/converted_[20221220]-[WZ1141328391].wav\'",
                reception.getCaseNumber(), reception.getDate(), reception.getTime(), reception.getAddress(),
                reception.getMajorCategory(), reception.getSubCategory(), reception.getUrgency());

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
                    String caseNum = reception.getCaseNumber();  // 접수 번호 가져오기
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

