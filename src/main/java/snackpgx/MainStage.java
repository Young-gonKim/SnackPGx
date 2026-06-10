package snackpgx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class MainStage extends Application {
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setX(0);
		primaryStage.setY(0);
		primaryStage.setTitle("SnackPGx Ver." + RootController.version);
		FXMLLoader loader = new FXMLLoader(getClass().getResource("stage.fxml"));
		Parent root = loader.load();

		RootController controller = loader.getController();
		controller.setPrimaryStage(primaryStage);

		Scene scene = Ui.scene(root);
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	public static void main (String[] args) {
		launch(args);
	}
}
