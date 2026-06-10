package snackpgx;

import java.util.TreeSet;
import java.util.Vector;

public class Sample {
	private final static int candidateCount = 5;
	private final static double scoreCutoff = 0.5;

	public String sampleID = "";
	public String path = "";
	public String reportText = "";
	public boolean filesExist = false;
	private RootController rootController;

	public GeneContent[] geneContentArray = null;
	public int geneCount = 0;
	// Set during NUDT15 normalization (dup Hom + R139C Het) so *3/*6 is preferred post-diplotyping.
	private boolean nudt15PreferStar3Star6 = false;


	public Sample(String sampleID, String path, RootController rootController) {
		try {
			this.sampleID = sampleID;
			this.path = path;
			this.rootController = rootController;
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}


		geneCount = rootController.geneMetaDataList.size();
		geneContentArray = new GeneContent[geneCount];
		int cnt = 0;
		for(GeneMetaData geneMeta : rootController.geneMetaDataList) {
			GeneContent geneContent = new GeneContent(geneMeta);
			geneContentArray[cnt++] = geneContent;
		}
		readAnnotationFile();
		diplotyping();
		if(nudt15PreferStar3Star6)
			for(GeneContent gc : geneContentArray)
				if(gc.name.equals("NUDT15")) gc.preferDiplotype("*3/*6");
		printResult();

	}


	private String getInputSettings(String key, String[] tokens) {
		try {
			return tokens[rootController.inputColumnMap.get(key)];
		} catch (Exception e) {
			return "N/A";
		}
	}
	
	

