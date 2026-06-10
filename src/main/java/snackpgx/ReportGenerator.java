package snackpgx;

import java.util.Hashtable;
import java.util.TreeSet;
import java.util.Vector;

public class ReportGenerator {

	//Top N haplotype frequency
	private static final int topN = 5;
	private static final int reportWidth = 81;
	private static final String noGenotypeText = "No genotype result available";
	private static final String databaseComment = "The above results were prepared using the Gene-specific Information Tables from PharmGKB. (https://www.pharmgkb.org/page/pgxGeneRef, downloaded 2024.3.12)";
	private static final String notReportedPhenotypeList[] = {"Unknown", "Normal", "Not defined", ""};
	// The "TEST INFORMATION AND FINDINGS" footer is no longer hard-coded here; it is
	// externalized to comments/HealthCheckup.txt and comments/Cardiology Panel.txt
	// (with {VERSION}/{DBVERSION} placeholders substituted in RootController.readComments).



	private static String V433M_detected(Vector<Variant> variantList) {
		String ret = "no";
		for(Variant v : variantList) {
			if(v.gene.equals("CYP4F2") && v.AAchange.equals("p.Val433Met"))
				ret = v.zygosity;
		}
		return ret;
	}


	private static Diplotype getFirstDiplotype(Vector<Diplotype> diplotypeList) {
		//Default to the first diplotype (handle the exception where all are excluded).
		Diplotype ret = diplotypeList.get(0);		
		for(Diplotype diplotype:diplotypeList) {
			if(diplotype.included.equals("O")) { 
				ret = diplotype;
				break;
			}
		}
		return ret;
	}


	public static boolean abnormalPhenotype(String phenotypeDescription) {
		boolean ret = true;
		for(String phenotype : notReportedPhenotypeList) {
			if(phenotypeDescription.equals(phenotype)) 
				ret = false;
		}
		return ret;
	}


	public static String enzymeActivity(String haplotypePhenotype) {
		String ret = "";

		if(haplotypePhenotype.equals("No function"))
			return "with no enzyme activity";
		else if(haplotypePhenotype.equals("Normal function"))
			return "with normal enzyme activity";
		else if(haplotypePhenotype.equals("Increased function"))
			return "with increased enzyme activity";
		else if(haplotypePhenotype.equals("Decreased function"))
			return "with decreased enzyme activity";
		else if(haplotypePhenotype.equals("Uncertain function"))
			return "with uncertain enzyme activity";
		return ret;
	}

	public static String getMTHFRReport(GeneContent geneContent, boolean healthCare) {

		String genotype665 = "CC";
		String genotype1286 = "AA";
		String zygosity665 = "homozygote";
		String zygosity1286 = "homozygote";
		String phenotype665 = "Normal enzyme activity";
		String phenotype1286 = "Normal enzyme activity";


		Vector<Variant> variantList = geneContent.variantList;

		for(Variant v: variantList) {

			if(v.NTchange.equals("c.665C>T")) {
				if(v.zygosity.equals("Hom")) {
					genotype665 = "TT";
					phenotype665 = "Decreased enzyme activity";
				}
				else if (v.zygosity.equals("Het")) {
					genotype665 = "CT";
					zygosity665 = "heterozygote";
					phenotype665 = "Slightly decreased enzyme activity";
				}
			}
			if(v.NTchange.equals("c.1286A>C")) {
				if(v.zygosity.equals("Hom")) {
					genotype1286 = "CC";
					phenotype1286 = "Slightly decreased enzyme activity";
				}
				else if (v.zygosity.equals("Het")) {
					genotype1286 = "AC";
					zygosity1286 = "heterozygote";
					phenotype1286 = "Slightly decreased enzyme activity";
				}
			}
		}

		String report = "";

		if(healthCare) {
			char genotype665_1 = genotype665.charAt(0);
			char genotype665_2 = genotype665.charAt(1);

			char genotype1286_1 = genotype1286.charAt(0);
			char genotype1286_2 = genotype1286.charAt(1);

			Diplotype diplotype = getFirstDiplotype(geneContent.diplotypeList);
			report += String.format("MTHFR      665%c/665%c, 1286%c/1286%c         %s\n", genotype665_1, genotype665_2, genotype1286_1, genotype1286_2, diplotype.phenotypeDescription);

			/*

			report += String.format("MTHFR      c.665C>T  : %2s                 %s\n", genotype665, diplotype.phenotypeDescription);
			report += String.format("           c.1286A>C : %2s\n", genotype1286);
			 */
		}

		else {
			report += "▣ CONCLUSION AND DIAGNOSIS\n\n" + 
					"< MTHFR Genotyping>\n\n" + 
					"- RESULTS:\n";
			report += "-------------------------------------------------------------------------------\n" + 
					"   Gene   NT change  rs number     Genotype         Phenotype\n" + 
					"-------------------------------------------------------------------------------\n";
			report += String.format("   MTHFR  c.665C>T   rs1801133     %2s %-12s  %-32s\n", genotype665, zygosity665, phenotype665);
			report += String.format("          c.1286A>C  rs1801131     %2s %-12s  %-32s\n", genotype1286, zygosity1286, phenotype1286);
			report += "-------------------------------------------------------------------------------\n\n";	
		}

		return report;
	}


	public static String getHealthCheckUpReport(GeneContent[] geneContentArray) {
		String conclusion = "▣ CONCLUSION AND DIAGNOSIS\nBased on the results of the 20 genes tested below, ";
		String opinion = "▣ COMMENTS\n";
		String abnormalGeneList = "";
		String reportedInterpretationList = "";

		opinion += "Clinical drug response\n" +
				"The genotypes detected in the tested genes and their corresponding phenotypes are shown below. If a patient is taking a drug associated with a gene showing a non-normal phenotype (see Table 1), the efficacy or the risk of adverse effects may be increased/decreased compared with other people even at the same dose. However, drug response is not determined by genetic factors alone; even with the same genotype, individual differences arise from numerous factors such as concomitant medications and disease state, so comprehensive interpretation of the results and determination of the application policy are required.\n" +
				"If you are taking or plan to take any of the related drugs, or if you would like a more detailed explanation of these test results, genetic counseling is available.\n\n";

		opinion += "----------------------------------------------------------------------------------\n";
		opinion += "Gene          Genotype                     Phenotype\n";
		opinion += "----------------------------------------------------------------------------------\n";


		for(GeneContent geneContent : geneContentArray) {
			String gene = geneContent.name;
			Diplotype diplotype = null;
			if(gene.equals("CYP1A2") || gene.equals("ALDH2"))	//Not in the current panel; excluded from the report.
				continue;

			if(gene.equals("CYP4F2")) {
				if(V433M_detected(geneContent.variantList).equals("Het") || V433M_detected(geneContent.variantList).equals("Hom"))
					opinion += String.format("%-10s %-30s %-40s\n",  gene, "V433M : Detected", "Increased dose requirement");
				else
					opinion += String.format("%-10s %-30s %-40s\n",  gene, "V433M : Not detected", "Normal dose requirement");
				continue;
			}

			if(geneContent.diplotypeList.isEmpty()) 
				opinion += "Failure to derive diplotype from variants.\n";
			else {
				diplotype = getFirstDiplotype(geneContent.diplotypeList);

				if(abnormalPhenotype(diplotype.phenotypeDescription)) 
					abnormalGeneList += gene + ", ";

				if(gene.equals("MTHFR")) {
					opinion += getMTHFRReport(geneContent, true);
				}

				else {
					opinion += String.format("%-10s %-30s %-40s\n",  gene, diplotype.name, diplotype.phenotypeDescription);
				}
			}

			if(gene.equals("DPYD")) {
				opinion += "HLA-A/B    Risk allele not detected**     Normal risk of cutaneous hypersensitivity \n";
				/*
				opinion += String.format("%-10s %-30s %-40s\n",  "HLA-A", "A*31:01 : Positive/Negative", "Increased/normal risk of cutaneous hypersensitivity");
				opinion += String.format("%-10s %-30s %-40s\n",  "HLA-B", "B*15:02 : Positive/Negative", "Increased/normal risk of cutaneous hypersensitivity");
				opinion += String.format("%-10s %-30s %-40s\n",  "", "B*15:11 : Positive/Negative", "");
				opinion += String.format("%-10s %-30s %-40s\n",  "", "B*57:01 : Positive/Negative", "");
				opinion += String.format("%-10s %-30s %-40s\n",  "", "B*58:01 : Positive/Negative", "");
				 */
				continue;
			}



		}
		opinion += "----------------------------------------------------------------------------------\n";
		opinion += "** HLA-A/B risk alleles : A*31:01, B*15:02, B*15:11, B*57:01, B*58:01\n";

		if(abnormalGeneList.length()>2)
			abnormalGeneList = abnormalGeneList.substring(0, abnormalGeneList.length()-2);
		conclusion += abnormalGeneList + " showed findings that may affect drug response.\n";


		//opinion += reportedInterpretationList;

		// TEST INFORMATION footer is externalized to comments/HealthCheckup.txt
		// and appended by RootController.readComments/fillResults.
		String report = conclusion + "\n" + opinion + "\n";
		return report;
	}

