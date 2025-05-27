package application;

import java.util.HashMap;
import java.util.Map;

public class AccidentGroupMapping {

    private static final Map<String, Integer> disasterTypeIndexMap = new HashMap<>();

    static {
        String[] type0 = {
            "구급 - 심정지", "구급 - 약물중독", "구급 - 임산부", "구급 - 질병(중증)",
            "구조 - 자살", "화재 - 일반화재", "화재 - 기타화재", "화재 - 산불"
        };
        String[] type1 = {
            "구급 - 기타구급", "구급 - 사고", "구급 - 질병(중증 외)",
            "구조 - 대물사고", "구조 - 안전사고"
        };
        String[] type2 = {
            "구급 - 부상", "구조 - 기타구조", "기타 - 기타"
        };

        for (String key : type0) disasterTypeIndexMap.put(key, 0);
        for (String key : type1) disasterTypeIndexMap.put(key, 1);
        for (String key : type2) disasterTypeIndexMap.put(key, 2);
    }
    
    
    // 인덱스 반환
    public static int getGroupIndex(String mainCategory, String subCategory) {
        return disasterTypeIndexMap.getOrDefault(mainCategory + " - " + subCategory, 2);  // 기본값: 2 (Low)
    }
    
    // 벡터 반환 (원핫인코딩)
    public static int[] getGroupVector(String mainCategory, String subCategory) {
        int idx = getGroupIndex(mainCategory, subCategory);
        int[] vector = new int[3];
        vector[idx] = 1;
        return vector;
    }
}