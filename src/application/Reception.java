// 신고 접수 정보를 저장, 관리하기 위한 데이터 모델 클래스

package application;

import java.util.Objects;

import javafx.beans.property.*;

public class Reception {
	private final IntegerProperty caseNumber;   // 사건접수번호
    private final StringProperty date;         // 날짜
    private final StringProperty time;         // 시간
    private final StringProperty address;      // 주소
    private final StringProperty majorCategory;  // 사고유형 대분류
    private final StringProperty subCategory;  // 사고유형 중분류
    private final StringProperty urgency;      // 긴급도
    private final StringProperty status;      // 처리현황
    
    private final SimpleStringProperty reportDetail;     // 신고 내용
    private final SimpleStringProperty problemDescription;     // 문제 정의
    private final SimpleStringProperty result;           // 처리 결과
    private final SimpleStringProperty phoneNumber;      // 신고 전화번호
    private final SimpleStringProperty reporter;
    

    // 주 생성자 (필요인자 12개)
    public Reception(Integer caseNum, String date, String time, String address, String majorCategory, String subCategory, String urgency, String status,
    		String reportDetail, String problemDescription, String result, String phoneNumber, String reporter) {
    	this.caseNumber = new SimpleIntegerProperty(caseNum != null ? caseNum : -1);
        this.date = new SimpleStringProperty(date);
        this.time = new SimpleStringProperty(time);
        this.address = new SimpleStringProperty(address);
        this.majorCategory = new SimpleStringProperty(majorCategory);
        this.subCategory = new SimpleStringProperty(subCategory);
        this.urgency = new SimpleStringProperty(urgency);
        this.status = new SimpleStringProperty(status);
        
        this.reportDetail = new SimpleStringProperty(reportDetail);
        this.problemDescription = new SimpleStringProperty(problemDescription);
        this.result = new SimpleStringProperty(result);
        this.phoneNumber = new SimpleStringProperty(phoneNumber);
        this.reporter = new SimpleStringProperty(reporter);
    }
    
    public Reception(Integer caseNumber, String date, String time, String address,
                     String majorCategory, String subCategory, String urgency, String status) {
        this(caseNumber, date, time, address, majorCategory, subCategory, urgency, status,
             "", "", "", "", "");
    }
    
    // 검색용: 처리현황 없이 7개 필드만 받는 생성자
    public Reception(Integer caseNum, String date, String time, String address,
                     String majorCategory, String subCategory, String urgency) {
        this(caseNum, date, time, address, majorCategory, subCategory, urgency, 
        		"", "", "", "", "", "");
    }
    
    // 내용 비교
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Reception that = (Reception) obj;

        return Objects.equals(majorCategory, that.majorCategory) &&
               Objects.equals(subCategory, that.subCategory) &&
               Objects.equals(urgency, that.urgency) &&
               Objects.equals(address, that.address) &&
               Objects.equals(reportDetail, that.reportDetail) &&
               Objects.equals(phoneNumber, that.phoneNumber);
    }
    
    public StringProperty accidentTypeCombinedProperty() {
        return new SimpleStringProperty(getMajorCategory() + "-" + getSubCategory());
    }
    
    
    // Getter
    public Integer getCaseNumber() { return caseNumber.get(); }
    public String getDate() { return date.get(); }
    public String getTime() { return time.get(); }
    public String getAddress() { return address.get(); }
    public String getMajorCategory() { return majorCategory.get(); }
    public String getSubCategory() { return subCategory.get(); }
    
    
    public String getUrgency() { return urgency.get(); }
    public String getStatus() { return status.get(); }
    public String getreportDetail() { return reportDetail.get(); }
    public String getproblemDescription() { return problemDescription.get(); }
    public String getResult() { return result.get(); }
    public String getPhoneNumber() { return phoneNumber.get(); }
    public String getReporter() { return reporter.get(); }

    
    // Property Getter (TableView에서 사용)
    public IntegerProperty caseNumberProperty() { return caseNumber; }
    public StringProperty dateProperty() { return date; }
    public StringProperty timeProperty() { return time; }
    public StringProperty addressProperty() { return address; }
    public StringProperty majorCategoryProperty() { return majorCategory; }
    public StringProperty subCategoryProperty() { return subCategory; }
    
    public StringProperty urgencyProperty() { return urgency; }
    public StringProperty statusProperty() { return status; }
    
    public StringProperty reportDetailProperty() { return reportDetail; }
    public StringProperty reportProblemDescription() { return problemDescription; }
    public StringProperty resultProperty() { return result; }
    public StringProperty phoneNumberProperty() { return phoneNumber; }
    public StringProperty reporterProperty() { return reporter; }
}