	public static String getCardioPanelReport(GeneContent[] geneContentArray, int panelIndex) {
		String title = "<Pharmacogenetics Report : Cardiology Panel>\n\n";

		String clopidogrel = "I. Clopidogrel-related genes\n";
		String warfarin = "II. Warfarin-related genes\n";
		String statin = "III. Statin-related genes\n";
		String phenotypeTable = "";
		String variantTable = "";

		Diplotype CYP2C9Diplotype = null;
		Diplotype VKORC1Diplotype = null;
		Diplotype CYP4F2Diplotype = null;
		Diplotype CYP2C19Diplotype = null;
		Diplotype SLCO1B1Diplotype = null;
		Diplotype ABCG2Diplotype = null;

		String V433M = null;


		for(GeneContent geneContent : geneContentArray) {

			if(geneContent.name.equals("CYP2C9")) 
				if(!geneContent.diplotypeList.isEmpty())
					CYP2C9Diplotype = getFirstDiplotype(geneContent.diplotypeList);

			if(geneContent.name.equals("VKORC1"))
				if(!geneContent.diplotypeList.isEmpty())
					VKORC1Diplotype = getFirstDiplotype(geneContent.diplotypeList);

			if(geneContent.name.equals("CYP4F2")) {
				if(!geneContent.diplotypeList.isEmpty())
					CYP4F2Diplotype = getFirstDiplotype(geneContent.diplotypeList);
				V433M = V433M_detected(geneContent.variantList);
			}

			if(geneContent.name.equals("CYP2C19")) 
				if(!geneContent.diplotypeList.isEmpty())
					CYP2C19Diplotype = getFirstDiplotype(geneContent.diplotypeList);

			if(geneContent.name.equals("SLCO1B1")) 
				if(!geneContent.diplotypeList.isEmpty())
					SLCO1B1Diplotype = getFirstDiplotype(geneContent.diplotypeList);

			if(geneContent.name.equals("ABCG2")) 
				if(!geneContent.diplotypeList.isEmpty())
					ABCG2Diplotype = getFirstDiplotype(geneContent.diplotypeList);

		}

		//clopidogrel : PMID: 35034351
		clopidogrel += "1) CYP2C19\n";
		clopidogrel += String.format("  - Genotype : %-30s\n  - Phenotype : %-40s\n", CYP2C19Diplotype.name, CYP2C19Diplotype.diplotypePhenotype);
		if (CYP2C19Diplotype.diplotypePhenotype.contains("Intermediate") || CYP2C19Diplotype.diplotypePhenotype.contains("Poor")) {
			clopidogrel += "  - Recommendation : Consider alternative drug such as prasugrel or ticagrelor (CPIC guideline, PMID: 35034351)\n";
		}

		//warfarin : PMID: 28198005
		warfarin += "1) CYP2C9\n";
		warfarin += String.format("  - Genotype : %-30s\n  - Phenotype : %-40s\n", CYP2C9Diplotype.name, CYP2C9Diplotype.diplotypePhenotype);
		if (CYP2C9Diplotype.diplotypePhenotype.contains("Intermediate") || CYP2C9Diplotype.diplotypePhenotype.contains("Poor")) {
			warfarin += "  - Recommendation : Warfarin dose adjusment (CPIC guideline, PMID: 28198005)\n";
		}

		warfarin += "2) VKORC1\n";
		warfarin += String.format("  - Genotype : %-30s\n  - Phenotype : %-40s\n", VKORC1Diplotype.name, VKORC1Diplotype.diplotypePhenotype);
		if (VKORC1Diplotype.diplotypePhenotype.contains("Medium") || VKORC1Diplotype.diplotypePhenotype.contains("Low")) {
			warfarin += "  - Recommendation : Warfarin dose adjusment (CPIC guideline, PMID: 28198005)\n";
		}

		warfarin += "3) CYP4F2\n";

		String V433M_genotype = "V433M not detected";
		String V433M_phenotype = "Normal activity";
		if(V433M.equals("Hom") || V433M.equals("Het")){
			V433M_genotype = "V433M detected (" + V433M + ")";
			V433M_phenotype = "Reduced activity";
		}

		warfarin += String.format("  - Genotype : %-30s\n  - Phenotype : %-40s\n", V433M_genotype, V433M_phenotype);

		if(V433M.equals("Hom") || V433M.equals("Het")){
			warfarin += "  - Recommendation : Warfarin dose adjusment (CPIC guideline, PMID: 28198005)\n";
		}



		//statin : PMID: 35152405
		statin += "1) SLCO1B1\n";
		statin += String.format("  - Genotype : %-30s\n  - Phenotype : %-40s\n", SLCO1B1Diplotype.name, SLCO1B1Diplotype.diplotypePhenotype);
		if (SLCO1B1Diplotype.diplotypePhenotype.contains("Decreased") || SLCO1B1Diplotype.diplotypePhenotype.contains("Poor")) {
			statin += "  - Recommendation : Increased statin exposure. Refer to the CPIC guideline for myopathy risk assessment and statin dosing (PMID: 35152405)\n";
		}

		statin += "2) ABCG2\n";
		statin += String.format("  - Genotype : %-30s\n  - Phenotype : %-40s\n", ABCG2Diplotype.name, ABCG2Diplotype.diplotypePhenotype);
		if (ABCG2Diplotype.diplotypePhenotype.contains("Decreased") || ABCG2Diplotype.diplotypePhenotype.contains("Poor")) {
			statin += "  - Recommendation : Increased rosuvastatin exposure. Refer to the CPIC guideline for myopathy risk assessment and rosuvastatin dosing (PMID: 35152405)\n";
		}

		statin += "3) CYP2C9\n";
		statin += String.format("  - Genotype : %-30s\n  - Phenotype : %-40s\n", CYP2C9Diplotype.name, CYP2C9Diplotype.diplotypePhenotype);
		if (CYP2C9Diplotype.diplotypePhenotype.contains("Intermediate") || CYP2C9Diplotype.diplotypePhenotype.contains("Poor")) {
			statin += "  - Recommendation : Increased fluvastatin exposure. Refer to the CPIC guideline for myopathy risk assessment and fluvastatin dosing (PMID: 35152405)\n";
		}


		phenotypeTable += "----------------------------------------------------------------------------------\n";
		phenotypeTable += "Gene      Genotype                Phenotype\n";
		phenotypeTable += "----------------------------------------------------------------------------------\n";

		/*
		//alphabetical order version
		for(GeneContent geneContent : geneContentArray) {
			String gene = geneContent.name;
			Diplotype diplotype = null;
			//pass if not included in cardiology panel
			if(geneContent.geneMetaData.panelInclusionList[panelIndex] == false)
				continue;
			if(geneContent.diplotypeList.isEmpty()) 
				phenotypeTable += "Failure to derive diplotype from variants.\n";
			else {
				diplotype = getFirstDiplotype(geneContent.diplotypeList);
				phenotypeTable += String.format("%-9s %-23s %-40s\n",  gene, diplotype.name, diplotype.diplotypePhenotype);
			}
		}
		 */

		//ORDER CHANGE
		Hashtable orderTable = new Hashtable<String, String>();
		orderTable.put("CYP2C19", "1");
		orderTable.put("CYP2C9", "2");
		orderTable.put("VKORC1", "3");
		orderTable.put("CYP4F2", "4");
		orderTable.put("SLCO1B1", "5");
		orderTable.put("ABCG2", "6");
		orderTable.put("CYP2D6", "7");

		TreeSet<String> lineSet = new TreeSet<String>();

		for(GeneContent geneContent : geneContentArray) {
			String gene = geneContent.name;
			String line = "";
			Diplotype diplotype = null;
			//pass if not included in cardiology panel
			if(geneContent.geneMetaData.panelInclusionList[panelIndex] == false)
				continue;
			if(geneContent.diplotypeList.isEmpty())
				line = "   Failure to derive diplotype from variants.\n";
			else {
				String order = (String)orderTable.get(gene);
				diplotype = getFirstDiplotype(geneContent.diplotypeList);

				if(gene.equals("CYP4F2")) 
					line = String.format("%2s:%-9s %-23s %-40s\n", order, gene, V433M_genotype, V433M_phenotype);
				else 
					line = String.format("%2s:%-9s %-23s %-40s\n", order, gene, diplotype.name, diplotype.diplotypePhenotype);
				lineSet.add(line);
			}
		}

		for(String line : lineSet) 
			phenotypeTable += line.substring(3);

		phenotypeTable += "----------------------------------------------------------------------------------\n\n";

		// TEST INFORMATION footer is externalized to comments/Cardiology Panel.txt
		// and appended by RootController.readComments/fillResults.
		String report = title + clopidogrel + "\n" + warfarin + "\n" + statin + "\n" + phenotypeTable + "\n";
		return report;
	}

