package snackpgx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class RootController implements Initializable {




	private String resourcePath = AppPaths.resourcesDir();
	// Input column-mapping file (sheet index 1). Selectable via Settings > Input Parameters.
	private String inputSettingsPath = resourcePath + "/settings.xlsx";

	public static String version = "2.00";
	public static String DBversion = "2026.05.31";

	//update history : see UPDATE_HISTORY.txt

	public Vector<String> panelNameList = new Vector<String>(); 
	public int selectedPanel = 0;
	public TreeMap<String, String> commentMap = new TreeMap<String, String>();
	public HashMap<String, Integer> inputColumnMap = new HashMap<String, Integer>();
	// Set true by Sample parsing when the selected Input Parameters mapping does
	// not match the file (e.g. WGS settings used on panel data). Used to show a
	// single explanatory dialog instead of one popup per malformed row.
	public boolean inputTypeError = false;


	/**
	selectedSample : changes when the sampleListView selection changes (+ on first load: reset to 0 after iteration)

	<Overview>
	- actualRun builds speciesList and selectedSpeciesList
	- fillResults renders them on screen.

	 */

	private Sample[] sampleArray = null;
	private int selectedSample = -1;
	private String batchReport = "";
	public Vector<GeneMetaData> geneMetaDataList = null;
	public static TreeMap<String, String> phenotypeDescriptionMap = new TreeMap<String, String>();

	public static String techName1 = "", techName2 = "", doctorName1 = "", doctorName2 = "", doctorName3 = "";

	// The biogeographic group whose allele/diplotype frequencies are loaded from the
	// ClinPGx frequency tables. Users pick one of the 9 ClinPGx groups from
	// Settings > Ethnicity; genes lacking the chosen group's column show blank/N-A.
	public static String selectedEthnicity = "East Asian";
	public static final String[] ethnicityList = {
		"African American/Afro-Caribbean",
		"American",
		"Central/South Asian",
		"East Asian",
		"European",
		"Latino",
		"Near Eastern",
		"Oceanian",
		"Sub-Saharan African"
	};

	@FXML private TextField techNameField1, techNameField2, doctorNameField1, doctorNameField2, doctorNameField3;
	@FXML private ListView<String> sampleListView;
	@FXML private VBox vBox;

	ToggleGroup radioToggleGroup = new ToggleGroup();
	@FXML private RadioButton variantRadio, diplotypeRadio, reportRadio, batchReportRadio, batchVariantRadio;


	private enum Context {
		VARIANT, DIPLOTYPE, REPORT, BATCH_REPORT, BATCH_VARIANT;
	}

	private Context context = Context.VARIANT;
	private String lastVisitedDir=".";
	private Stage primaryStage;
	public void setPrimaryStage(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}

	private boolean numToBool (String num) {
		if(num.equals("1"))
			return true;
		else return false;
	}

	public int getPanelIndex (String panelName) {
		for(int i=0;i<panelNameList.size();i++) {
			if(panelNameList.get(i).contains(panelName))
				return i;
		}
		return -1; 
	}

	public static String addAbbreviation (String input) {
		String ret = "";

		if(input.equals("Normal Metabolizer"))
			ret = input + " (NM)";
		else if(input.equals("Intermediate Metabolizer"))
			ret = input + " (IM)";
		else if(input.equals("Poor Metabolizer"))
			ret = input + " (PM)";
		else if(input.equals("Rapid Metabolizer"))
			ret = input + " (RM)";
		else if(input.equals("Ultrarapid Metabolizer"))
			ret = input + " (UM)";
		else ret = input;

		return ret;
	}

	private void readPhenotypeTable() {
		String fileName = resourcePath + "/phenotype_table.xlsx";
		File file = new File(fileName);
		try (XSSFWorkbook workbook = new XSSFWorkbook(file);){
			XSSFSheet curSheet;
			XSSFRow curRow;
			XSSFCell curCell;
			curSheet = workbook.getSheetAt(0);
			for(int i=1;i<curSheet.getPhysicalNumberOfRows();i++) {
				String gene = "";
				String phenotypeDescription = "";
				String phenotype = "";
				curRow = curSheet.getRow(i);

				curCell = curRow.getCell(0);
				gene = curCell.getStringCellValue().trim();


				curCell = curRow.getCell(1);
				phenotype = curCell.getStringCellValue().trim();
				//ex) CYP2C19 Intermediate metabolizer -> Intermediate metabolizer (IM)
				phenotype = phenotype.replace(gene,  "");
				phenotype = phenotype.trim();
				phenotype = addAbbreviation(phenotype);

				curCell = curRow.getCell(2);
				phenotypeDescription = curCell.getStringCellValue().trim();

				String key = gene + ":" + phenotype;

				phenotypeDescriptionMap.put(key,  phenotypeDescription);

				//System.out.println(String.format("(%s, %s)\n", phenotype, phenotypeDescription));

			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}


	private void readInputSettings() {
		String fileName = inputSettingsPath;
		inputColumnMap.clear();
		File file = new File(fileName);
		String cellValue = "";
		double numCellValue = 0;
		try (XSSFWorkbook workbook = new XSSFWorkbook(file);){
			XSSFSheet curSheet;
			XSSFRow curRow;
			XSSFCell curCell;
			curSheet = workbook.getSheetAt(1);
			curRow = curSheet.getRow(2);
			curCell = curRow.getCell(1);
			numCellValue = curCell.getNumericCellValue();
			inputColumnMap.put("Header row", (int)numCellValue-1);
						
			for(int i=4;i<curSheet.getPhysicalNumberOfRows();i++) {
				curRow = curSheet.getRow(i);
				curCell = curRow.getCell(0);
				cellValue = curCell.getStringCellValue().trim();
				//System.out.print(cellValue + ", ");
				curCell = curRow.getCell(1);
				numCellValue = curCell.getNumericCellValue();
				//System.out.println(numCellValue);
				inputColumnMap.put(cellValue, (int)numCellValue-1);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}

	}

	
	
	public static String getRowAsTabSeparatedString(XSSFRow curRow) {
		if (curRow == null) return "";

		StringBuilder sb = new StringBuilder();
		DataFormatter formatter = new DataFormatter();

		int lastCellNum = curRow.getLastCellNum(); // last cell index + 1

		for (int i = 0; i < lastCellNum; i++) {
			Cell cell = curRow.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			String cellValue = "";
			if (cell != null) {
				cellValue = formatter.formatCellValue(cell);
			}

			sb.append(cellValue);
			if (i < lastCellNum - 1) {
				sb.append("\t");
			}
		}
		return sb.toString();
	}


	public void readSettings() {
		Vector<String> v_lines = new Vector<String>();
		String fileName = resourcePath + "/settings.xlsx";
		File file = new File(fileName);
		try (XSSFWorkbook workbook = new XSSFWorkbook(file);){
			XSSFSheet curSheet;
			XSSFRow curRow;
			curSheet = workbook.getSheetAt(0);
			for(int i=0;i<curSheet.getPhysicalNumberOfRows();i++) {
				curRow = curSheet.getRow(i);
				v_lines.add(getRowAsTabSeparatedString(curRow));
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}


		String[] lines = v_lines.toArray(new String[v_lines.size()]);
		
		Vector<String[]> panelInclusionList = new Vector<String[]>();
		HashMap <String, Integer> propertyMap = new HashMap<String, Integer>();

		int linePos=0;
		for(linePos=0;linePos<lines.length;linePos++) {
			String line = lines[linePos];
			if(line == null || line.trim().length()==0) {
				continue;
			}
			if(line.trim().equals("Tests")) 
				break;

			//System.out.println("linepos : " + linePos);
			String[] tokens = line.split("\t");
			propertyMap.put(tokens[0],  new Integer(linePos));

		}

		for(int i=linePos+1;i<lines.length;i++) {
			String line = lines[i];
			if(line == null || line.trim().length()==0) {
				continue;
			}

			String[] tokens = line.split("\t");
			panelNameList.add(tokens[0]);
			panelInclusionList.add(tokens);
		}


		String[] geneList = lines[propertyMap.get("Gene").intValue()].split("\t");
		String[] usedList = lines[propertyMap.get("Used").intValue()].split("\t");
		String[] chromosomeList = lines[propertyMap.get("chromosome").intValue()].split("\t");
		String[] transcriptList = lines[propertyMap.get("transcript").intValue()].split("\t");
		String[] GRCh38RowList = lines[propertyMap.get("GRCh38Row").intValue()].split("\t");
		String[] referenceRowList = lines[propertyMap.get("ReferenceRow").intValue()].split("\t");
		// Table-layout columns (diplotype/haplotype phenotype, activity score and
		// frequency columns/sheets) are no longer configured here; they are detected
		// by header label inside GeneMetaData so freshly downloaded ClinPGx tables
		// work without manual re-mapping. settings.xlsx only carries product config.
		geneMetaDataList = new Vector<GeneMetaData>();
		for(int i=1;i<geneList.length;i++) {
			if(usedList[i].equals("No"))
				continue;
			int GRCh38Row=0, referenceRow=0;

			try {
				GRCh38Row = Integer.parseInt(GRCh38RowList[i]);
				referenceRow = Integer.parseInt(referenceRowList[i]);

				boolean[] b_panelInclusionList = new boolean[panelNameList.size()];
				for(int j=0;j<panelNameList.size();j++) {
					b_panelInclusionList[j] = numToBool(panelInclusionList.get(j)[i]);
				}

				GeneMetaData gene = new GeneMetaData(geneList[i], chromosomeList[i], transcriptList[i], GRCh38Row, referenceRow,
						b_panelInclusionList);
				geneMetaDataList.add(gene);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				popUp("Only integers are acceptable for GRCh38Row and referenceRow in settings.txt");
			}
		}

	}

	public void readComments() {
		for (String panelName : panelNameList) {
			String comment = readFile(resourcePath + "/comments/" + panelName + ".txt");
			if(comment != null)
				comment = comment.replace("{VERSION}", version).replace("{DBVERSION}", DBversion);
			commentMap.put(panelName,  comment);
		}
	}

	/**
	 * Initializes required settings
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		File tempFile = new File(lastVisitedDir);
		if(!tempFile.exists())
			lastVisitedDir=".";

		sampleListView.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				selectedSample = newValue.intValue();
				fillResults();
			}
		});

		techNameField1.setText(techName1);
		techNameField2.setText(techName2);
		doctorNameField1.setText(doctorName1);
		doctorNameField2.setText(doctorName2);
		doctorNameField3.setText(doctorName3);


		variantRadio.setToggleGroup(radioToggleGroup);
		variantRadio.setSelected(true);
		diplotypeRadio.setToggleGroup(radioToggleGroup);
		reportRadio.setToggleGroup(radioToggleGroup);
		batchReportRadio.setToggleGroup(radioToggleGroup);
		batchVariantRadio.setToggleGroup(radioToggleGroup);

		variantRadio.setUserData("variant");
		diplotypeRadio.setUserData("diplotype");
		reportRadio.setUserData("report");
		batchReportRadio.setUserData("batch");
		batchVariantRadio.setUserData("batch_variant");

		radioToggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>(){
			public void changed(ObservableValue<? extends Toggle> ov,
					Toggle old_toggle, Toggle new_toggle) {
				if (radioToggleGroup.getSelectedToggle() != null) {
					String t1 = (String)radioToggleGroup.getSelectedToggle().getUserData();
					if(t1.equals("variant")) {
						context = Context.VARIANT;
					}
					else if (t1.equals("diplotype")) { 
						context = Context.DIPLOTYPE;
					}
					else if (t1.equals("report")) { 
						context = Context.REPORT;
					}

					else if (t1.equals("batch")) { 
						context = Context.BATCH_REPORT;
					}

					else if (t1.equals("batch_variant")) { 
						context = Context.BATCH_VARIANT;
					}


					if(sampleArray!=null && sampleArray.length>0) {	
						fillResults();
					}
				}                
			}
		});

		readPhenotypeTable();
		readSettings();
		readInputSettings();
		readComments();
	}

	public void handleReflectAll() {
		techName1 = techNameField1.getText();
		techName2 = techNameField2.getText();
		doctorName1 = doctorNameField1.getText();
		doctorName2 = doctorNameField2.getText();
		doctorName3 = doctorNameField3.getText();
		fillResults();
	}

	private void makeBatchReport() {
		batchReport = "Sample ID\t";

		for(GeneMetaData geneMeta : geneMetaDataList) {
			batchReport += String.format("%s\t",  geneMeta.name);
		}
		batchReport += "\n";

		for(Sample sample : sampleArray) {
			batchReport += sample.getBatchReport();
			batchReport += "\n";
		}
	}



	public String getReaderString() {
		String panelName = panelNameList.get(selectedPanel);

		String ret = String.format("\n\n                      - Examiner: %-3s M.T./%-3s M.T.\n"
				+ "                      - Reporting physician: %-3s M.D./%-3s M.D./%-3s M.D.", techName1, techName2, doctorName1, doctorName2, doctorName3);


		return ret;
	}


	public void fillResults() {
		//System.out.println("Fill result called");
		//Called when the SampleListView is initialized and selectedSample changes to -1.. ignore..
		if(selectedSample == -1) 
			return;
		Sample sample = sampleArray[selectedSample];
		TextArea reportArea = new TextArea();
		reportArea.setMinHeight(1670);
		//System.out.println(Font.getFontNames());
		// Monospaced font so the fixed-width report tables align correctly.
		reportArea.setFont(Font.font("Consolas", 12));
		vBox.getChildren().clear();
		switch (context) {
		case VARIANT : 
			for(int i=0;i<geneMetaDataList.size();i++) {
				if(geneMetaDataList.get(i).targeted) {
					Label geneLabel = new Label(geneMetaDataList.get(i).name + " (" + geneMetaDataList.get(i).transcript + ")");
					geneLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-weight: bold; -fx-font-size: 16px;");
					vBox.getChildren().add(geneLabel);
					GeneContent geneContent = sample.geneContentArray[i];
					HBox hBox = new HBox();
					hBox.setAlignment(Pos.CENTER_RIGHT);
					Button excludeButton = new Button ("Exclude selected variant");
					Button includeButton = new Button ("Include selected variant");
					hBox.getChildren().add(excludeButton);
					hBox.getChildren().add(new Label("   "));
					hBox.getChildren().add(includeButton);
					vBox.getChildren().add(hBox);
					TableView<Variant> variantTable = new TableView<Variant>();
					TableColumn<Variant, String> tcIncluded = new TableColumn<Variant, String>("Included");
					tcIncluded.setMinWidth(60);
					TableColumn<Variant, String> tcIndexId = new TableColumn<Variant, String>("IndexID");
					tcIndexId.setMinWidth(200);
					TableColumn<Variant, String> tcRsId = new TableColumn<Variant, String>("rsID");
					tcIndexId.setMinWidth(200);
					TableColumn<Variant, String> tcNTchange = new TableColumn<Variant, String>("NTchange");
					tcNTchange.setMinWidth(150);
					TableColumn<Variant, String> tcAAchange= new TableColumn<Variant, String>("AAchange");
					tcAAchange.setMinWidth(150);
					TableColumn<Variant, String> tcZygosity = new TableColumn<Variant, String>("Zygosity");
					tcZygosity.setMinWidth(70);
					TableColumn<Variant, String> tcVAF = new TableColumn<Variant, String>("VAF");
					tcVAF.setMinWidth(50);
					TableColumn<Variant, String> tcDepth = new TableColumn<Variant, String>("Depth");
					tcDepth.setMinWidth(50);
					TableColumn<Variant, String> tcGQ = new TableColumn<Variant, String>("GQ");
					tcGQ.setMinWidth(50);
					TableColumn<Variant, String> tcStrandBias = new TableColumn<Variant, String>("StrandBias");
					tcStrandBias.setMinWidth(70);
					TableColumn<Variant, String> tcQUAL = new TableColumn<Variant, String>("QUAL");
					tcQUAL.setMinWidth(50);

					TableColumn<Variant, String> tcAlleleFreq = new TableColumn<Variant, String>("AlleleFreq");
					tcAlleleFreq.setMinWidth(50);

					TableColumn<Variant, String> tcStarAlleles = new TableColumn<Variant, String>("StarAlleles");
					tcStarAlleles.setMinWidth(130);


					variantTable.getColumns().setAll(tcIncluded, tcIndexId, tcRsId, tcNTchange, tcAAchange, tcZygosity, tcVAF, tcDepth, tcGQ, tcStrandBias, tcQUAL, tcAlleleFreq, tcStarAlleles);

					tcIncluded.setCellValueFactory(new PropertyValueFactory("IncludedProperty"));
					tcIndexId.setCellValueFactory(new PropertyValueFactory("indexIdProperty"));
					tcRsId.setCellValueFactory(new PropertyValueFactory("rsIdProperty"));
					tcNTchange.setCellValueFactory(new PropertyValueFactory("NTchangeProperty"));
					tcAAchange.setCellValueFactory(new PropertyValueFactory("AAchangeProperty"));
					tcZygosity.setCellValueFactory(new PropertyValueFactory("ZygosityProperty"));
					tcVAF.setCellValueFactory(new PropertyValueFactory("VAFProperty"));
					tcDepth.setCellValueFactory(new PropertyValueFactory("DepthProperty"));
					tcGQ.setCellValueFactory(new PropertyValueFactory("GQProperty"));
					tcStrandBias.setCellValueFactory(new PropertyValueFactory("StrandBiasProperty"));
					tcQUAL.setCellValueFactory(new PropertyValueFactory("QUALProperty"));
					tcAlleleFreq.setCellValueFactory(new PropertyValueFactory("AlleleFreqProperty"));
					tcStarAlleles.setCellValueFactory(new PropertyValueFactory("StarAllelesProperty"));

					//editable; low-QUAL variants (QUAL < Variant.QUAL_red_threshold)
					//are rendered in red font across every column.
					variantTable.setEditable(true);
					tcIncluded.setCellFactory(lowQualDisplayCellFactory());
					tcIndexId.setCellFactory(lowQualCellFactory());
					tcRsId.setCellFactory(lowQualCellFactory());
					tcNTchange.setCellFactory(lowQualCellFactory());
					tcAAchange.setCellFactory(lowQualCellFactory());
					tcZygosity.setCellFactory(lowQualCellFactory());
					tcVAF.setCellFactory(lowQualCellFactory());
					tcDepth.setCellFactory(lowQualCellFactory());
					tcGQ.setCellFactory(lowQualCellFactory());
					tcStrandBias.setCellFactory(lowQualCellFactory());
					tcQUAL.setCellFactory(lowQualCellFactory());
					tcAlleleFreq.setCellFactory(lowQualCellFactory());
					tcStarAlleles.setCellFactory(lowQualCellFactory());

					// Show included ("O") variants first; stable sort preserves the
					// original genomic order within each group. Sorting the backing
					// list (not just the view) keeps row indices aligned with the
					// include/exclude button handlers below.
					geneContent.variantList.sort((a, b) ->
							Integer.compare(a.included.equals("O") ? 0 : 1, b.included.equals("O") ? 0 : 1));
					ObservableList<Variant> observableList= FXCollections.observableArrayList(geneContent.variantList);
					//observableList.sort(Variant::compareTo);
					variantTable.setItems(observableList);

					variantTable.setFixedCellSize(25);
					variantTable.prefHeightProperty().bind(variantTable.fixedCellSizeProperty().multiply(Bindings.size(variantTable.getItems()).add(1.01)));
					variantTable.minHeightProperty().bind(variantTable.prefHeightProperty());
					variantTable.maxHeightProperty().bind(variantTable.prefHeightProperty());

					vBox.getChildren().add(variantTable);
					vBox.getChildren().add(new Label("  "));
					vBox.getChildren().add(new Label("  "));

					final int geneIndex = i;
					excludeButton.setOnAction(event -> {
						int selectedIndex = variantTable.getSelectionModel().getSelectedIndex();
						if(selectedIndex<0) return;
						geneContent.variantList.get(selectedIndex).included = "X";
						geneContent.updateIncludedVariantList();
						sample.diplotyping(geneIndex, true);
						variantTable.getItems().get(selectedIndex).setIncludedProperty("X");
						variantTable.refresh();
					});

					includeButton.setOnAction(event -> {
						int selectedIndex = variantTable.getSelectionModel().getSelectedIndex();
						if(selectedIndex<0) return;
						geneContent.variantList.get(selectedIndex).included = "O";
						geneContent.updateIncludedVariantList();
						sample.diplotyping(geneIndex, true);
						variantTable.getItems().get(selectedIndex).setIncludedProperty("O");
						variantTable.refresh();
					});
				}
			}

			break;
		case DIPLOTYPE :
			for(int i=0;i<geneMetaDataList.size();i++) {
				if(geneMetaDataList.get(i).targeted) {
					Label geneLabel = new Label(geneMetaDataList.get(i).name + " (" + geneMetaDataList.get(i).transcript + ")");
					geneLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-weight: bold; -fx-font-size: 16px;");
					vBox.getChildren().add(geneLabel);
					GeneContent geneContent = sample.geneContentArray[i];
					HBox hBox = new HBox();
					hBox.setAlignment(Pos.CENTER_RIGHT);
					Button excludeButton = new Button ("Exclude selected diplotype");
					Button includeButton = new Button ("Include selected diplotype");
					hBox.getChildren().add(excludeButton);
					hBox.getChildren().add(new Label("   "));
					hBox.getChildren().add(includeButton);
					vBox.getChildren().add(hBox);

					TableView<Diplotype> diplotypeTable = new TableView<Diplotype>();
					TableColumn<Diplotype, String> tcIncluded = new TableColumn<Diplotype, String>("Included");
					tcIncluded.setMinWidth(60);
					TableColumn<Diplotype, String> tcName = new TableColumn<Diplotype, String>("Diplotype");
					tcName.setMinWidth(80);
					TableColumn<Diplotype, String> tcScore = new TableColumn<Diplotype, String>("Score");
					tcScore.setMinWidth(60);
					TableColumn<Diplotype, String> tcFrequency = new TableColumn<Diplotype, String>("Frequency");
					tcFrequency.setMinWidth(80);
					TableColumn<Diplotype, String> tcActivityScore = new TableColumn<Diplotype, String>("Activity Score");
					tcActivityScore.setMinWidth(100);
					TableColumn<Diplotype, String> tcPhenotype = new TableColumn<Diplotype, String>("Phenotype");
					tcPhenotype.setMinWidth(80);

					TableColumn<Diplotype, String> tcHaplotype1Name = new TableColumn<Diplotype, String>("Haplotype 1");
					tcFrequency.setMinWidth(80);
					TableColumn<Diplotype, String> tcHaplotype1Frequency = new TableColumn<Diplotype, String>("Frequency");
					tcFrequency.setMinWidth(80);
					TableColumn<Diplotype, String> tcHaplotype1ActivityScore = new TableColumn<Diplotype, String>("Activity Score");
					tcActivityScore.setMinWidth(100);
					TableColumn<Diplotype, String> tcHaplotype1Phenotype = new TableColumn<Diplotype, String>("Phenotype");
					tcPhenotype.setMinWidth(80);

					TableColumn<Diplotype, String> tcHaplotype2Name = new TableColumn<Diplotype, String>("Haplotype 2");
					tcFrequency.setMinWidth(80);
					TableColumn<Diplotype, String> tcHaplotype2Frequency = new TableColumn<Diplotype, String>("Frequency");
					tcFrequency.setMinWidth(80);
					TableColumn<Diplotype, String> tcHaplotype2ActivityScore = new TableColumn<Diplotype, String>("Activity Score");
					tcActivityScore.setMinWidth(100);
					TableColumn<Diplotype, String> tcHaplotype2Phenotype = new TableColumn<Diplotype, String>("Phenotype");
					tcPhenotype.setMinWidth(80);

					//TableColumn<Diplotype, String> tcVariants = new TableColumn<Diplotype, String>("Variants");
					//tcVariants.setMinWidth(730);

					diplotypeTable.getColumns().setAll(tcIncluded, tcName, tcScore, tcFrequency, tcActivityScore, tcPhenotype, 
							tcHaplotype1Name, tcHaplotype1Frequency, tcHaplotype1ActivityScore, tcHaplotype1Phenotype, 
							tcHaplotype2Name, tcHaplotype2Frequency, tcHaplotype2ActivityScore, tcHaplotype2Phenotype
							);

					tcIncluded.setCellValueFactory(new PropertyValueFactory("IncludedProperty"));
					tcName.setCellValueFactory(new PropertyValueFactory("NameProperty"));
					tcScore.setCellValueFactory(new PropertyValueFactory("ScoreProperty"));
					tcFrequency.setCellValueFactory(new PropertyValueFactory("DiplotypeFrequencyProperty"));
					tcActivityScore.setCellValueFactory(new PropertyValueFactory("DiplotypeActivityScoreProperty"));
					tcPhenotype.setCellValueFactory(new PropertyValueFactory("DiplotypePhenotypeProperty"));

					tcHaplotype1Name.setCellValueFactory(new PropertyValueFactory("Haplotype1NameProperty"));
					tcHaplotype1Frequency.setCellValueFactory(new PropertyValueFactory("Haplotype1FrequencyProperty"));
					tcHaplotype1ActivityScore.setCellValueFactory(new PropertyValueFactory("Haplotype1ActivityScoreProperty"));
					tcHaplotype1Phenotype.setCellValueFactory(new PropertyValueFactory("Haplotype1PhenotypeProperty"));

					tcHaplotype2Name.setCellValueFactory(new PropertyValueFactory("Haplotype2NameProperty"));
					tcHaplotype2Frequency.setCellValueFactory(new PropertyValueFactory("Haplotype2FrequencyProperty"));
					tcHaplotype2ActivityScore.setCellValueFactory(new PropertyValueFactory("Haplotype2ActivityScoreProperty"));
					tcHaplotype2Phenotype.setCellValueFactory(new PropertyValueFactory("Haplotype2PhenotypeProperty"));

					//tcVariants.setCellValueFactory(new PropertyValueFactory("VariantsProperty"));

					//editable
					diplotypeTable.setEditable(true);
					tcName.setCellFactory(TextFieldTableCell.<Diplotype>forTableColumn());
					tcScore.setCellFactory(TextFieldTableCell.<Diplotype>forTableColumn());
					tcFrequency.setCellFactory(TextFieldTableCell.<Diplotype>forTableColumn());
					tcActivityScore.setCellFactory(TextFieldTableCell.<Diplotype>forTableColumn());
					tcPhenotype.setCellFactory(TextFieldTableCell.<Diplotype>forTableColumn());
					//tcVariants.setCellFactory(TextFieldTableCell.<Diplotype>forTableColumn());

					ObservableList<Diplotype> observableList= FXCollections.observableArrayList(geneContent.diplotypeList);
					//observableList.sort(Variant::compareTo);
					diplotypeTable.setItems(observableList);
					diplotypeTable.setFixedCellSize(25);
					diplotypeTable.prefHeightProperty().bind(diplotypeTable.fixedCellSizeProperty().multiply(Bindings.size(diplotypeTable.getItems()).add(1.01)));
					diplotypeTable.minHeightProperty().bind(diplotypeTable.prefHeightProperty());
					diplotypeTable.maxHeightProperty().bind(diplotypeTable.prefHeightProperty());

					vBox.getChildren().add(diplotypeTable);
					vBox.getChildren().add(new Label("  "));
					vBox.getChildren().add(new Label("  "));

					excludeButton.setOnAction(event -> {
						int selectedIndex = diplotypeTable.getSelectionModel().getSelectedIndex();
						if(selectedIndex<0) return;
						geneContent.diplotypeList.get(selectedIndex).included = "X";
						diplotypeTable.getItems().get(selectedIndex).setIncludedProperty("X");
						diplotypeTable.refresh();
					});

					includeButton.setOnAction(event -> {
						int selectedIndex = diplotypeTable.getSelectionModel().getSelectedIndex();
						if(selectedIndex<0) return;
						geneContent.diplotypeList.get(selectedIndex).included = "O";
						diplotypeTable.getItems().get(selectedIndex).setIncludedProperty("O");
						diplotypeTable.refresh();
					});
				}
			}
			break;
		case REPORT : 
			String panelName = panelNameList.get(selectedPanel);
			if(panelName.equals("UGT1A1 DPYD")) {
				String comment = commentMap.get(panelName);
				int markerIdx = comment.indexOf("▣ TEST INFORMATION AND FINDINGS");
				String comment1 = markerIdx >= 0 ? comment.substring(0, markerIdx) : comment;
				String comment2 = markerIdx >= 0 ? comment.substring(markerIdx) : "";
				reportArea = new TextArea(sample.getReport("UGT1A1") + comment1 + comment2 + sample.getReport("DPYD_short") + getReaderString());
			}

			else
				reportArea = new TextArea(sample.getReport(panelName) + commentMap.get(panelName) + getReaderString());

			reportArea.setFont(Font.font("Consolas", 12));
			reportArea.setMinHeight(800);
			vBox.getChildren().add(reportArea);
			break;

		case BATCH_REPORT :
			makeBatchReport();
			reportArea = new TextArea(batchReport);
			reportArea.setFont(Font.font("Consolas", 12));
			reportArea.setMinHeight(800);
			vBox.getChildren().add(reportArea);
			break;

		case BATCH_VARIANT :
			for(int i=0;i<geneMetaDataList.size();i++) {
				if(geneMetaDataList.get(i).targeted) {
					Label geneLabel = new Label(geneMetaDataList.get(i).name + " (" + geneMetaDataList.get(i).transcript + ")");
					geneLabel.setStyle("-fx-font-family: 'Verdana'; -fx-font-weight: bold; -fx-font-size: 20px;");
					vBox.getChildren().add(geneLabel);

					Vector<BatchVariant> batchVariantList = new Vector<BatchVariant>();
					for(Sample batchSample : sampleArray) {
						GeneContent geneContent = batchSample.geneContentArray[i];
						for(Variant variant : geneContent.variantList) {
							BatchVariant bv = new BatchVariant(variant, batchSample.sampleID);
							batchVariantList.add(bv);
						}

					}

					TableView<BatchVariant> variantTable = new TableView<BatchVariant>();
					TableColumn<BatchVariant, String> tcSampleID = new TableColumn<BatchVariant, String>("SampleID");
					tcSampleID.setMinWidth(60);
					TableColumn<BatchVariant, String> tcIndexId = new TableColumn<BatchVariant, String>("IndexID");
					tcIndexId.setMinWidth(200);
					TableColumn<BatchVariant, String> tcRsId = new TableColumn<BatchVariant, String>("rsID");
					tcIndexId.setMinWidth(200);
					TableColumn<BatchVariant, String> tcNTchange = new TableColumn<BatchVariant, String>("NTchange");
					tcNTchange.setMinWidth(150);
					TableColumn<BatchVariant, String> tcAAchange= new TableColumn<BatchVariant, String>("AAchange");
					tcAAchange.setMinWidth(150);
					TableColumn<BatchVariant, String> tcZygosity = new TableColumn<BatchVariant, String>("Zygosity");
					tcZygosity.setMinWidth(70);
					TableColumn<BatchVariant, String> tcVAF = new TableColumn<BatchVariant, String>("VAF");
					tcVAF.setMinWidth(50);
					TableColumn<BatchVariant, String> tcDepth = new TableColumn<BatchVariant, String>("Depth");
					tcDepth.setMinWidth(50);
					TableColumn<BatchVariant, String> tcGQ = new TableColumn<BatchVariant, String>("GQ");
					tcGQ.setMinWidth(50);
					TableColumn<BatchVariant, String> tcStrandBias = new TableColumn<BatchVariant, String>("StrandBias");
					tcStrandBias.setMinWidth(70);
					TableColumn<BatchVariant, String> tcQUAL = new TableColumn<BatchVariant, String>("QUAL");
					tcQUAL.setMinWidth(50);

					TableColumn<BatchVariant, String> tcAlleleFreq = new TableColumn<BatchVariant, String>("AlleleFreq");
					tcAlleleFreq.setMinWidth(50);

					TableColumn<BatchVariant, String> tcStarAlleles = new TableColumn<BatchVariant, String>("StarAlleles");
					tcStarAlleles.setMinWidth(130);


					variantTable.getColumns().setAll(tcSampleID, tcIndexId, tcRsId, tcNTchange, tcAAchange, tcZygosity, tcVAF, tcDepth, tcGQ, tcStrandBias, tcQUAL, tcAlleleFreq, tcStarAlleles);

					tcSampleID.setCellValueFactory(new PropertyValueFactory("SampleIDProperty"));
					tcIndexId.setCellValueFactory(new PropertyValueFactory("indexIdProperty"));
					tcRsId.setCellValueFactory(new PropertyValueFactory("rsIdProperty"));
					tcNTchange.setCellValueFactory(new PropertyValueFactory("NTchangeProperty"));
					tcAAchange.setCellValueFactory(new PropertyValueFactory("AAchangeProperty"));
					tcZygosity.setCellValueFactory(new PropertyValueFactory("ZygosityProperty"));
					tcVAF.setCellValueFactory(new PropertyValueFactory("VAFProperty"));
					tcDepth.setCellValueFactory(new PropertyValueFactory("DepthProperty"));
					tcGQ.setCellValueFactory(new PropertyValueFactory("GQProperty"));
					tcStrandBias.setCellValueFactory(new PropertyValueFactory("StrandBiasProperty"));
					tcQUAL.setCellValueFactory(new PropertyValueFactory("QUALProperty"));
					tcAlleleFreq.setCellValueFactory(new PropertyValueFactory("AlleleFreqProperty"));
					tcStarAlleles.setCellValueFactory(new PropertyValueFactory("StarAllelesProperty"));

					//editable
					variantTable.setEditable(true);
					tcIndexId.setCellFactory(TextFieldTableCell.<BatchVariant>forTableColumn());
					tcRsId.setCellFactory(TextFieldTableCell.<BatchVariant>forTableColumn());
					tcNTchange.setCellFactory(TextFieldTableCell.<BatchVariant>forTableColumn());
					tcAAchange.setCellFactory(TextFieldTableCell.<BatchVariant>forTableColumn());
					tcZygosity.setCellFactory(TextFieldTableCell.<BatchVariant>forTableColumn());
					tcVAF.setCellFactory(TextFieldTableCell.<BatchVariant>forTableColumn());
					tcDepth.setCellFactory(TextFieldTableCell.<BatchVariant>forTableColumn());
					tcGQ.setCellFactory(TextFieldTableCell.<BatchVariant>forTableColumn());
					tcStrandBias.setCellFactory(TextFieldTableCell.<BatchVariant>forTableColumn());
					tcQUAL.setCellFactory(TextFieldTableCell.<BatchVariant>forTableColumn());
					tcAlleleFreq.setCellFactory(TextFieldTableCell.<BatchVariant>forTableColumn());
					tcStarAlleles.setCellFactory(TextFieldTableCell.<BatchVariant>forTableColumn());

					ObservableList<BatchVariant> observableList= FXCollections.observableArrayList(batchVariantList);
					//observableList.sort(Variant::compareTo);
					variantTable.setItems(observableList);

					variantTable.setFixedCellSize(25);
					variantTable.prefHeightProperty().bind(variantTable.fixedCellSizeProperty().multiply(Bindings.size(variantTable.getItems()).add(1.01)));
					variantTable.minHeightProperty().bind(variantTable.prefHeightProperty());
					variantTable.maxHeightProperty().bind(variantTable.prefHeightProperty());

					vBox.getChildren().add(variantTable);
					vBox.getChildren().add(new Label("  "));
					vBox.getChildren().add(new Label("  "));

					final int geneIndex = i;
				}
			}
		}


	}

	public void handleTargetGenes() {
		//System.out.println(fwdFileHandler.fileUploader.getUploadedFile().getAbsolutePath());
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("targetGenes.fxml"));
			Parent root1 = (Parent) fxmlLoader.load();
			Stage stage = new Stage();
			TargetGeneController controller = fxmlLoader.getController();
			controller.showGenes(this, stage);
			stage.setScene(Ui.scene(root1));
			stage.setTitle("Target Test Selection");
			//stage.setAlwaysOnTop(true);
			stage.initOwner(primaryStage);
			stage.show();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return;
		}


	}

	/**
	 * Re-reads every currently loaded sample from its source file so that any
	 * change to quality cutoffs (Variant static fields) or to the input column
	 * mapping (inputColumnMap) takes effect immediately.
	 */
	private void reloadSamples() {
		if(sampleArray == null || sampleArray.length == 0)
			return;

		Vector<Sample> tempList = new Vector<Sample>();
		Vector<String> idList = new Vector<String>();
		inputTypeError = false;
		for(Sample old : sampleArray) {
			Sample sample = new Sample(old.sampleID, old.path, this);
			tempList.add(sample);
			idList.add(old.sampleID);
		}

		if(inputTypeError) {
			popUp("Please check input file type (Settings -> Input Parameters)");
			return;
		}

		sampleArray = tempList.toArray(new Sample[tempList.size()]);
		sampleListView.setItems(FXCollections.observableArrayList(idList));
		selectedSample = sampleArray.length > 0 ? 0 : -1;
		fillResults();
	}

	/**
	 * Editable variants-table cell that renders its entire row in red font when the
	 * row's variant is a lower-confidence call (QUAL &lt; Variant.QUAL_red_threshold).
	 * Editing behaviour is identical to TextFieldTableCell.forTableColumn().
	 */
	private Callback<TableColumn<Variant, String>, TableCell<Variant, String>> lowQualCellFactory() {
		return col -> new TextFieldTableCell<Variant, String>(new DefaultStringConverter()) {
			@Override
			public void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				Variant v = (getTableRow() == null) ? null : (Variant) getTableRow().getItem();
				if (!empty && v != null && v.isLowQual())
					setStyle("-fx-text-fill: #d00000;");
				else
					setStyle("");
			}
		};
	}

	/**
	 * Read-only counterpart of {@link #lowQualCellFactory()} for non-editable
	 * columns (e.g. the include/exclude marker), keeping the red low-QUAL styling.
	 */
	private Callback<TableColumn<Variant, String>, TableCell<Variant, String>> lowQualDisplayCellFactory() {
		return col -> new TableCell<Variant, String>() {
			@Override
			public void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty ? null : item);
				Variant v = (getTableRow() == null) ? null : (Variant) getTableRow().getItem();
				if (!empty && v != null && v.isLowQual())
					setStyle("-fx-text-fill: #d00000;");
				else
					setStyle("");
			}
		};
	}

	/**
	 * Settings > Quality Parameters.
	 * Opens a panel where every variant-filter cutoff is separately adjustable.
	 * A single QUAL cutoff is applied to every variant. A separate (display-only)
	 * red-flag threshold marks lower-confidence calls in red font in the variants
	 * table without affecting filtering.
	 */
	public void handleQualityParameters() {
		Stage dialog = new Stage(StageStyle.DECORATED);
		dialog.initOwner(primaryStage);
		dialog.initModality(Modality.WINDOW_MODAL);
		dialog.setTitle("Quality Parameters");

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(8);
		grid.setPadding(new Insets(15));

		TextField qualField = new TextField();
		TextField qualRedField = new TextField();
		TextField depthField = new TextField();
		TextField heteroVafField = new TextField();
		TextField homoVafField = new TextField();

		grid.add(new Label("QUAL cutoff (>=)"), 0, 0);
		grid.add(qualField, 1, 0);
		grid.add(new Label("QUAL red-flag threshold (red if <)"), 0, 1);
		grid.add(qualRedField, 1, 1);
		grid.add(new Label("Depth cutoff (>=)"), 0, 2);
		grid.add(depthField, 1, 2);
		grid.add(new Label("Hetero VAF % (>=)"), 0, 3);
		grid.add(heteroVafField, 1, 3);
		grid.add(new Label("Homo VAF % (>=)"), 0, 4);
		grid.add(homoVafField, 1, 4);

		// Fill fields from the current global cutoffs.
		Runnable refresh = () -> {
			qualField.setText(String.valueOf(Variant.QUAL_cutoff));
			qualRedField.setText(String.valueOf(Variant.QUAL_red_threshold));
			depthField.setText(String.valueOf(Variant.depth_cutoff));
			heteroVafField.setText(String.valueOf(Variant.hetero_VAF));
			homoVafField.setText(String.valueOf(Variant.homo_VAF));
		};
		refresh.run();

		Button applyButton = new Button("Apply");
		Button cancelButton = new Button("Cancel");
		HBox buttonBox = new HBox(10, applyButton, cancelButton);
		buttonBox.setAlignment(Pos.CENTER_RIGHT);
		grid.add(buttonBox, 0, 5, 2, 1);

		applyButton.setOnAction(e -> {
			try {
				double qual = Double.parseDouble(qualField.getText().trim());
				double qualRed = Double.parseDouble(qualRedField.getText().trim());
				double depth = Double.parseDouble(depthField.getText().trim());
				double heteroVaf = Double.parseDouble(heteroVafField.getText().trim());
				double homoVaf = Double.parseDouble(homoVafField.getText().trim());

				Variant.QUAL_cutoff = qual;
				Variant.QUAL_red_threshold = qualRed;
				Variant.depth_cutoff = depth;
				Variant.hetero_VAF = heteroVaf;
				Variant.homo_VAF = homoVaf;

				dialog.close();
				reloadSamples();
			}
			catch (NumberFormatException ex) {
				popUp("All quality parameters must be numeric values.");
			}
		});
		cancelButton.setOnAction(e -> dialog.close());

		dialog.setScene(Ui.scene(grid));
		dialog.setResizable(false);
		dialog.show();
	}

	/**
	 * Settings > Ethnicity.
	 * Lets the user pick which of the 9 ClinPGx biogeographic groups supplies the
	 * allele/diplotype frequencies. Frequencies are resolved by column header at
	 * load time, so changing the group re-reads the ClinPGx frequency tables.
	 * Genes lacking the chosen group's column simply show blank/N-A.
	 */
	public void handleEthnicity() {
		Stage dialog = new Stage(StageStyle.DECORATED);
		dialog.initOwner(primaryStage);
		dialog.initModality(Modality.WINDOW_MODAL);
		dialog.setTitle("Ethnicity");

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(8);
		grid.setPadding(new Insets(15));

		ComboBox<String> ethnicityBox = new ComboBox<String>();
		ethnicityBox.getItems().addAll(ethnicityList);
		ethnicityBox.setValue(selectedEthnicity);

		grid.add(new Label("Biogeographic group (ClinPGx)"), 0, 0);
		grid.add(ethnicityBox, 1, 0);

		Button applyButton = new Button("Apply");
		Button cancelButton = new Button("Cancel");
		HBox buttonBox = new HBox(10, applyButton, cancelButton);
		buttonBox.setAlignment(Pos.CENTER_RIGHT);
		grid.add(buttonBox, 0, 1, 2, 1);

		applyButton.setOnAction(e -> {
			String chosen = ethnicityBox.getValue();
			if(chosen == null || chosen.length() == 0) {
				popUp("Please select a biogeographic group.");
				return;
			}
			selectedEthnicity = chosen;
			// Frequencies are read during GeneMetaData construction, so rebuild the
			// gene metadata (re-reads the frequency tables) before re-rendering.
			readSettings();
			dialog.close();
			reloadSamples();
		});
		cancelButton.setOnAction(e -> dialog.close());

		dialog.setScene(Ui.scene(grid));
		dialog.setResizable(false);
		dialog.show();
	}

	/**
	 * Settings > Input Parameters.
	 * Selects which settings workbook supplies the input column mapping (sheet 1)
	 * and reloads it. Default = settings.xlsx, WGS = settings_WGS.xlsx, or any
	 * user-chosen .xlsx via Browse.
	 */
	public void handleInputParameters() {
		Stage dialog = new Stage(StageStyle.DECORATED);
		dialog.initOwner(primaryStage);
		dialog.initModality(Modality.WINDOW_MODAL);
		dialog.setTitle("Input Parameters");

		VBox root = new VBox(10);
		root.setPadding(new Insets(15));

		String defaultPath = resourcePath + "/settings.xlsx";
		String wgsPath = resourcePath + "/settings_WGS.xlsx";

		ToggleGroup group = new ToggleGroup();
		RadioButton defaultRadio = new RadioButton("Default (settings.xlsx)");
		RadioButton wgsRadio = new RadioButton("WGS (settings_WGS.xlsx)");
		RadioButton customRadio = new RadioButton("Custom file");
		defaultRadio.setToggleGroup(group);
		wgsRadio.setToggleGroup(group);
		customRadio.setToggleGroup(group);

		Label customLabel = new Label("");
		final String[] customPath = { null };

		if(inputSettingsPath.equals(defaultPath))
			defaultRadio.setSelected(true);
		else if(inputSettingsPath.equals(wgsPath))
			wgsRadio.setSelected(true);
		else {
			customRadio.setSelected(true);
			customPath[0] = inputSettingsPath;
			customLabel.setText(inputSettingsPath);
		}

		Button browseButton = new Button("Browse...");
		browseButton.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Choose Input Settings File");
			fileChooser.getExtensionFilters().add(new ExtensionFilter("Excel Files", "*.xlsx"));
			File initDir = new File(resourcePath);
			if(initDir.exists())
				fileChooser.setInitialDirectory(initDir);
			File chosen = fileChooser.showOpenDialog(dialog);
			if(chosen != null) {
				customPath[0] = chosen.getAbsolutePath();
				customLabel.setText(customPath[0]);
				customRadio.setSelected(true);
			}
		});

		HBox customBox = new HBox(10, customRadio, browseButton, customLabel);
		customBox.setAlignment(Pos.CENTER_LEFT);

		Button reloadButton = new Button("Reload");
		Button cancelButton = new Button("Cancel");
		HBox buttonBox = new HBox(10, reloadButton, cancelButton);
		buttonBox.setAlignment(Pos.CENTER_RIGHT);

		root.getChildren().addAll(
				new Label("Input column-mapping file:"),
				defaultRadio, wgsRadio, customBox, buttonBox);

		reloadButton.setOnAction(e -> {
			String selectedPath;
			if(wgsRadio.isSelected())
				selectedPath = wgsPath;
			else if(customRadio.isSelected()) {
				if(customPath[0] == null) {
					popUp("Please choose a custom .xlsx file first.");
					return;
				}
				selectedPath = customPath[0];
			}
			else
				selectedPath = defaultPath;

			if(!new File(selectedPath).exists()) {
				popUp("Settings file not found:\n" + selectedPath);
				return;
			}

			inputSettingsPath = selectedPath;
			readInputSettings();
			dialog.close();
			reloadSamples();
		});
		cancelButton.setOnAction(e -> dialog.close());

		dialog.setScene(Ui.scene(root));
		dialog.setResizable(false);
		dialog.show();
	}

	/**
	 * Open forward trace file and opens trim.fxml with that file
	 */
	public void handleNewProject() {

		File tempFile2 = new File(lastVisitedDir);
		if(!tempFile2.exists())
			lastVisitedDir=".";

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Choose Annotation Files");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("Annotation Files", "*.txt"));
		fileChooser.setInitialDirectory(new File(lastVisitedDir));
		List<File> fileList = fileChooser.showOpenMultipleDialog(primaryStage);

		if (fileList==null || fileList.size()==0) return;
		lastVisitedDir=fileList.get(0).getParent();


		Vector<Sample> tempList = new Vector<Sample>();
		Vector<String> idList = new Vector<String>();

		inputTypeError = false;
		//sampleListView.setItems
		for(File file:fileList) {
			String fileName = file.getName();
			if(fileName.length()<9) continue;

			int sampleIdLength = 0;
			if(fileName.contains("."))
				sampleIdLength = fileName.indexOf(".");
			if(fileName.contains("_") && (fileName.indexOf(".") > fileName.indexOf("_")))
				sampleIdLength = fileName.indexOf("_");

			/*
			if(fileName.contains("_S")) 
					sampleIdLength = fileName.indexOf("_S");
			else if(fileName.contains("_NTC")) 
					sampleIdLength = fileName.indexOf("_NTC");
			else if(fileName.contains("_NewPRM")) 
				sampleIdLength = fileName.indexOf("_NewPRM");
			else if(fileName.contains("_PRM")) 
					sampleIdLength = fileName.indexOf("_PRM");
			else if(fileName.contains("_NEW")) 
				sampleIdLength = fileName.indexOf("_NEW");
			else if(fileName.contains("_CAP")) 
				sampleIdLength = fileName.indexOf("_CAP");
			 */


			String sampleID = fileName.substring(0,  sampleIdLength);
			Sample sample = new Sample(sampleID, file.getAbsolutePath(), this);

			if(!idList.contains(sampleID)) {
				tempList.add(sample);
				idList.add(sampleID);
			}
		}



		if(inputTypeError) {
			popUp("Please check input file type (Settings -> Input Parameters)");
			return;
		}

		sampleArray = new Sample[tempList.size()];
		int cnt = 0;
		for(Sample sample:tempList) {
			sampleArray[cnt++] = sample;
		}

		sampleListView.setItems(FXCollections.observableArrayList(idList));	
		selectedSample = 0;
		fillResults();
	}


	
	public static boolean isDouble(String str) {
	    if (str == null || str.isEmpty()) return false;

	    try {
	        Double.parseDouble(str);
	        return true;
	    } catch (NumberFormatException e) {
	        return false;
	    }
	}
	
	
	
	/**
	 * Shows the message with a popup
	 * @param message : message to be showen
	 */
	public void popUp (String message) {
		Stage dialog = new Stage(StageStyle.DECORATED);
		dialog.initOwner(primaryStage);
		dialog.initModality(Modality.WINDOW_MODAL);
		dialog.setTitle("Notice");
		Parent parent;
		try {
			parent = FXMLLoader.load(getClass().getResource("popup.fxml"));
			Label messageLabel = (Label)parent.lookup("#messageLabel");
			messageLabel.setText(message);
			messageLabel.setWrapText(true);
			Button okButton = (Button) parent.lookup("#okButton");
			okButton.setOnAction(event->dialog.close());
			Scene scene = Ui.scene(parent);


			dialog.setScene(scene);
			dialog.setResizable(false);
			dialog.show();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}


	public static String readFile(String filePath)
	{
		// Declaring object of StringBuilder class
		StringBuilder builder = new StringBuilder();

		// try block to check for exceptions where
		// object of BufferedReader class us created
		// to read filepath
		try (BufferedReader buffer = new BufferedReader(
				new InputStreamReader(new FileInputStream(filePath), java.nio.charset.StandardCharsets.UTF_8))) {

			String str;

			// Condition check via buffer.readLine() method
			// holding true upto that the while loop runs
			while ((str = buffer.readLine()) != null) {
				builder.append(str).append("\n");
			}
		}

		// Catch block to handle the exceptions
		catch (IOException e) {

			// Print the line number here exception occurred
			// using printStackTrace() method
			e.printStackTrace();
		}

		// Returning a string
		return builder.toString();
	}
}



