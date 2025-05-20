// 신고 접수 정보를 저장, 관리하기 위한 데이터 모델 클래스

package application;

import javafx.beans.property.*;

public class Reception {
    private final StringProperty caseNumber;   // 사건접수번호
    private final StringProperty date;         // 날짜
    private final StringProperty time;         // 시간
    private final StringProperty address;      // 주소
    private final StringProperty majorCategory;  // 사고유형 대분류
    private final StringProperty subCategory;  // 사고유형 중분류
    private final StringProperty urgency;      // 긴급도
    private final StringProperty status;      // 처리현황
    
    private final SimpleStringProperty addressDetail;    // 상세 주소
    private final SimpleStringProperty reportDetail;     // 신고 내용
    private final SimpleStringProperty result;           // 처리 결과
    private final SimpleStringProperty phoneNumber;      // 신고 전화번호
    private final SimpleStringProperty audioFilePath;    // wav 파일 위치
    

    // 주 생성자 (필요인자 11개)
    public Reception(String caseNum, String date, String time, String address, String majorCategory, String subCategory, String urgency, String status,
    				 String addressDetail, String reportDetail, String result, String phoneNumber, String audioFilePath) {
        this.caseNumber = new SimpleStringProperty(caseNum);
        this.date = new SimpleStringProperty(date);
        this.time = new SimpleStringProperty(time);
        this.address = new SimpleStringProperty(address);
        this.majorCategory = new SimpleStringProperty(majorCategory);
        this.subCategory = new SimpleStringProperty(subCategory);
        this.urgency = new SimpleStringProperty(urgency);
        this.status = new SimpleStringProperty(status);
        
        this.addressDetail = new SimpleStringProperty(addressDetail);
        this.reportDetail = new SimpleStringProperty(reportDetail);
        this.result = new SimpleStringProperty(result);
        this.phoneNumber = new SimpleStringProperty(phoneNumber);
        this.audioFilePath = new SimpleStringProperty(audioFilePath);
    }
    
    // 오버로딩 생성자 (기본 8개만 받음, 나머지는 빈 문자열로 처리)
    public Reception(String caseNumber, String date, String time, String address,
                     String majorCategory, String subCategory, String urgency, String status) {
        this(caseNumber, date, time, address, majorCategory, subCategory, urgency, status,
             "", "", "", "", "");
    }
    

    // Getter
    public String getCaseNumber() { return caseNumber.get(); }
    public String getDate() { return date.get(); }
    public String getTime() { return time.get(); }
    public String getAddress() { return address.get(); }
    public String getMajorCategory() { return majorCategory.get(); }
    public String getSubCategory() { return subCategory.get(); }
    
    
    public String getUrgency() { return urgency.get(); }
    public String getStatus() { return status.get(); }
    
    public String getAddressDetail() { return addressDetail.get(); }
    public String getreportDetail() { return reportDetail.get(); }
    public String getResult() { return result.get(); }
    public String getPhoneNumber() { return phoneNumber.get(); }
    public String getAudioFilePath() { return audioFilePath.get(); }

    
    // Property Getter (TableView에서 사용)
    public StringProperty caseNumberProperty() { return caseNumber; }
    public StringProperty dateProperty() { return date; }
    public StringProperty timeProperty() { return time; }
    public StringProperty addressProperty() { return address; }
    public StringProperty majorCategoryProperty() { return majorCategory; }
    public StringProperty subCategoryProperty() { return subCategory; }
    
    public StringProperty urgencyProperty() { return urgency; }
    public StringProperty statusProperty() { return status; }
    
    public StringProperty addressDetailProperty() { return addressDetail; }
    public StringProperty reportDetailProperty() { return reportDetail; }
    public StringProperty resultProperty() { return result; }
    public StringProperty phoneNumberProperty() { return phoneNumber; }
    public StringProperty audioFilePathProperty() { return audioFilePath; }
}