	public static String getASMPanelReport(GeneContent[] geneContentArray) {

		Diplotype CYP2C9Diplotype = null;
		Diplotype CYP2C19Diplotype = null;

		for(GeneContent geneContent : geneContentArray) {

			if(geneContent.name.equals("CYP2C9")) 
				if(!geneContent.diplotypeList.isEmpty())
					CYP2C9Diplotype = getFirstDiplotype(geneContent.diplotypeList);

			if(geneContent.name.equals("CYP2C19")) 
				if(!geneContent.diplotypeList.isEmpty())
					CYP2C19Diplotype = getFirstDiplotype(geneContent.diplotypeList);
		}

		String report = "▣ CONCLUSION AND DIAGNOSIS\n\n"; 
		report += "< Pharmacogenetics Anti-Seizure Medication Panel >\n\n";
		report += "- RESULTS:\n" +
				"------------------------------------------------------------\n" + 
				"  Gene      Genotype      Phenotype\n" + 
				"------------------------------------------------------------\n";
		report += String.format("  CYP2C9    %-14s%-30s\n", CYP2C9Diplotype.name, CYP2C9Diplotype.diplotypePhenotype);
		report += String.format("  CYP2C19   %-14s%-30s\n", CYP2C19Diplotype.name, CYP2C19Diplotype.diplotypePhenotype);

		report += "------------------------------------------------------------\n";


		report += "-------------------------------------------------------------------------------\n" + 
				"  Allele     Functional status   Variants         \n" + 
				"-------------------------------------------------------------------------------\n";

		String CYP2C9variants1 = CYP2C9Diplotype.haplotype1Variants;
		if(CYP2C9variants1.length()==0)
			CYP2C9variants1 = "(-)";
		String CYP2C9variants2 = CYP2C9Diplotype.haplotype2Variants;
		if(CYP2C9variants2.length()==0)
			CYP2C9variants2 = "(-)";

		String CYP2C19variants1 = CYP2C19Diplotype.haplotype1Variants;
		if(CYP2C19variants1.length()==0)
			CYP2C19variants1 = "(-)";
		String CYP2C19variants2 = CYP2C19Diplotype.haplotype2Variants;
		if(CYP2C19variants2.length()==0)
			CYP2C19variants2 = "(-)";

		report += String.format("  CYP2C9%-5s%-20s%-20s\n", CYP2C9Diplotype.haplotype1Name, CYP2C9Diplotype.haplotype1Phenotype, CYP2C9variants1);
		if(!CYP2C9Diplotype.haplotype1Name.equals(CYP2C9Diplotype.haplotype2Name))  
			report += String.format("  CYP2C9%-5s%-20s%-20s\n", CYP2C9Diplotype.haplotype2Name, CYP2C9Diplotype.haplotype2Phenotype, CYP2C9variants2);
		report += String.format("  CYP2C19%-4s%-20s%-20s\n", CYP2C19Diplotype.haplotype1Name, CYP2C19Diplotype.haplotype1Phenotype, CYP2C19variants1);
		if(!CYP2C19Diplotype.haplotype1Name.equals(CYP2C19Diplotype.haplotype2Name))  
			report += String.format("  CYP2C19%-4s%-20s%-20s\n", CYP2C19Diplotype.haplotype2Name, CYP2C19Diplotype.haplotype2Phenotype, CYP2C19variants2);


		report += "-------------------------------------------------------------------------------\n\n";

		report += "- ADDENDUM RESULTS:\r\n" + 
				"----------------------------------------------------------------------------------\r\n" + 
				"  Risk allele    Status     Implication\r\n" + 
				"----------------------------------------------------------------------------------\r\n" + 
				"  HLA-A*31:01:   Positive   Increased risk of severe cutaneous adverse reaction                   \r\n" + 
				"  HLA-A*31:01:   Negative   (-)\r\n" + 
				"  HLA-B*15:02:   Positive   Increased risk of severe cutaneous adverse reaction       \r\n" + 
				"  HLA-B*15:02:   Negative   (-)\r\n" + 
				"  HLA-B*15:11:   Positive   Increased risk of severe cutaneous adverse reaction       \r\n" + 
				"  HLA-B*15:11:   Negative   (-)                        \r\n" + 
				"----------------------------------------------------------------------------------\n\n";



		report += "▣ COMMENTS\n\n";
		report += "1. CYP2C9 INTERPRETATION\n";
		report += diplotypeInterpretation("CYP2C9", CYP2C9Diplotype);

		report +=  "\n   For CYP2C9 Intermediate metabolizers (IM) and Poor metabolizers (PM), phenytoin metabolism is reduced,\r\n" +
				"   which is known to increase drug levels and the risk of adverse effects. In general, a dose reduction to 50% or less of the standard dose is recommended for PMs;\r\n" +
				"   please refer to the CPIC guideline (PMID: 32779747) and the DPWG guideline (PMID: 38570725). \r\n" +
				"\r\n" +
				"   In addition, CYP2C9 is involved in the metabolism of NSAIDs (celecoxib, flurbiprofen, lornoxicam, ibuprofen), warfarin, fluvastatin, and others,\r\n" +
				"   so when prescribing these drugs, prescribing in line with the CPIC guideline (PMID: 32189324, 28198005, 35152405) is recommended.\r\n" +
				"  \r\n" +
				"   In Koreans, the frequencies of the CYP2C9*1 and *3 alleles are reported as 96.0% and 3.9%, respectively. \r\n" +
				"   (Ref. ClinPGx CYP2C9 Allele Frequency Table, Accessed Dec. 2025) \n\n";

		report += "2. CYP2C19 INTERPRETATION\n";

		report += diplotypeInterpretation("CYP2C19", CYP2C19Diplotype);


		report += "\n   CYP2C19 is involved in the metabolism of phenytoin and benzodiazepines such as clobazam and diazepam. In CYP2C19 Poor\r\n" +
				"   metabolizers (PM), phenytoin levels have been reported to be about 40% higher than in Normal metabolizers (NM)\r\n" +
				"   (PMID 39115850); note that with clobazam, reduced clearance of the active metabolite N-desmethylclobazam (norclobazam) can increase blood levels\r\n" +
				"   and toxicity risk (ClinPGx, PMID: 15533655, 18466100).\r\n" +
				"\r\n" +
				"   CYP2C19 is also involved in the metabolism of clopidogrel, voriconazole, SSRIs (citalopram, escitalopram, sertraline),\r\n" +
				"   TCA (amitriptyline), and PPIs (omeprazole, lansoprazole, pantoprazole, dexlansoprazole).\r\n" +
				"   When treating with these drugs, prescribing in line with the CPIC guideline (PMID: 35034351, 27981572, 37032427, 27997040,\r\n" +
				"   32770672) is recommended.\r\n" +
				"\r\n" +
				"   In Koreans, the CYP2C19*1, *2, *3, and *17 allele frequencies are reported as 62.9%, 29.3%, 9.1%, and 1.2%, respectively. \r\n" +
				"   (Ref. ClinPGx CYP2C19 Allele Frequency Table, Accessed Dec. 2025)\r\n" +
				"\r\n" +
				"3. HLA-A, HLA-B INTERPRETATION\r\n" +
				"   This patient was found to carry the HLA-A*31:01, HLA-B*15:02, and HLA-B*15:11 alleles associated with an increased risk of anti-seizure-medication-induced cutaneous hypersensitivity.\r\n" +
				"  \r\n" +
				"   Carriers of the HLA-B*15:02 genotype have an increased risk of serious cutaneous adverse reactions to phenytoin, carbamazepine, oxcarbazepine, and lamotrigine,\r\n" +
				"   and cutaneous adverse reactions to carbamazepine are also increased by HLA-B*15:11 and HLA-A*31:01, so avoiding these drugs when possible\r\n" +
				"   is recommended.\r\n" +
				"   (CPIC guideline, PMID: 29392710; DPWG guideline, PMID: 38570725)\r\n" +
				"\r\n" +
				"   In Koreans, the HLA-A*31:01, HLA-B*15:02, and HLA-B*15:11 genotype frequencies are reported as 5.4%, 0.8%, and 1.7%, respectively.\r\n" +
				"   (Ref. ClinPGx HLA-A/B Allele Frequency Table, Allele Frequency Net Database, Accessed Dec. 2025)\n\n";
			


		return report;
	}

	
	private static String getCancerAlleleTable(String gene, Diplotype diplotype) {
		String table = "-------------------------------------------------------------------------------\n" +
				"  Allele          Functional status       Variants         \n" +
				"-------------------------------------------------------------------------------\n";

		String variants1 = diplotype.haplotype1Variants;
		if(variants1.length()==0)
			variants1 = "(-)";
		String variants2 = diplotype.haplotype2Variants;
		if(variants2.length()==0)
			variants2 = "(-)";

		String allele1 = gene + diplotype.haplotype1Name;
		String allele2 = gene + diplotype.haplotype2Name;

		table += String.format("  %-15s %-23s %-30s\n", allele1, diplotype.haplotype1Phenotype, variants1);
		if(!diplotype.haplotype1Name.equals(diplotype.haplotype2Name) || !variants1.equals(variants2))
			table += String.format("  %-15s %-23s %-30s\n", allele2, diplotype.haplotype2Phenotype, variants2);

		table += "-------------------------------------------------------------------------------\n";
		return table;
	}

