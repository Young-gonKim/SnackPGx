package snackpgx;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;

public class Variant implements Comparable<Variant> {
	//public double GQ_cutoff=20;



	// Quality-filter cutoffs are global (static) so they can be adjusted at runtime
	// via Settings > Quality Parameters.
	//
	// A single QUAL cutoff is applied to every variant regardless of whether it is
	// allele-defining (no allele-defining vs. others distinction).
	public static double QUAL_cutoff=100;
	// Variants whose QUAL is below this threshold are flagged (shown in red font in
	// the variants table) as lower-confidence calls, even though they still pass the
	// filter. This is a display-only threshold and does not affect filtering.
	public static double QUAL_red_threshold=300;
	public static double hetero_VAF=20;
	public static double depth_cutoff=10;
	public static double homo_VAF=80;


	public boolean passFilter() {
		//double d_GQ=0, d_QUAL=0, d_VAF=0, d_depth=0;
		double d_QUAL=0, d_VAF=0, d_depth=0;

		try {
			d_QUAL = Double.parseDouble(QUAL);
		} catch(Exception ex) {}

		try {
			d_VAF = Double.parseDouble(VAF);
		} catch(Exception ex) {}

		try {
			d_depth = Double.parseDouble(depth);
		} catch(Exception ex) {}

		// A single unified QUAL cutoff applies to every variant.
		boolean pass = d_VAF>=hetero_VAF && d_QUAL>=QUAL_cutoff && d_depth>=depth_cutoff;

		return pass;

		//For homozygous calls, GQ can be low even when normal, so GQ filtering is not applied.
		//return ((d_VAF>=homo_VAF || (d_VAF>=hetero_VAF && d_GQ>=GQ_cutoff && d_GQ<=100)) && d_QUAL>=QUAL_cutoff && d_depth>=depth_cutoff);

	}

	/** Lower-confidence call: QUAL below the red-highlight threshold (display only). */
	public boolean isLowQual() {
		try {
			return Double.parseDouble(QUAL) < QUAL_red_threshold;
		} catch(Exception ex) {
			return false;
		}
	}

	/*
	public boolean passFilter() {
		try {
			double d_QUAL = Double.parseDouble(QUAL);
			double d_VAF = Double.parseDouble(VAF);
			double d_depth = Double.parseDouble(depth);

			return (d_QUAL>=QUAL_cutoff && d_VAF>=hetero_VAF && d_depth>=depth_cutoff);
		}
		catch(Exception ex) {
			return false;
		}
	}
	 */



	//gene, indexID, refseq, NTchange, AAchange, depth, VAF, GQ, strandBias, QUAL

	public String gene = "";
	public String indexID = "";
	public String rsID = "";
	public String refseq = "";
	public String NTchange = "";
	public String AAchange = "";
	public String depth = "";
	public String zygosity = "";
	public String VAF = "";
	public String GQ = "";
	public String strandBias = "";
	public String QUAL = "";
	public String starAlleles = "";
	public String included = "X";
	public String AlleleFreq = "";

	public SimpleStringProperty IncludedProperty;
	public SimpleStringProperty indexIdProperty;
	public SimpleStringProperty rsIdProperty;
	public SimpleStringProperty NTchangeProperty;
	public SimpleStringProperty AAchangeProperty;
	public SimpleStringProperty ZygosityProperty;
	public SimpleStringProperty VAFProperty;
	public SimpleStringProperty DepthProperty;
	public SimpleStringProperty GQProperty;
	public SimpleStringProperty StrandBiasProperty;
	public SimpleStringProperty QUALProperty;
	public SimpleStringProperty StarAllelesProperty;
	public SimpleStringProperty AlleleFreqProperty;

	public int compareTo(Variant o) {
		String thisKey = this.gene+":"+this.indexID;
		System.out.println(thisKey);
		String oKey = o.gene+":"+o.indexID;
		return thisKey.compareTo(oKey);
	}

	public Variant (Variant v1) {
		this.gene = v1.gene;
		this.indexID = v1.indexID;
		this.rsID = v1.rsID;
		this.refseq = v1.refseq;
		this.NTchange = v1.NTchange;
		this.AAchange = v1.AAchange;
		this.depth = v1.depth;
		this.zygosity = v1.zygosity;
		this.VAF = v1.VAF;
		this.GQ = v1.GQ;
		this.strandBias = v1.strandBias;
		this.QUAL = v1.QUAL;
		this.starAlleles = v1.starAlleles;
		this.AlleleFreq = v1.AlleleFreq;

		this.IncludedProperty = v1.IncludedProperty;
		this.indexIdProperty =  v1.indexIdProperty;
		this.rsIdProperty = v1.rsIdProperty;
		this.NTchangeProperty = v1.NTchangeProperty;
		this.AAchangeProperty = v1.AAchangeProperty;
		this.ZygosityProperty = v1.ZygosityProperty;
		this.VAFProperty = v1.VAFProperty;
		this.DepthProperty = v1.DepthProperty;
		this.GQProperty = v1.GQProperty;
		this.StrandBiasProperty = v1.StrandBiasProperty;
		this.QUALProperty = v1.QUALProperty;
		this.StarAllelesProperty = v1.StarAllelesProperty;
		this.AlleleFreqProperty = v1.AlleleFreqProperty;
		this.QUALProperty = v1.QUALProperty;
		this.StarAllelesProperty = v1.StarAllelesProperty;
		this.AlleleFreqProperty = v1.AlleleFreqProperty;

	}