	private void readAnnotationFile() {
		try {
			String annoFile = rootController.readFile(path);
			//System.out.println(annoFile);
			String[] annoFileLines = annoFile.split("\n");
			
			Integer headerRowObj = rootController.inputColumnMap.get("Header row");
			if(headerRowObj == null || headerRowObj < 0) {
				rootController.inputTypeError = true;
				return;
			}
			int headerRow = headerRowObj;

			for(int i=headerRow+1; i<annoFileLines.length;i++) {
				String[] tokens = annoFileLines[i].split("\t");
				if(tokens.length<1)
					continue;

				String gene, indexID, rsID, transcript, NTchange, AAchange, zygosity, depth, VAF, GQ, strandBias, QUAL, AF;
				gene    = getInputSettings("gene", tokens);
				indexID    = getInputSettings("indexID", tokens);
				rsID       = getInputSettings("rsID", tokens);
				transcript = getInputSettings("transcript", tokens);
				NTchange   = getInputSettings("NTchange", tokens);
				AAchange   = getInputSettings("AAchange", tokens);
				zygosity   = getInputSettings("zygosity", tokens);
				
				depth      = getInputSettings("depth", tokens);
				if(!RootController.isDouble(depth)) {
					rootController.inputTypeError = true;
					break;
				}
				
				VAF        = getInputSettings("VAF", tokens);
				if (!RootController.isDouble(VAF)) {
					rootController.inputTypeError = true;
					break;
				}
				
				GQ         = getInputSettings("GQ", tokens);
				strandBias = getInputSettings("strandBias", tokens);
				
				QUAL       = getInputSettings("QUAL", tokens);
				if (!RootController.isDouble(QUAL)) {
					rootController.inputTypeError = true;
					break;
				}
				
				AF         = getInputSettings("AF", tokens);
				
				
				GeneMetaData geneMetaData = null;
				for(GeneMetaData geneMeta : rootController.geneMetaDataList) {
					if(geneMeta.name.equals(gene))
						geneMetaData = geneMeta;
				}

				int geneIndex = -1;
				for(geneIndex=0;geneIndex<geneCount;geneIndex++) {
					if(geneContentArray[geneIndex].name.equals(gene))
						break;
				}

				//Skip if the gene is not a target.
				if(geneMetaData==null)
					continue;

				String starAlleles = "";
				//String[] indexID_tokens = tokens[6].split("-");
				//String key = indexID_tokens[0] + "-" + indexID_tokens[1] + "-" + indexID_tokens[3];
				starAlleles = geneMetaData.variantToAlleleMap.get(indexID);
				if(starAlleles == null)
					starAlleles = "";

				
				Variant variant = new Variant(gene, indexID, rsID, transcript, NTchange, AAchange, zygosity, depth, VAF, GQ, strandBias, QUAL, starAlleleFormatter(starAlleles), AF);

				if(variant.passFilter())
					geneContentArray[geneIndex].addVariant(variant);
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			rootController.inputTypeError = true;
		}
		normalizeNUDT15();
		for(GeneContent geneContent : geneContentArray)
			geneContent.updateIncludedVariantList();
	}

	/**
	 * NUDT15 dosage normalization (allele-definition table left unmodified).
	 *
	 * The GAGTCG(4) duplication (c.50_55dupGAGTCG, rs869320766) is, in current PharmVar
	 * nomenclature, part of the *3 GAGTCG(4) sub-haplotype that also carries R139C
	 * (c.415C>T, rs116855232) in cis. When both variants are present, the diplotype caller
	 * (set-based Jaccard) cannot tell *1/*3 from *3/*3 and reports *3/*3. Because each *3
	 * allele (counted by R139C dosage) already accounts for one GAGTCG(4), we subtract the
	 * R139C copy number from the GAGTCG(4) copy number so the residual variant set resolves
	 * correctly:
	 *   - both Het              -> remove the dup            -> {R139C het}        -> *1/*3
	 *   - both Hom              -> remove the dup            -> {R139C hom}        -> *3/*3
	 *   - dup Hom, R139C Het    -> demote the dup to Het     -> {dup het,R139C het}-> *3/*6
	 *   - dup Het, R139C Hom    -> remove the dup            -> {R139C hom}        -> *3/*3
	 */
	private void normalizeNUDT15() {
		for(GeneContent gc : geneContentArray) {
			if(!gc.name.equals("NUDT15")) continue;
			Variant dup = null, r139c = null;
			for(Variant v : gc.variantList) {
				if(!v.included.equals("O")) continue;            // allele-defining (mapped) only
				String nt = (v.NTchange == null) ? "" : v.NTchange;
				if(nt.contains("50_55dup")) dup = v;             // GAGTCG(4) duplication
				else if(nt.equals("c.415C>T")) r139c = v;        // R139C
			}
			if(dup == null || r139c == null) return;             // rule applies only when both present
			if(dup.zygosity.equals("Hom") && r139c.zygosity.equals("Het")) {
				// One GAGTCG(4) is the *3 GAGTCG(4) sub (in cis with R139C); the other is a
				// separate *6 -> *3/*6. Demote the dup to Het so *3/*6 (and *3/*3) score 1.0,
				// then prefer *3/*6 over the tied *3/*3 after diplotyping.
				dup.zygosity = "Het";
				nudt15PreferStar3Star6 = true;
			} else {
				// dup fully accounted for by *3: both Het -> {R139C het} -> *1/*3;
				// both Hom / (dup Het + R139C Hom) -> {R139C hom} -> *3/*3.
				gc.variantList.remove(dup);
			}
			return;
		}
	}

	private boolean AUPAC_compare(String a, String b) {
		boolean ret = false;
		if(a.equals("N"))
			return true;
		if(a.equals("M"))
			return "MAC".contains(b);
		if(a.equals("R"))
			return "RAG".contains(b);
		if(a.equals("W"))
			return "WAT".contains(b);
		if(a.equals("S"))
			return "SCG".contains(b);
		if(a.equals("Y"))
			return "YCT".contains(b);
		if(a.equals("K"))
			return "KGT".contains(b);
		if(a.equals("V"))
			return !("T".equals(b));
		if(a.equals("H"))
			return !("G".equals(b));
		if(a.equals("D"))
			return !("C".equals(b));
		if(a.equals("B"))
			return !("A".equals(b));

		return ret;

	}

	//True if the alt allele of this variant key is a single IUPAC ambiguity code (e.g. chr19-41009358-A-R).
	private boolean isAmbiguityAlt(String variantKey) {
		String[] tokens = variantKey.split("-");
		if(tokens.length < 4) return false;
		String alt = tokens[3];
		return alt.length() == 1 && "MRWSYKVHDBN".contains(alt);
	}




	/**
	 * @param target : list of detected variants
	 * @param candidate : list of variants of the candidate diplotype.
	 * @return the number of variants in the intersection
	 *
	 * Complex genotype resolution
	 * : read variants from target one by one and assign each to firstTarget, then count how many variants in candidate match firstTarget.
	 * Since candidate is built from two star alleles, there can be up to two variants matching firstTarget.
	 * In that case, if at least one of the two variants is a simple variant (does not contain a complex symbol such as RWSY), match it; complex variants are more useful, so this is a kind of greedy approach.
	 * If both variants are complex, it is unknown which choice will ultimately yield a better score, so explore both via recursive calls and pick the higher one.
	 */

	private int countIntersection(Vector<String> target, Vector<String> candidate) {
		if(target.isEmpty() || candidate.isEmpty())
			return 0;


		String firstTarget = target.get(0);

		int matchCount = 0;
		Vector<String> matchType = new Vector<String>();
		Vector<Integer> matchIndex = new Vector<Integer>(); 


		for(int i=0;i<candidate.size();i++) {
			String ci = candidate.get(i);
			if(firstTarget.equals(ci)) {
				matchCount++;
				matchType.add("simple");
				matchIndex.add(new Integer(i));
			}
			else {
				String head1 = firstTarget.substring(0, firstTarget.length()-1);
				String last1 = firstTarget.substring(firstTarget.length()-1);

				String head2 = ci.substring(0, ci.length()-1);
				String last2 = ci.substring(ci.length()-1);

				if(head1.equals(head2) && AUPAC_compare(last2, last1)) {
					//System.out.println(String.format("head1, last1 : %s, %s",  head1,  last1));
					//System.out.println(String.format("head2, last2 : %s, %s",  head2,  last2));
					matchCount++;
					matchType.add("complex");
					matchIndex.add(new Integer(i));
				}
			}
		}

		//if(matchCount>2) System.out.println("Match count : " + matchCount);

		target.remove(0);
		if(matchCount == 0) {
			return countIntersection(target, candidate); 
		}
		else if(matchCount == 1) {
			candidate.remove(matchIndex.get(0).intValue());
			return 1 + countIntersection(target, candidate);
		}
		else { //matchCount == 2
			if(matchType.get(0).equals("simple")) {
				candidate.remove(matchIndex.get(0).intValue());
				return 1 + countIntersection(target, candidate);
			}
			else if(matchType.get(1).equals("simple")) {
				candidate.remove(matchIndex.get(1).intValue());
				return 1 + countIntersection(target, candidate);
			}
			else {
				Vector<String> candidate2 = new Vector<String>(candidate);
				candidate.remove(matchIndex.get(0).intValue());
				candidate2.remove(matchIndex.get(1).intValue());

				int score1 = 1 + countIntersection(target, candidate);
				int score2 = 1 + countIntersection(target, candidate2);

				return Math.max(score1,  score2);
			}
		}
	}


	private double getScore(DiplotypeMeta candidate, Vector<Variant> targetVariantList) {
		Vector<String> targetVariants = new Vector<String>();
		for(Variant variant : targetVariantList) 
			targetVariants.add(variant.indexID);

		Vector<String> candidates = new Vector<String>();

		for(String candidateVariant : candidate.variants) {
			String[] tokens = candidateVariant.split("-");
			String ref = tokens[2];
			String alt = tokens[3];
			//System.out.println(String.format("ref : %s, alt : %s",  tokens[2], tokens[3]));

			boolean remove = false;	//Keep it if alt is a symbol that does not contain ref.

			//If ref is a complex symbol that contains alt, it is equivalent to no variant.
			if(AUPAC_compare(ref, alt)) {
				remove = true;
			}
			//If alt is a complex symbol that contains ref, a variant may exist depending on alt. Keep it if an actual variant was called at that position.
			else if(AUPAC_compare(alt, ref)) {
				remove = true;
				for(String targetVariant : targetVariants) {
					String head1 = targetVariant.substring(0, targetVariant.length()-1);
					String head2 = candidateVariant.substring(0, candidateVariant.length()-1);
					if(head1.equals(head2)) {
						remove = false;
						break;
					}
				}
			}

			//Permissive IUPAC match: if this surviving key is an ambiguity-coded alt and an
			//explicit concrete-base variant already covers the observed copies at the same
			//position, drop the redundant ambiguity copy. Otherwise it inflates both the
			//required-variant count and the union, penalizing an otherwise-perfect diplotype
			//(e.g. CYP2B6 *6/*18 vs *9/*18 at chr19-41009358 K262R). The explicitCount >=
			//observedCount guard keeps the ambiguity copy whenever it is genuinely needed to
			//cover an extra observed copy (e.g. homozygous positions).
			if(!remove && isAmbiguityAlt(candidateVariant)) {
				String head = candidateVariant.substring(0, candidateVariant.length()-1);
				int explicitCount = 0;
				for(String other : candidate.variants) {
					if(other.equals(candidateVariant)) continue;
					String otherHead = other.substring(0, other.length()-1);
					if(otherHead.equals(head) && !isAmbiguityAlt(other)) {
						String[] ot = other.split("-");
						if(ot.length >= 4 && !ot[2].equals(ot[3])) explicitCount++;
					}
				}
				int observedCount = 0;
				for(String targetVariant : targetVariants) {
					String targetHead = targetVariant.substring(0, targetVariant.length()-1);
					if(targetHead.equals(head)) observedCount++;
				}
				if(explicitCount >= 1 && explicitCount >= observedCount) {
					remove = true;
				}
			}

			if(!remove) {
				candidates.add(candidateVariant);
			}
		}

		int a = candidates.size();
		int b = targetVariants.size();

		int intersection = countIntersection(targetVariants, candidates);
		int union = a+b-intersection;
		if(union == 0) return 1.0;

		double score = intersection / (double)union;
		return score;
	}

	public void diplotyping(int geneIndex, boolean reset) {
		Diplotype[] tops = new Diplotype[candidateCount];
		double[] scores = new double[candidateCount];
		for(int j=0;j<candidateCount;j++)
			scores[j]=-1;

		GeneMetaData geneMeta = rootController.geneMetaDataList.get(geneIndex);

		if(reset) {
			geneContentArray[geneIndex].resetDiplotypeList();
		}

		for(DiplotypeMeta candidate : geneMeta.diplotypeMetaList) {
			double score = getScore(candidate, geneContentArray[geneIndex].includedVariantList);

			//If the score exceeds the cutoff, split into two haplotypes and use that information to create a Diplotype object.
			if(score >=scoreCutoff) {
				String haplotype1Variants = "";
				String haplotype2Variants = "";
				TreeSet<String> dedup1 = new TreeSet<String>();
				TreeSet<String> dedup2 = new TreeSet<String>();

				for (Variant variant : geneContentArray[geneIndex].includedVariantList) { 
					if(candidate.haplotype1VariantList.contains(variant.indexID))
						dedup1.add(variant.NTchange + " (" + variant.AAchange + ")");

					if(candidate.haplotype2VariantList.contains(variant.indexID))
						dedup2.add(variant.NTchange + " (" + variant.AAchange + ")");
				}

				for (String variant : dedup1)
					haplotype1Variants += variant + "; ";
				if(haplotype1Variants.length()>0)
					haplotype1Variants = haplotype1Variants.substring(0, haplotype1Variants.length()-2);


				for (String variant : dedup2)
					haplotype2Variants += variant + "; ";
				if(haplotype2Variants.length()>0)
					haplotype2Variants = haplotype2Variants.substring(0, haplotype2Variants.length()-2);


				Diplotype diplotype = new Diplotype(candidate, haplotype1Variants, haplotype2Variants, score);
				geneContentArray[geneIndex].addDiplotype(diplotype);

			}
		}
		geneContentArray[geneIndex].sortDiplotypeList();

	}


	private void diplotyping() {
		for(int i=0;i<geneCount;i++) {
			diplotyping(i, false);
		}
	}

	private void printResult() {
		//System.out.println("Make Report called");
		String printText = "";
		for(int i=0;i<geneCount;i++) {
			printText += String.format("%s\t%s\t",  sampleID, geneContentArray[i].name);
			Vector<Diplotype> diplotypeList = geneContentArray[i].diplotypeList;
			if(diplotypeList.size()==0) {
				printText += String.format("%s\n",  "NA");
				continue;
			}
			Diplotype firstDiplotype = diplotypeList.get(0);
			for(Diplotype diplotype : diplotypeList) {
				printText += String.format("%s (%.2f); ", diplotype.name, diplotype.score);
			}
			printText += "\n";
		}
		System.out.print(printText);
	}

	public String getBatchReport() {
		String ret = "";
		ret += String.format("%s\t",  sampleID);
		for(int i=0;i<geneCount;i++) {
			Vector<Diplotype> diplotypeList = geneContentArray[i].diplotypeList;
			if(diplotypeList.size()==0) {
				ret += String.format("%s\t",  "NA");
				continue;
			}

			Diplotype firstDiplotype = diplotypeList.get(0);
			for(Diplotype diplotype : diplotypeList) {
				if(diplotype.score == firstDiplotype.score) {
					if (diplotype.score != 1.0) 
						ret += String.format("%s (%.2f); ", diplotype.name, diplotype.score);
					else 
						ret += String.format("%s; ", diplotype.name);
				}
				else
					break;
			}
			ret = ret.substring(0, ret.length()-2);
			ret += "\t";

		}
		return ret;
	}

	public String getReport(String panelName) {
		String ret = "Please choose target test first";

		if(panelName.equals("HealthCheckup")) {
			ret = ReportGenerator.getHealthCheckUpReport(geneContentArray);
		}

		else if(panelName.equals("Cancer Panel")) {
			ret = ReportGenerator.getCancerPanelReport(geneContentArray);
		}

		else if(panelName.equals("NeuroPsychiatry Panel")) {
			ret = ReportGenerator.getNeuroPsychiatryPanelReport(geneContentArray);
		}

		else if(panelName.equals("Cardiology Panel")) {
			ret = ReportGenerator.getCardioPanelReport(geneContentArray, rootController.getPanelIndex("Cardiology"));
		}

		else if(panelName.equals("ASM Panel")) {
			ret = ReportGenerator.getASMPanelReport(geneContentArray);
		}

		else if(panelName.equals("DPYD_short")) {
			int targetGeneIndex = 0;
			for(targetGeneIndex=0;targetGeneIndex<geneCount;targetGeneIndex++) {
				if(geneContentArray[targetGeneIndex].name.equals("DPYD"))
					break;
			}
			ret = ReportGenerator.getDPYD_shortReport(geneContentArray[targetGeneIndex]);
		}


		else  { //Single gene
			int targetGeneIndex = 0;
			for(targetGeneIndex=0;targetGeneIndex<geneCount;targetGeneIndex++) {
				if(geneContentArray[targetGeneIndex].name.equals(panelName))
					break;
			}
			ret = ReportGenerator.getSingleGeneReport(geneContentArray[targetGeneIndex]);
		}
		return ret;
	}


	private String starAlleleFormatter(String value) {
		String ret = value;
		if(value.length()>2 && value.substring(value.length()-2, value.length()).equals(", "))
			ret = value.substring(0, value.length()-2);
		return ret;
	}
}