	private static boolean isNonNormal(Diplotype diplotype) {
		String phenotype = diplotype.diplotypePhenotype;
		return phenotype.contains("Intermediate") || phenotype.contains("Poor")
				|| phenotype.contains("Rapid") || phenotype.contains("Ultrarapid");
	}

	public static String getCancerPanelReport(GeneContent[] geneContentArray) {
		Diplotype UGT1A1Diplotype = null;
		Diplotype DPYDDiplotype = null;
		Diplotype TPMTDiplotype = null;
		Diplotype NUDT15Diplotype = null;
		Diplotype CYP2D6Diplotype = null;
		GeneContent MTHFRContent = null;

		for(GeneContent geneContent : geneContentArray) {
			if(geneContent.name.equals("UGT1A1") && !geneContent.diplotypeList.isEmpty())
				UGT1A1Diplotype = getFirstDiplotype(geneContent.diplotypeList);
			if(geneContent.name.equals("DPYD") && !geneContent.diplotypeList.isEmpty())
				DPYDDiplotype = getFirstDiplotype(geneContent.diplotypeList);
			if(geneContent.name.equals("TPMT") && !geneContent.diplotypeList.isEmpty())
				TPMTDiplotype = getFirstDiplotype(geneContent.diplotypeList);
			if(geneContent.name.equals("NUDT15") && !geneContent.diplotypeList.isEmpty())
				NUDT15Diplotype = getFirstDiplotype(geneContent.diplotypeList);
			if(geneContent.name.equals("CYP2D6") && !geneContent.diplotypeList.isEmpty())
				CYP2D6Diplotype = getFirstDiplotype(geneContent.diplotypeList);
			if(geneContent.name.equals("MTHFR"))
				MTHFRContent = geneContent;
		}

		String report = "▣ CONCLUSION AND DIAGNOSIS\n";
		report += "< Pharmacogenetics Cancer Panel >\n\n";
		report += "- RESULTS:\n";
		report += "----------------------------------------------------------------------------------\n";
		report += "  Gene       Genotype                 Phenotype\n";
		report += "----------------------------------------------------------------------------------\n";

		if(UGT1A1Diplotype != null)
			report += String.format("  UGT1A1     %-25s%s\n", UGT1A1Diplotype.name, UGT1A1Diplotype.diplotypePhenotype);
		if(DPYDDiplotype != null)
			report += String.format("  DPYD       %-25s%s\n", DPYDDiplotype.name, DPYDDiplotype.diplotypePhenotype);
		if(TPMTDiplotype != null)
			report += String.format("  TPMT       %-25s%s\n", TPMTDiplotype.name, TPMTDiplotype.diplotypePhenotype);
		if(NUDT15Diplotype != null)
			report += String.format("  NUDT15     %-25s%s\n", NUDT15Diplotype.name, NUDT15Diplotype.diplotypePhenotype);
		if(CYP2D6Diplotype != null) {
			String cyp2d6Phenotype = CYP2D6Diplotype.diplotypePhenotype;
			if(CYP2D6Diplotype.diplotypeActivityScore != null && CYP2D6Diplotype.diplotypeActivityScore.length() > 0)
				cyp2d6Phenotype += ", Activity score " + CYP2D6Diplotype.diplotypeActivityScore;
			report += String.format("  CYP2D6     %-25s%s\n", CYP2D6Diplotype.name, cyp2d6Phenotype);
		}
		String mthfrGenotype665 = "CC", mthfrGenotype1286 = "AA";
		String mthfrZygosity665 = "homozygote", mthfrZygosity1286 = "homozygote";
		String mthfrPhenotype665 = "Normal enzyme activity", mthfrPhenotype1286 = "Normal enzyme activity";
		String mthfrCombinedPhenotype = "Normal enzyme activity";
		if(MTHFRContent != null) {
			for(Variant v : MTHFRContent.variantList) {
				if(v.NTchange.equals("c.665C>T")) {
					if(v.zygosity.equals("Hom")) {
						mthfrGenotype665 = "TT";
						mthfrPhenotype665 = "Decreased enzyme activity";
					}
					else if(v.zygosity.equals("Het")) {
						mthfrGenotype665 = "CT";
						mthfrZygosity665 = "heterozygote";
						mthfrPhenotype665 = "Slightly decreased enzyme activity";
					}
				}
				if(v.NTchange.equals("c.1286A>C")) {
					if(v.zygosity.equals("Hom")) {
						mthfrGenotype1286 = "CC";
						mthfrPhenotype1286 = "Slightly decreased enzyme activity";
					}
					else if(v.zygosity.equals("Het")) {
						mthfrGenotype1286 = "AC";
						mthfrZygosity1286 = "heterozygote";
						mthfrPhenotype1286 = "Slightly decreased enzyme activity";
					}
				}
			}

			if(mthfrPhenotype665.equals("Decreased enzyme activity") || mthfrPhenotype1286.equals("Decreased enzyme activity"))
				mthfrCombinedPhenotype = "Decreased enzyme activity";
			else if(mthfrPhenotype665.equals("Slightly decreased enzyme activity") || mthfrPhenotype1286.equals("Slightly decreased enzyme activity"))
				mthfrCombinedPhenotype = "Slightly decreased enzyme activity";

			String mthfrGenotypeStr = String.format("665%c/665%c, 1286%c/1286%c",
					mthfrGenotype665.charAt(0), mthfrGenotype665.charAt(1),
					mthfrGenotype1286.charAt(0), mthfrGenotype1286.charAt(1));
			report += String.format("  MTHFR      %-25s%s\n", mthfrGenotypeStr, mthfrCombinedPhenotype);
		}

		report += "----------------------------------------------------------------------------------\n\n";

		report += "▣ COMMENTS\n";
		int sectionNum = 1;

		// 1. UGT1A1
		if(UGT1A1Diplotype != null) {
			report += sectionNum + ". UGT1A1\n";
			if(isNonNormal(UGT1A1Diplotype))
				report += getCancerAlleleTable("UGT1A1", UGT1A1Diplotype);
			report += diplotypeInterpretation("UGT1A1", UGT1A1Diplotype);
			report += "\n   UGT1A1 is a Phase II enzyme involved in the metabolism of SN-38, the active metabolite of\n";
			report += "   irinotecan. In patients with a genotype associated with reduced UGT1A1 activity, the risk of\n";
			report += "   toxicities such as myelosuppression and diarrhea increases during irinotecan therapy.\n";
			if(UGT1A1Diplotype.diplotypePhenotype.contains("Poor")) {
				report += "   In particular, for patients with a poor metabolizer genotype, a 30% dose reduction has been\n";
				report += "   recommended during irinotecan therapy, whereas dose reduction is not recommended for\n";
				report += "   intermediate metabolizers or normal metabolizers. (PMID:36443464)\n";
			}
			report += "\n";
			sectionNum++;
		}

		// 2. DPYD
		if(DPYDDiplotype != null) {
			report += sectionNum + ". DPYD\n";
			boolean dpydHasVariants = DPYDDiplotype.haplotype1Variants.length() > 0 || DPYDDiplotype.haplotype2Variants.length() > 0;
			if(isNonNormal(DPYDDiplotype) || dpydHasVariants)
				report += getCancerAlleleTable("", DPYDDiplotype);
			report += diplotypeInterpretation("DPYD", DPYDDiplotype);
			report += "\n   DPD is an enzyme involved in the catabolism of 5-fluorouracil (5-FU). In patients with a\n";
			report += "   genotype associated with reduced DPD activity, severe toxicities such as mucositis,\n";
			report += "   diarrhea, and neutropenia are more likely to occur during treatment with fluoropyrimidine\n";
			report += "   chemotherapy (5-FU, capecitabine, tegafur). (PMID:39721301)\n";
			if(isNonNormal(DPYDDiplotype)) {
				report += "   For patients with a Poor metabolizer or Intermediate metabolizer genotype, avoiding these\n";
				report += "   drugs or reducing the dose is required, and dosing decisions that consider the availability\n";
				report += "   of alternative drug therapy and the activity score are recommended. (PMID:29152729)\n";
			}
			report += "\n";
			sectionNum++;
		}

		boolean tpmtIM = TPMTDiplotype != null && TPMTDiplotype.diplotypePhenotype.contains("Intermediate");
		boolean nudt15IM = NUDT15Diplotype != null && NUDT15Diplotype.diplotypePhenotype.contains("Intermediate");
		boolean compoundIM = tpmtIM && nudt15IM;

		// 3. TPMT
		if(TPMTDiplotype != null) {
			report += sectionNum + ". TPMT\n";
			if(isNonNormal(TPMTDiplotype))
				report += getCancerAlleleTable("TPMT", TPMTDiplotype);
			report += diplotypeInterpretation("TPMT", TPMTDiplotype);
			report += "\n   TPMT is an enzyme involved in the s-methylation of azathioprine, 6-mercaptopurine, and\n";
			report += "   thioguanine. In patients with a genotype associated with reduced TPMT enzyme activity, the\n";
			report += "   risk of drug toxicity such as myelosuppression is known to increase when these drugs are\n";
			report += "   administered. (PMID:41618934)\n";
			if(isNonNormal(TPMTDiplotype)) {
				report += "\n   For Intermediate metabolizers, a dose reduction to 30-80% of the standard dose is recommended;\n";
				report += "   for Poor metabolizers, a dose reduction to 10% or less, reducing dosing frequency to three\n";
				report += "   times per week (malignant condition), or substitution with another drug (non-malignant\n";
				report += "   condition) is recommended. (PMID:41618934)\n";
			}
			report += "\n";
			sectionNum++;
		}

		// 4. NUDT15
		if(NUDT15Diplotype != null) {
			report += sectionNum + ". NUDT15\n";
			if(isNonNormal(NUDT15Diplotype))
				report += getCancerAlleleTable("NUDT15", NUDT15Diplotype);
			report += diplotypeInterpretation("NUDT15", NUDT15Diplotype);
			report += "\n   NUDT15 is an enzyme involved in the conversion of cytotoxic thioguanine triphosphate into\n";
			report += "   non-toxic thioguanine monophosphate. In patients with a genotype associated with reduced\n";
			report += "   NUDT15 enzyme activity, the risk of toxicities such as myelosuppression increases when\n";
			report += "   thiopurine drugs (azathioprine, 6-mercaptopurine, thioguanine) are administered. (PMID:41618934)\n";
			if(isNonNormal(NUDT15Diplotype)) {
				report += "\n   For Intermediate metabolizers, a dose reduction to 30-80% of the standard dose is recommended;\n";
				report += "   for Poor metabolizers, a dose reduction to 10% or less, reducing dosing frequency to three\n";
				report += "   times per week (malignant condition), or substitution with another drug (non-malignant\n";
				report += "   condition) is recommended. (PMID:41618934)\n";
			}
			if(compoundIM) {
				report += "\n   In addition, when a patient is an Intermediate metabolizer for both TPMT and NUDT15\n";
				report += "   (TPMT/NUDT15 compound IM), a dose reduction to 20-50% of the standard dose is recommended.\n";
				report += "   (PMID:41618934)\n";
			}
			report += "\n";
			sectionNum++;
		}

		// 5. CYP2D6
		if(CYP2D6Diplotype != null) {
			report += sectionNum + ". CYP2D6\n";
			report += getCancerAlleleTable("CYP2D6", CYP2D6Diplotype);
			report += diplotypeInterpretation("CYP2D6", CYP2D6Diplotype);
			report += "\n   CYP2D6 is an enzyme involved in the metabolism of tamoxifen into its active metabolite\n";
			report += "   endoxifen. When a CYP2D6 inhibitor is co-administered or a patient carries a genotype with\n";
			report += "   reduced CYP2D6 activity, decreased blood endoxifen levels and an increased risk of breast\n";
			report += "   cancer recurrence during tamoxifen therapy have been reported. (PMID: 29385237)\n";
			if(isNonNormal(CYP2D6Diplotype)) {
				report += "\n   Accordingly, for patients being treated with tamoxifen who are Poor metabolizers or\n";
				report += "   Intermediate metabolizers, switching to an alternative drug or increasing the tamoxifen dose\n";
				report += "   is recommended; for details on dose-adjustment recommendations, please refer to the CPIC\n";
				report += "   guideline (PMID: 29385237).\n";
			}
			report += "   CYP2D6 is also involved in the metabolism of ondansetron, opioids, some SSRIs (paroxetine,\n";
			report += "   fluvoxamine, vortioxetine, venlafaxine), TCAs (amitriptyline, nortriptyline), and\n";
			report += "   atomoxetine, among others. For recommendations on each drug, please refer to the CPIC\n";
			report += "   guidelines (PMID: 28002639, 33387367, 37032427, 27997040, 30801677).\n\n";
			sectionNum++;
		}

		// 6. MTHFR
		if(MTHFRContent != null) {
			report += sectionNum + ". MTHFR\n";
			report += "-------------------------------------------------------------------------------\n";
			report += "  Gene   NT change  rs number   Genotype         Phenotype\n";
			report += "-------------------------------------------------------------------------------\n";
			report += String.format("  MTHFR  c.665C>T   rs1801133   %2s %-12s  %-32s\n", mthfrGenotype665, mthfrZygosity665, mthfrPhenotype665);
			report += String.format("         c.1286A>C  rs1801131   %2s %-12s  %-32s\n", mthfrGenotype1286, mthfrZygosity1286, mthfrPhenotype1286);
			report += "-------------------------------------------------------------------------------\n";
			report += "   MTHFR is a key enzyme involved in intracellular folate homeostasis/metabolism, converting\n";
			report += "   5,10-methylenetetrahydrofolate into 5-methyltetrahydrofolate. In patients with a genotype\n";
			report += "   that reduces MTHFR enzyme activity, associations with the efficacy or adverse effects of\n";
			report += "   fluoropyrimidine (5-FU) and antifolate (methotrexate) drug therapy have been reported;\n";
			report += "   however, the level of evidence has not yet been clearly established, and no genotype-based\n";
			report += "   treatment guideline has been recommended.\n\n";
		}

		// The static <COMMENTS> and TEST INFORMATION footer is externalized to
		// resources/comments/Cancer Panel.txt and appended by RootController (see ASM Panel).

		return report;
	}