	public Variant(String gene, String indexID, String rsID, String refseq, String nTchange, String aAchange, String zygosity, String depth, String vAF,
			String gQ, String strandBias, String qUAL, String starAlleles, String AlleleFreq) {
		super();
		this.gene = gene;
		this.indexID = indexID;
		this.rsID = rsID;
		this.refseq = refseq;
		this.NTchange = nTchange;

		this.AAchange = aAchange;
		if (this.AAchange.length()<2) 
			this.AAchange = "p.?";
		else if (this.AAchange.length() >= 9) //p.Met1Met
			if (this.AAchange.substring(2,5).equals(this.AAchange.substring(this.AAchange.length()-3, this.AAchange.length())))
				this.AAchange = this.AAchange.substring(0,this.AAchange.length()-3) + "=";


		this.zygosity = zygosity;
		this.depth = depth;
		this.GQ = gQ;
		this.strandBias = strandBias;
		this.QUAL = qUAL;
		this.VAF = vAF;
		this.starAlleles = starAlleles;
		this.AlleleFreq = AlleleFreq;
		//System.out.println("EAS freq : " + AlleleFreq);


		if(starAlleles != null && starAlleles.length()>0)
			included = "O";

		double vaf = 0;
		try {
			vaf = Double.parseDouble(VAF);
			vaf *= 100;

			if(vaf>=homo_VAF)  
				this.zygosity = "Hom";

			this.VAF = String.format("%.2f",  vaf);
		}
		catch (Exception ex) {
			//ex.printStackTrace();
		}


		if(NTchange.contains("del") && !NTchange.contains("ins")) {
			NTchange = NTchange.substring(0, NTchange.indexOf("del")+3);
		}

		if(NTchange.contains("del") && NTchange.contains("ins")) {
			NTchange = NTchange.substring(0, NTchange.indexOf("del")+3) + NTchange.substring(NTchange.indexOf("ins"));
		}


		if(NTchange.contains("dup")) {
			NTchange = NTchange.substring(0, NTchange.indexOf("dup")+3);
		}

		if(AAchange.length()>15)
			AAchange = transformShortAA(AAchange, true);

		IncludedProperty =  new SimpleStringProperty(this.included);
		indexIdProperty =  new SimpleStringProperty(this.indexID);
		rsIdProperty =  new SimpleStringProperty(this.rsID);
		NTchangeProperty = new SimpleStringProperty(this.NTchange);
		AAchangeProperty = new SimpleStringProperty(this.AAchange);
		ZygosityProperty = new SimpleStringProperty(this.zygosity);
		VAFProperty =  new SimpleStringProperty(this.VAF);
		DepthProperty = new SimpleStringProperty(this.depth);
		GQProperty =  new SimpleStringProperty(this.GQ);
		StrandBiasProperty =  new SimpleStringProperty(this.strandBias);
		QUALProperty =  new SimpleStringProperty(this.QUAL);
		StarAllelesProperty =  new SimpleStringProperty(this.starAlleles);
		AlleleFreqProperty = new SimpleStringProperty(this.AlleleFreq);
	}

	public String getIndexIdProperty() {
		return indexIdProperty.get();
	}

	public String getRsIdProperty() {
		return rsIdProperty.get();
	}

	public String getNTchangeProperty() {
		return NTchangeProperty.get();
	}

	public String getAAchangeProperty() {
		return AAchangeProperty.get();
	}

	public String getZygosityProperty() {
		return ZygosityProperty.get();
	}

	public String getDepthProperty() {
		return DepthProperty.get();
	}

	public String getIncludedProperty() {
		return IncludedProperty.get();
	}

	public void setIncludedProperty(String included) {
		this.included = included;
		IncludedProperty.set(included);
	}

	public String getVAFProperty() {
		return VAFProperty.get();
	}

	public String getGQProperty() {
		return GQProperty.get();
	}

	public String getStrandBiasProperty() {
		return StrandBiasProperty.get();
	}

	public String getQUALProperty() {
		return QUALProperty.get();
	}

	public String getStarAllelesProperty() {
		return StarAllelesProperty.get();
	}

	public String getAlleleFreqProperty() {
		return AlleleFreqProperty.get();
	}

	public static String[] longAAList = {"Ala", "Cys", "Asp", "Glu", "Phe", "Gly", "His", "Ile", "Lys", "Leu", "Met", "Asn", "Pro", "Gln", "Arg", "Ser", "Thr", "Trp", "Tyr", "Val", "Ter"};
	public static String[] shortAAList = {"A", "C", "D", "E", "F", "G", "H", "I", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "W", "Y", "V", "*"};

	public static String transformShortAA(String aaChange, boolean pDot) {
		String ret = aaChange;
		if(ret.equals("p.?"))
			return ret;
		for(int i=0;i<longAAList.length;i++) {
			ret = ret.replace(longAAList[i],  shortAAList[i]);
		}
		if(ret.length()>2 && !pDot) {
			ret = ret.substring(2);
		}
		return ret;
	}
}
