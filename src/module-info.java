module voicefront119 {
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires java.sql;
	requires java.net.http;
	
	opens application to javafx.graphics, javafx.fxml;
}