	public static String getNeuroPsychiatryPanelReport(GeneContent[] geneContentArray) {
		Diplotype CYP2B6Diplotype = null;
		Diplotype CYP2C19Diplotype = null;
		Diplotype CYP2C9Diplotype = null;
		Diplotype CYP2D6Diplotype = null;
		Diplotype CYP3A4Diplotype = null;
		Diplotype CYP3A5Diplotype = null;

		for(GeneContent geneContent : geneContentArray) {
			if(geneContent.diplotypeList.isEmpty()) continue;
			if(geneContent.name.equals("CYP2B6"))  CYP2B6Diplotype  = getFirstDiplotype(geneContent.diplotypeList);
			if(geneContent.name.equals("CYP2C19")) CYP2C19Diplotype = getFirstDiplotype(geneContent.diplotypeList);
			if(geneContent.name.equals("CYP2C9"))  CYP2C9Diplotype  = getFirstDiplotype(geneContent.diplotypeList);
			if(geneContent.name.equals("CYP2D6"))  CYP2D6Diplotype  = getFirstDiplotype(geneContent.diplotypeList);
			if(geneContent.name.equals("CYP3A4"))  CYP3A4Diplotype  = getFirstDiplotype(geneContent.diplotypeList);
			if(geneContent.name.equals("CYP3A5"))  CYP3A5Diplotype  = getFirstDiplotype(geneContent.diplotypeList);
		}

		String report = "▣ CONCLUSION AND DIAGNOSIS\n";
		report += "< Pharmacogenetics NeuroPsychiatry Panel >\n\n";
		report += "- RESULTS:\n";
		report += "----------------------------------------------------------------------------------\n";
		report += "  Gene       Genotype                 Phenotype\n";
		report += "----------------------------------------------------------------------------------\n";

		if(CYP2D6Diplotype != null) {
			String pheno = CYP2D6Diplotype.diplotypePhenotype;
			if(CYP2D6Diplotype.diplotypeActivityScore != null && CYP2D6Diplotype.diplotypeActivityScore.length() > 0)
				pheno += ", Activity score " + CYP2D6Diplotype.diplotypeActivityScore;
			report += String.format("  CYP2D6     %-25s%s\n", CYP2D6Diplotype.name, pheno);
		}
		if(CYP2C19Diplotype != null)
			report += String.format("  CYP2C19    %-25s%s\n", CYP2C19Diplotype.name, CYP2C19Diplotype.diplotypePhenotype);
		if(CYP2B6Diplotype != null)
			report += String.format("  CYP2B6     %-25s%s\n", CYP2B6Diplotype.name, CYP2B6Diplotype.diplotypePhenotype);
		if(CYP2C9Diplotype != null)
			report += String.format("  CYP2C9     %-25s%s\n", CYP2C9Diplotype.name, CYP2C9Diplotype.diplotypePhenotype);
		if(CYP3A4Diplotype != null)
			report += String.format("  CYP3A4     %-25s%s\n", CYP3A4Diplotype.name, CYP3A4Diplotype.diplotypePhenotype);
		if(CYP3A5Diplotype != null)
			report += String.format("  CYP3A5     %-25s%s\n", CYP3A5Diplotype.name, CYP3A5Diplotype.diplotypePhenotype);

		report += "----------------------------------------------------------------------------------\n\n";

		report += "▣ COMMENTS\n";
		int sectionNum = 1;

		// CYP2D6
		if(CYP2D6Diplotype != null) {
			report += sectionNum + ". CYP2D6\n";
			report += getCancerAlleleTable("CYP2D6", CYP2D6Diplotype);
			report += diplotypeInterpretation("CYP2D6", CYP2D6Diplotype);
			report += "\n";
			sectionNum++;
		}

		// CYP2C19
		if(CYP2C19Diplotype != null) {
			report += sectionNum + ". CYP2C19\n";
			if(isNonNormal(CYP2C19Diplotype))
				report += getCancerAlleleTable("CYP2C19", CYP2C19Diplotype);
			report += diplotypeInterpretation("CYP2C19", CYP2C19Diplotype);
			report += "\n";
			sectionNum++;
		}

		// CYP2B6
		if(CYP2B6Diplotype != null) {
			report += sectionNum + ". CYP2B6\n";
			if(isNonNormal(CYP2B6Diplotype))
				report += getCancerAlleleTable("CYP2B6", CYP2B6Diplotype);
			report += diplotypeInterpretation("CYP2B6", CYP2B6Diplotype);
			report += "\n";
			sectionNum++;
		}

		// CYP2C9
		if(CYP2C9Diplotype != null) {
			report += sectionNum + ". CYP2C9\n";
			if(isNonNormal(CYP2C9Diplotype))
				report += getCancerAlleleTable("CYP2C9", CYP2C9Diplotype);
			report += diplotypeInterpretation("CYP2C9", CYP2C9Diplotype);
			report += "\n";
			sectionNum++;
		}

		// CYP3A4
		if(CYP3A4Diplotype != null) {
			report += sectionNum + ". CYP3A4\n";
			if(isNonNormal(CYP3A4Diplotype))
				report += getCancerAlleleTable("CYP3A4", CYP3A4Diplotype);
			report += diplotypeInterpretation("CYP3A4", CYP3A4Diplotype);
			report += "\n";
			sectionNum++;
		}

		// CYP3A5
		if(CYP3A5Diplotype != null) {
			report += sectionNum + ". CYP3A5\n";
			if(isNonNormal(CYP3A5Diplotype))
				report += getCancerAlleleTable("CYP3A5", CYP3A5Diplotype);
			report += diplotypeInterpretation("CYP3A5", CYP3A5Diplotype);
			report += "\n";
			sectionNum++;
		}

		// Static <COMMENTS> and TEST INFORMATION footer is externalized to
		// resources/comments/NeuroPsychiatry Panel.txt and appended by RootController.

		return report;
	}

