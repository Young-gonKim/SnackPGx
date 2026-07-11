package snackpgx;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Vector;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class TargetGeneController implements Initializable  {
	@FXML private VBox targetGeneVBox, panelVBox;
	@FXML private CheckBox[] checkBoxList = null; 

	ToggleGroup radioToggleGroup = new ToggleGroup();

	private RootController rootController = null;
	private Stage primaryStage;
	private final int noGenesInRow=5, noPanelsInRow=3;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {

	}
	public void showGenes(RootController rootController, Stage primaryStage) {
		this.rootController = rootController;
		this.primaryStage = primaryStage;

		Vector<GeneMetaData> geneList = rootController.geneMetaDataList;
		checkBoxList = new CheckBox[geneList.size()];
		int cnt = 0;
		HBox hBox = new HBox();
		for(GeneMetaData geneMetaData : geneList) {
			if(cnt%noGenesInRow==0 && cnt!=0) {
				targetGeneVBox.getChildren().add(hBox);
				hBox = new HBox();
			}

			CheckBox checkBox = new CheckBox(geneMetaData.name);
			checkBox.setSelected(geneMetaData.targeted); 
			checkBoxList[cnt++] = checkBox;
			hBox.getChildren().add(checkBox);
			hBox.getChildren().add(new Label("      "));
		}
		if(!hBox.getChildren().isEmpty())
			targetGeneVBox.getChildren().add(hBox);

		//show panels
		Vector<String> panelList = rootController.panelNameList;
		RadioButton[] radioButtonList = new RadioButton[panelList.size()];
		cnt = 0;
		hBox = new HBox();


		boolean prevWasPanel = false;
		for(int i=0;i<panelList.size();i++) {
			String panelName = panelList.get(i);
			boolean isPanel = panelName.endsWith("Panel") || panelName.equals("HealthCheckup");

			// Start a new row at the boundary between the preset panels and the
			// single-gene tests (i.e., a line break after the last panel).
			if(i>0 && prevWasPanel && !isPanel) {
				if(!hBox.getChildren().isEmpty()) {
					panelVBox.getChildren().add(hBox);
					hBox = new HBox();
				}
				cnt = 0;
			}
			prevWasPanel = isPanel;

			if(cnt%noPanelsInRow==0 && cnt!=0) {
				panelVBox.getChildren().add(hBox);
				hBox = new HBox();
			}

			// Display label only; the internal panel key stays "ASM Panel".
			String label = panelName.equals("ASM Panel") ? "Anti-seizure medication panel" : panelName;
			RadioButton radioButton = new RadioButton(label);
			if(i==0) radioButton.setSelected(true);
			radioButton.setToggleGroup(radioToggleGroup);
			radioButton.setUserData(new Integer(i));
			radioButtonList[i] = radioButton;
			cnt++;

			hBox.getChildren().add(radioButton);
			hBox.getChildren().add(new Label("       "));
		}
		if(!hBox.getChildren().isEmpty())
			panelVBox.getChildren().add(hBox);


		radioToggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>(){
			public void changed(ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) {
				if (radioToggleGroup.getSelectedToggle() != null) {
					int index = ((Integer)radioToggleGroup.getSelectedToggle().getUserData()).intValue();
					for(int i=0;i<geneList.size();i++) {
						checkBoxList[i].setSelected(geneList.get(i).panelInclusionList[index]);
						
					}
					
				}                
			}
		});
	}

	public void handleConfirm() {
		rootController.selectedPanel = ((Integer)radioToggleGroup.getSelectedToggle().getUserData()).intValue();
		for(int i=0;i<checkBoxList.length;i++) 
			rootController.geneMetaDataList.get(i).targeted = checkBoxList[i].isSelected();
		rootController.fillResults();
		primaryStage.close();
	}

	public void handleCancel() {
		primaryStage.close();
	}

}
