package snackpgx;

import javafx.beans.property.SimpleStringProperty;

public class BatchVariant extends Variant {
	public String sampleID = "";
	private SimpleStringProperty sampleIDProperty;
	
	public BatchVariant (Variant v1, String sampleID) {
		super(v1);
		this.sampleID = sampleID;
		sampleIDProperty = new SimpleStringProperty(sampleID);
	}
	
	public String getSampleIDProperty() {
		return sampleIDProperty.get();
	}
	
}