	public static String diplotypeInterpretation(String gene, Diplotype diplotype) {
		String ret = "";
		/*
		 * 1. wild type
		 * 2. normal enzyme function
		 * 3. Others
		 */

		//No variant means wild type. Even with a variant, *1 is regarded as wild type (e.g., CYP2C19).
		boolean wildtype1 =diplotype.haplotype1Variants.length() == 0 || diplotype.haplotype1Name.equals("*1");
		boolean wildtype2 =diplotype.haplotype2Variants.length() == 0 || diplotype.haplotype2Name.equals("*1");

		ret += "   This patient is ";

		if(wildtype1 && wildtype2) {
			ret += "a Normal Metabolizer carrying a normal genotype.\n";
		}

		else if(diplotype.diplotypePhenotype.equals("Normal Metabolizer (NM)")) {
			ret += "a Normal Metabolizer carrying a genotype with normal enzyme function.\n";
		}

		else if(!wildtype1 && wildtype2) {

			ret += "a heterozygote for " + gene + diplotype.haplotype1Name + " (an allele " + enzymeActivity(diplotype.haplotype1Phenotype) + "),\n";
			ret += "   corresponding to " + diplotype.diplotypePhenotype + ".\n";

		}
		else if(wildtype1 && !wildtype2) {
			ret += "a heterozygote for " + gene + diplotype.haplotype2Name + " (an allele " + enzymeActivity(diplotype.haplotype2Phenotype) + "),\n";
			ret += "   corresponding to " + diplotype.diplotypePhenotype + ".\n";
		}

		else if(!wildtype1 && !wildtype2) {
			if(diplotype.haplotype1Name.equals(diplotype.haplotype2Name)) { //homozygous
				ret += "a homozygote for " + gene + diplotype.haplotype1Name + " (an allele " + enzymeActivity(diplotype.haplotype1Phenotype) + "),\n";
				ret += "   corresponding to " + diplotype.diplotypePhenotype + ".\n";
			}
			else {
				if(diplotype.haplotype1Phenotype.equals(diplotype.haplotype2Phenotype)) {
					ret += "a compound heterozygote for " + gene + diplotype.haplotype1Name + " and ";
					ret += gene + diplotype.haplotype2Name + " (alleles " + enzymeActivity(diplotype.haplotype1Phenotype) + "),\n";
					ret += "   corresponding to " + diplotype.diplotypePhenotype + ".\n";
				}
				else {
					ret += "a heterozygote for " + gene + diplotype.haplotype1Name + " (an allele " + enzymeActivity(diplotype.haplotype1Phenotype) + ")\n";
					ret += "   and a heterozygote for " + gene + diplotype.haplotype2Name + " (an allele " + enzymeActivity(diplotype.haplotype2Phenotype) + "),\n";
					ret += "   corresponding to " + diplotype.diplotypePhenotype + ".\n";
				}
			}
		}
		
		
		
		return ret;
	}
	
	
	
