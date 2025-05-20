package application;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class apitest {
	private static String API_URL = "https://71a6-35-204-127-207.ngrok-free.app/";  // API 주소

    public static void main(String[] args) {
    	
    	
    	String text = "여기 선수예요? 119입니다. 아, 네, 여기 서창도예요. 네네. 백동교회 쪽에 지금 연기가 좀 많이 나네. 백, 백동교회. 백동이에요? 백동? 백동교회. 백송교회. 네. 잠깐만요. 맞은 차는 연기가 지금 되게 많이 나네요. 잠깐만요. 연기 색깔 혹시 어떤가요? 흰색 흰색 연기예요. 흰색 연기예요? 네. 흰색 연기가 혹시 흐트러지면서 올라오나요? 아니면 그냥 일자로 굽게 올라오나요? 어 지금 좀, 지금 조금 이제 또 잠잠을 지긴 했네요. 아 뭘 채우는 거 같진 않으시고요? 아 모르겠어요, 저는 아파트에 사는데 네. 갑자기 연기가 너무 심해서 혹시 혹시나 해서요?  어, 그래요? 지금은 또 없었습니다. 지금도 많이 안 나요? 아, 네네네. 채우는 걸 수도 있는데 일단 나가서 한번 확인해보겠습니다. 아, 네네. 네.";
    	int[] disasterType = {0, 0, 1};
    	CompletableFuture<String> urgencyFuture = sendUrgencyClassification(text, disasterType);
    	CompletableFuture<String> summaryFuture = sendSummaryAnalysis(text);
    	String urgency = urgencyFuture.join();   // 작업 완료 대기 후 결과 반환
    	String summary = summaryFuture.join();
    	
    	System.out.println(urgency + "\n" + summary);
    	
    	
    	
    }
    
    // 신고유형 int[] -> string
    private static String convertArrayToJsonArray(int[] arr) {
        return Arrays.stream(arr)
                     .mapToObj(String::valueOf)
                     .collect(Collectors.joining(",", "[", "]"));
    }
    
    
    // API 요청 - 긴급도 분류 
    private static CompletableFuture<String> sendUrgencyClassification(String text, int[] disasterType) {
    	HttpClient client = HttpClient.newHttpClient();
    	String url = API_URL + "urgency_classification";
        String json = """
            {
                "text": "%s",
                "disaster_type": %s
            }
            """.formatted(text, convertArrayToJsonArray(disasterType));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        		.thenApply(response -> {
                    String body = response.body();
                    
                    int start = body.indexOf(":\"") + 2;
        	        int end = body.lastIndexOf("\"");
        	        if (start > 1 && end > start) {
        	            return body.substring(start, end);
        	        } else {
                        return "파싱 오류";
                    }
                });
    }
    
    
    // API 요청 - GPT 요약 
    private static CompletableFuture<String> sendSummaryAnalysis(String text) {
    	
//    	text = text.replaceAll("\\r?\\n", "");
    	
    	String url = API_URL + "gpt_response";
        String json = """
            {
                "text": "%s"
            }
            """.formatted(text);
        
        HttpClient client = HttpClient.newHttpClient();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        		.thenApply(response -> {
                    String body = response.body();
                    
                    int start = body.indexOf(":\"") + 2;
        	        int end = body.lastIndexOf("\"");
        	        if (start > 1 && end > start) {
        	            return body.substring(start, end);
        	        } else {
                        return "파싱 오류";
                    }
                });
    }
        
        
        
}