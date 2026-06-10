package snackpgx;

import java.util.TreeSet;
import java.util.Vector;

public class DiplotypeMeta {

	public String name = null;
	public Vector<String> variants = null;
	public String diplotypeFrequency = "";
	public String diplotypePhenotype = "";
	public String phenotypeDescription = "";
	public String diplotypeActivityScore = "";
	public String haplotype1Name = "";
	public Vector<String> haplotype1VariantList = null;
	public String haplotype1Frequency = "";
	public String haplotype1Phenotype = "";
	public String haplotype1ActivityScore = "";
	public String haplotype2Name = "";
	public Vector<String> haplotype2VariantList = null;
	public String haplotype2Frequency = "";
	public String haplotype2Phenotype = "";
	public String haplotype2ActivityScore = "";
	
	public DiplotypeMeta() {
	}

	public DiplotypeMeta(String name, Vector<String> variants, String diplotypeFrequency, String diplotypePhenotype, String phenotypeDescription, String diplotypeActivityScore, 
			String haplotype1Name, Vector<String> haplotype1VariantList, String haplotype1Frequency, String haplotype1Phenotype, String haplotype1ActivityScore,
			String haplotype2Name, Vector<String> haplotype2VariantList, String haplotype2Frequency, String haplotype2Phenotype, String haplotype2ActivityScore
			) 
	{
		this.name = name;
		this.variants = variants;
		this.diplotypeFrequency = nullToEmpty(diplotypeFrequency);
		this.diplotypePhenotype = EmptyToUnknown(nullToEmpty(diplotypePhenotype));
		this.phenotypeDescription = nullToEmpty(phenotypeDescription);
		this.diplotypeActivityScore = nullToEmpty(diplotypeActivityScore);

		this.haplotype1Name = haplotype1Name;
		this.haplotype1VariantList = haplotype1VariantList;
		this.haplotype1Frequency = nullToEmpty(haplotype1Frequency);
		this.haplotype1Phenotype = nullToEmpty(haplotype1Phenotype);
		this.haplotype1ActivityScore = nullToEmpty(haplotype1ActivityScore);

		this.haplotype2Name = haplotype2Name;
		this.haplotype2VariantList = haplotype2VariantList;
		this.haplotype2Frequency = nullToEmpty(haplotype2Frequency);
		this.haplotype2Phenotype = nullToEmpty(haplotype2Phenotype);
		this.haplotype2ActivityScore = nullToEmpty(haplotype2ActivityScore);
	}

	private String EmptyToUnknown(String input) {
		if(input == null || input.trim().length() == 0) 
			return "Unknown";
		else 
			return input;
	}

	
	private String nullToEmpty(String input) {
		if(input == null || input.equals("n/a")) 
			return "";
		else 
			return input;
	}
	
	public String getStringVariants() {
		String ret = "";
		for(String variant : variants) 
			ret += variant + ", ";
		return ret;
	}
}