	/**
	 * CYP4F2 single-gene report. CYP4F2 has no star-allele functionality/diplotype-
	 * phenotype tables; its actionable effect is on warfarin dose requirement via the
	 * p.Val433Met (V433M, *3, rs2108622) variant, so the report is built around that
	 * variant rather than the generic star-allele phenotype path.
	 */
	public static String getCYP4F2Report(GeneContent geneContent) {
		String V433M = V433M_detected(geneContent.variantList);
		boolean reduced = V433M.equals("Hom") || V433M.equals("Het");
		String genotype = reduced ? ("p.Val433Met detected (" + V433M + ")") : "p.Val433Met not detected";
		String phenotype = reduced ? "Reduced activity" : "Normal activity";

		String report = "▣ CONCLUSION AND DIAGNOSIS\n\n" +
				"< CYP4F2 Genotyping>\n\n" +
				"- RESULTS:\n" +
				"------------------------------------------------------------\n" +
				"  CYP4F2 genotype                Deduced phenotype\n" +
				"------------------------------------------------------------\n";
		report += String.format("   %-30s %-20s\n", genotype, phenotype);
		report += "------------------------------------------------------------\n\n";

		report += "▣ COMMENTS\n\n- INTERPRETATION\n";
		if(reduced) {
			report += "   This patient carries the CYP4F2 p.Val433Met (V433M, *3, rs2108622) variant (" + V433M + "),\n";
			report += "   which is associated with reduced CYP4F2 enzyme activity and a modestly increased warfarin\n";
			report += "   dose requirement. CYP4F2 genotype is interpreted together with CYP2C9 and VKORC1 in\n";
			report += "   warfarin dosing algorithms. Refer to the CPIC warfarin guideline.\n";
		}
		else {
			report += "   The CYP4F2 p.Val433Met (V433M, *3, rs2108622) variant was not detected; CYP4F2 activity\n";
			report += "   is expected to be normal with respect to warfarin dose requirement.\n";
		}
		return report;
	}

