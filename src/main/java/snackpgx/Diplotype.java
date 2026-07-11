package snackpgx;

import java.util.Vector;

import javafx.beans.property.SimpleStringProperty;

public class Diplotype extends DiplotypeMeta implements Comparable<Diplotype> {

	public double score = 0;
	public String included = "O";
	public String haplotype1Variants = "";
	public String haplotype2Variants = "";
	
	private SimpleStringProperty IncludedProperty;
	private SimpleStringProperty NameProperty;
	private SimpleStringProperty ScoreProperty;
	private SimpleStringProperty VariantsProperty;
	private SimpleStringProperty DiplotypeFrequencyProperty;
	private SimpleStringProperty DiplotypeActivityScoreProperty;
	private SimpleStringProperty DiplotypePhenotypeProperty;
	private SimpleStringProperty Haplotype1NameProperty;
	private SimpleStringProperty Haplotype1FrequencyProperty;
	private SimpleStringProperty Haplotype1ActivityScoreProperty;
	private SimpleStringProperty Haplotype1PhenotypeProperty;
	private SimpleStringProperty Haplotype2NameProperty;
	private SimpleStringProperty Haplotype2FrequencyProperty;
	private SimpleStringProperty Haplotype2ActivityScoreProperty;
	private SimpleStringProperty Haplotype2PhenotypeProperty;
	
	public int compareTo(Diplotype  d) {
		if(score > d.score) return -1;
		else if(score < d.score) return 1;
		return 0;
	}


	public Diplotype (DiplotypeMeta diplotypeMeta, String haplotype1Variants, String haplotype2Variants, double score) {
		super();
		this.name = diplotypeMeta.name;
		this.variants = diplotypeMeta.variants;
		this.diplotypeFrequency = diplotypeMeta.diplotypeFrequency;
		this.diplotypeActivityScore = diplotypeMeta.diplotypeActivityScore;
		this.diplotypePhenotype = diplotypeMeta.diplotypePhenotype;
		this.phenotypeDescription = diplotypeMeta.phenotypeDescription;
		this.haplotype1Name = diplotypeMeta.haplotype1Name;
		this.haplotype1Variants = haplotype1Variants;
		this.haplotype1Frequency = diplotypeMeta.haplotype1Frequency;
		this.haplotype1ActivityScore = diplotypeMeta.haplotype1ActivityScore;
		this.haplotype1Phenotype = diplotypeMeta.haplotype1Phenotype;
		this.haplotype2Name = diplotypeMeta.haplotype2Name;
		this.haplotype2Variants = haplotype2Variants;
		this.haplotype2Frequency = diplotypeMeta.haplotype2Frequency;
		this.haplotype2ActivityScore = diplotypeMeta.haplotype2ActivityScore;
		this.haplotype2Phenotype = diplotypeMeta.haplotype2Phenotype;
		this.score = score;
		
		String variantString = "";
		for(String variant : variants) {
			variantString += variant + ", ";
		}
		
		IncludedProperty =  new SimpleStringProperty(included);
		NameProperty =  new SimpleStringProperty(name);
		VariantsProperty =  new SimpleStringProperty(variantString);
		DiplotypeFrequencyProperty =  new SimpleStringProperty(diplotypeFrequency);
		DiplotypeActivityScoreProperty =  new SimpleStringProperty(diplotypeActivityScore);
		DiplotypePhenotypeProperty =  new SimpleStringProperty(diplotypePhenotype);
		
		Haplotype1NameProperty = new SimpleStringProperty(haplotype1Name);
		Haplotype1FrequencyProperty =  new SimpleStringProperty(haplotype1Frequency);
		Haplotype1ActivityScoreProperty =  new SimpleStringProperty(haplotype1ActivityScore);
		Haplotype1PhenotypeProperty =  new SimpleStringProperty(haplotype1Phenotype);

		Haplotype2NameProperty = new SimpleStringProperty(haplotype2Name);
		Haplotype2FrequencyProperty =  new SimpleStringProperty(haplotype2Frequency);
		Haplotype2ActivityScoreProperty =  new SimpleStringProperty(haplotype2ActivityScore);
		Haplotype2PhenotypeProperty =  new SimpleStringProperty(haplotype2Phenotype);
		
		
		ScoreProperty =  new SimpleStringProperty(String.format("%.2f",  score));
	}

	public void setIncludedProperty(String included) {
		this.included = included;
		IncludedProperty.set(included);
	}
	public String getIncludedProperty() {
		return IncludedProperty.get();
	}
	public String getNameProperty() {
		return NameProperty.get();
	}
	public String getVariantsProperty() {
		return VariantsProperty.get();
	}
	
	public String getDiplotypeFrequencyProperty() {
		return DiplotypeFrequencyProperty.get();
	}
	public String getDiplotypeActivityScoreProperty() {
		return DiplotypeActivityScoreProperty.get();
	}
	public String getDiplotypePhenotypeProperty() {
		return DiplotypePhenotypeProperty.get();
	}
	
	public String getHaplotype1NameProperty() {
		return Haplotype1NameProperty.get();
	}
	public String getHaplotype1FrequencyProperty() {
		return Haplotype1FrequencyProperty.get();
	}
	public String getHaplotype1ActivityScoreProperty() {
		return Haplotype1ActivityScoreProperty.get();
	}
	public String getHaplotype1PhenotypeProperty() {
		return Haplotype1PhenotypeProperty.get();
	}

	public String getHaplotype2NameProperty() {
		return Haplotype2NameProperty.get();
	}
	public String getHaplotype2FrequencyProperty() {
		return Haplotype2FrequencyProperty.get();
	}
	public String getHaplotype2ActivityScoreProperty() {
		return Haplotype2ActivityScoreProperty.get();
	}
	public String getHaplotype2PhenotypeProperty() {
		return Haplotype2PhenotypeProperty.get();
	}
	
	public String getScoreProperty() {
		return ScoreProperty.get();
	}
}