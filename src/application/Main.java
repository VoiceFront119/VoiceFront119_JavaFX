package application;
	
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;


public class Main extends Application {
	
	@Override
	public void start(Stage primaryStage) {
		try {
		    // FXML 로드
			Parent root = FXMLLoader.load(getClass().getResource("login_page.fxml"));
			primaryStage.setTitle("VoiceFront119 - 119 신고접수 보조시스템");
			
			Image image = new Image(getClass().getResource("/images/119 Logo-01.png").toExternalForm());
			primaryStage.getIcons().add(image);

			primaryStage.setScene(new Scene(root));
			primaryStage.show();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		
		launch(args);

	}
}