	public static String getSingleGeneReport(GeneContent geneContent) {
		if(geneContent.diplotypeList.isEmpty())
			return noGenotypeText;

		String gene = geneContent.name;
		if(gene.equals("MTHFR"))
			return getMTHFRReport(geneContent, false);
		if(gene.equals("CYP4F2"))
			return getCYP4F2Report(geneContent);

		Diplotype diplotype = getFirstDiplotype(geneContent.diplotypeList);
		String report = "▣ CONCLUSION AND DIAGNOSIS\n\n" + 
				"< " + gene + " Genotyping>\n\n" + 
				"- RESULTS:\n" + 
				"------------------------------------------------------------\n" + 
				"  " + gene + " genotype      Deduced phenotype\n" + 
				"------------------------------------------------------------\n";
		report += String.format("   %-17s   %-30s\n", diplotype.name, diplotype.diplotypePhenotype);

		report += "------------------------------------------------------------\n";

		report += "-------------------------------------------------------------------------------\n" + 
				"  Allele     Functional status   Variants         \n" + 
				"-------------------------------------------------------------------------------\n";

		String variants1 = diplotype.haplotype1Variants;
		if(variants1.length()==0)
			variants1 = "(-)";
		String variants2 = diplotype.haplotype2Variants;
		if(variants2.length()==0)
			variants2 = "(-)";


		report += String.format("   %-7s   %-20s%-20s\n", diplotype.haplotype1Name, diplotype.haplotype1Phenotype, variants1);
		if(!diplotype.haplotype1Name.equals(diplotype.haplotype2Name) || !variants1.equals(variants2))  
			report += String.format("   %-7s   %-20s%-20s\n", diplotype.haplotype2Name, diplotype.haplotype2Phenotype, variants2);
		report += "-------------------------------------------------------------------------------\n\n";
		report += "▣ COMMENTS\n\n" + 
				"- INTERPRETATION\n";

		report += diplotypeInterpretation(gene, diplotype);


		if(gene.equals("NUDT15")) {

			if(diplotype.name.equals("*1/*2")) {
				report += "\n";
				report += "   Note that for *1/*2, the possibility of *3/*6 (Possible Intermediate Metabolizer), a compound\n"
						+ "   heterozygote of the c.50_55dup and c.415C>T variants, cannot be excluded.\n";
			}

			if(diplotype.name.contains("*4")) {
				report += "\n";
				report += "   Note that studies have reported reduced NUDT15 enzyme activity in the presence of the *4\n" +
						"   allele. (Ref. Nat Genet. 2016 Apr;48(4):367-73) \n";
			}

			if(diplotype.name.contains("*5")) {
				report += "\n";
				report += "   Note that studies have reported reduced NUDT15 enzyme activity in the presence of the *5\n" +
						"   allele. (Ref. Nat Genet. 2016 Apr;48(4):367-73) \n";
			}

			report += "\n";
			report += "   Note that responsiveness to thiopurine drugs is determined by two genes, TPMT and NUDT15;\n"
					+ "   additional testing of the TPMT genotype is recommended.\n";

		}

		if(gene.equals("TPMT")) {
			if(diplotype.diplotypePhenotype.equals("Indeterminate")) {
				report += "\n";
				report += "   Thiopurine metabolite monitoring is recommended when administering thiopurine drugs.\n";
			}
			report += "\n";
			report += "   Note that responsiveness to thiopurine drugs is determined by two genes, TPMT and NUDT15;\n"
					+ "   additional testing of the NUDT15 genotype is recommended.\n";

		}


		report += "\n";

		String haplotypeFreqComment = "";
		haplotypeFreqComment += "The most common " + gene + " genotypes in East Asians are ";

		int cnt = 1;
		Vector<HaplotypeFreq> hfList = geneContent.geneMetaData.hfList;
		for(HaplotypeFreq hf : hfList) {
			if(cnt > topN) break;

			haplotypeFreqComment += String.format("%s (%.1f%%)",  hf.name, hf.freq);
			if(cnt<topN && cnt<hfList.size())
				haplotypeFreqComment += ", ";
			cnt++;
		}
		haplotypeFreqComment += ".\n";

		/*
		report += StringFormatter(haplotypeFreqComment);
		report += "\n";


		report += StringFormatter(databaseComment);
		report += "\n";
		 */

		return report;
	}

	public static String getDPYD_shortReport(GeneContent geneContent) {
		if(geneContent.diplotypeList.isEmpty())
			return noGenotypeText;

		String gene = geneContent.name;

		Diplotype diplotype = getFirstDiplotype(geneContent.diplotypeList);
		String report = "\n\n<Addendum Report: DPYD genotyping>\n\n" + 
				"------------------------------------------------------------\n" + 
				gene + " genotype                  Deduced phenotype\n" + 
				"------------------------------------------------------------\n";
		report += String.format("%-28s  %-30s\n", diplotype.name, diplotype.diplotypePhenotype);

		report += "------------------------------------------------------------\n";


		report += "-------------------------------------------------------------------------------\n" + 
				"Allele          Functional status  Variants         \n" + 
				"-------------------------------------------------------------------------------\n";

		String variants1 = diplotype.haplotype1Variants;
		if(variants1.length()==0)
			variants1 = "(-)";
		String variants2 = diplotype.haplotype2Variants;
		if(variants2.length()==0)
			variants2 = "(-)";


		report += String.format("%-15s %-15s    %-20s\n", diplotype.haplotype1Name, diplotype.haplotype1Phenotype, variants1);
		if(!diplotype.haplotype1Name.equals(diplotype.haplotype2Name))  
			report += String.format("%-15s %-15s    %-20s\n", diplotype.haplotype2Name, diplotype.haplotype2Phenotype, variants2);
		report += "-------------------------------------------------------------------------------\n\n";
		report += "- INTERPRETATION\n"; 
		
		report += diplotypeInterpretation(gene, diplotype);
		
		Vector<Variant> additionalVariantList = new Vector<Variant>();
		for(Variant v : geneContent.variantList) {
			if(v.included.equals("X"))
				additionalVariantList.add(v);
		}


		if(!additionalVariantList.isEmpty()) {
			report += "\n";
			report += "- ADDITIONAL VARIANTS\n"; 
			report += "==================================================================================\r\n" + 
					"Gene      RefSeq         NT change         AA change           VAF(%)  Depth(X)\r\n" + 
					"----------------------------------------------------------------------------------\n";
			for(Variant v: additionalVariantList) {
				report += String.format("%-10s%-15s%-18s%-18s%8s%7s\n", v.gene, v.refseq, v.NTchange, v.AAchange, v.VAF, v.depth);
			}

			report += "==================================================================================\r\n";
		}

		return report;
	}

	/**
	 * 
	 * @param text : Text to be formatted
	 * @param width : Width of formatted text
	 * @return
	 */
	private static String StringFormatter(String text) {
		String ret = "";

		String[] tokens = text.split("[\n\r\t ]");
		int lineCharCount = 0;
		String line = "  ";
		for(int i=0;i<tokens.length;i++) {
			int tokenLength = 0;
			//Count Korean (multi-byte) characters as 2 bytes.
			for(int j=0;j<tokens[i].length();j++) {
				if(tokens[i].charAt(j) > 10000) 
					tokenLength += 2;
				else
					tokenLength += 1;
			}

			if(lineCharCount + tokenLength > reportWidth) {
				ret += line + "\n  ";
				line = tokens[i] + " ";
				lineCharCount = tokenLength + 1;
			}
			else {
				line += tokens[i] + " ";
				lineCharCount += tokenLength + 1;
			}
		}
		ret += line + "\n";

		return ret;
	}

}
