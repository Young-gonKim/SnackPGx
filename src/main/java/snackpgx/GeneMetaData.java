package snackpgx;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class GeneMetaData {
	public String name;
	public String chromosome;
	public String transcript;
	public int GRCh38RowNo;
	public int referenceRowNo;
	public Vector<HaplotypeFreq> hfList = new Vector<HaplotypeFreq>();

	public TreeMap<String, String> variantToAlleleMap = new TreeMap<String, String>();
	public TreeMap<String, Vector<String>> alleleToVariantMap = new TreeMap<String, Vector<String>>();
	public Vector<DiplotypeMeta> diplotypeMetaList = new Vector<DiplotypeMeta>();

	public TreeMap<String, String> diplotypeFrequencyMap = new TreeMap<String, String>();
	public TreeMap<String, String> diplotypeActivityScoreMap = new TreeMap<String, String>();
	public TreeMap<String, String> diplotypePhenotypeMap = new TreeMap<String, String>();

	public TreeMap<String, String> haplotypeFrequencyMap = new TreeMap<String, String>();
	public TreeMap<String, String> haplotypeActivityScoreMap = new TreeMap<String, String>();
	public TreeMap<String, String> haplotypePhenotypeMap = new TreeMap<String, String>();

	public double[] topHaplotypeFreq;
	public String[] topHaplotypeFreqAllele;





	//Even when targeted is false, do all the processing but just do not display it.
	public boolean targeted = true;
	public boolean[] panelInclusionList = null;



	public GeneMetaData(String name, String chromosome, String transcript, int GRCh38RowNo, int referenceRow,
			boolean[] panelInclusionList) {
		super();
		this.name = name;
		this.chromosome = chromosome;
		this.transcript = transcript;
		this.GRCh38RowNo = GRCh38RowNo;
		this.referenceRowNo = referenceRow;

		this.panelInclusionList = panelInclusionList;

		//Build the various maps.
		readDiplotypePhenotypeFile();
		//readPhenotypeTable();
		readHaplotypePhenotypeFile();
		readFrequencyFile();
		readAlleleDefTable();
		

		/*
		Set keySet = frequencyMap.keySet();
		Iterator<String> iter = keySet.iterator();
		while(iter.hasNext()) {
			String key = iter.next();
			System.out.println(String.format("%s : %s",  key, frequencyMap.get(key)));
		}
		 */
	}


	public String getReversedDiplotype (String diplotype) {
		String[] tokens = diplotype.split("/");
		return tokens[1] + "/" + tokens[0];
	}

	// ---- Header/label-based table introspection -------------------------------
	// Columns and sheets are located by their header text rather than by numbers
	// configured in settings.xlsx, so freshly downloaded ClinPGx tables work
	// without manual column re-mapping.

	/** Returns the 1-based column index in headerRowNo (1-based) whose header
	 *  contains every given substring (case-insensitive), or 0 if none. */
	private int findColumnByHeader(XSSFSheet sheet, int headerRowNo, String... substrings) {
		if(sheet == null) return 0;
		XSSFRow row = sheet.getRow(headerRowNo-1);
		if(row == null) return 0;
		for(int c=0; c<row.getLastCellNum(); c++) {
			XSSFCell cell = row.getCell(c);
			if(cell == null) continue;
			String header;
			try { header = cell.getStringCellValue().trim(); }
			catch(Exception ex) { continue; }
			String lower = header.toLowerCase();
			boolean all = true;
			for(String s : substrings) {
				if(!lower.contains(s.toLowerCase())) { all = false; break; }
			}
			if(all) return c+1;
		}
		return 0;
	}

	/** Reads a cell as a trimmed String regardless of its underlying type
	 *  (text or numeric). Returns "" for null cells. Numeric values keep an
	 *  integer look when whole (e.g. 2.0 -> "2") to match ClinPGx notation. */
	private String cellToString(XSSFCell cell) {
		if(cell == null) return "";
		try {
			String v = cell.getStringCellValue();
			return v == null ? "" : v.trim();
		}
		catch(IllegalStateException ex) {
			double d = cell.getNumericCellValue();
			if(d == Math.floor(d) && !Double.isInfinite(d))
				return String.valueOf((long)d);
			return String.valueOf(d);
		}
	}

	/** Returns the first sheet whose name contains every given substring
	 *  (case-insensitive), or null if none. */
	private XSSFSheet findSheetByName(XSSFWorkbook workbook, String... substrings) {
		for(int i=0; i<workbook.getNumberOfSheets(); i++) {
			String name = workbook.getSheetName(i).toLowerCase();
			boolean all = true;
			for(String s : substrings) {
				if(!name.contains(s.toLowerCase())) { all = false; break; }
			}
			if(all) return workbook.getSheetAt(i);
		}
		return null;
	}

	/** Locates the frequency column (1-based) in headerRowNo whose header matches
	 *  the chosen ethnicity. Match is exact (case-insensitive); the part before a
	 *  '/' is also compared so e.g. "African American" matches
	 *  "African American/Afro-Caribbean". Returns 0 when the gene's table has no
	 *  column for that ethnicity (frequency is then left blank). */
	private int findEthnicityColumn(XSSFSheet sheet, int headerRowNo, String ethnicity) {
		if(sheet == null || ethnicity == null) return 0;
		XSSFRow row = sheet.getRow(headerRowNo-1);
		if(row == null) return 0;
		String e = ethnicity.trim().toLowerCase();
		String e0 = e.split("/")[0].trim();
		for(int c=0; c<row.getLastCellNum(); c++) {
			XSSFCell cell = row.getCell(c);
			if(cell == null) continue;
			String header;
			try { header = cell.getStringCellValue().trim().toLowerCase(); }
			catch(Exception ex) { continue; }
			if(header.isEmpty()) continue;
			String h0 = header.split("/")[0].trim();
			if(header.equals(e) || h0.equals(e0)) return c+1;
		}
		return 0;
	}

	// Resolve a per-gene ClinPGx table by its filename template (e.g.
	// "%s_frequency_table.xlsx"). All ClinPGx-downloaded tables live in
	// ./gene_tables/ so that a user can refresh the database by simply
	// dropping freshly downloaded files there. ./resources/ is kept as a fallback
	// for backward compatibility. Returns null if neither location has the file.
	private File clinpgxTable(String template) {
		String leaf = String.format(template, name);
		File f = new File(AppPaths.geneTablesDir(), leaf);
		if(f.exists())
			return f;
		f = new File(AppPaths.resourcesDir(), leaf);
		if(f.exists())
			return f;
		return null;
	}


	private void readHaplotypePhenotypeFile() {
		File file = clinpgxTable("%s_allele_functionality_reference.xlsx");
		if(file == null)
			return;
		try (XSSFWorkbook workbook = new XSSFWorkbook(file);){
			XSSFSheet curSheet;
			XSSFRow curRow;
			XSSFCell curCell;
			curSheet = workbook.getSheetAt(0);

			// Locate columns by header label (header is on row 2; data starts row 3).
			int haplotypeActivityScoreColumnNo = findColumnByHeader(curSheet, 2, "activity value");
			int haplotypePhenotypeColumnNo = findColumnByHeader(curSheet, 2, "clinical functional status");
			if(haplotypePhenotypeColumnNo == 0)
				return;

			for(int i=2;i<curSheet.getPhysicalNumberOfRows();i++) {
				String haplotype = "";
				String activityScore = "";
				String phenotype = "";
				curRow = curSheet.getRow(i);
				curCell = curRow.getCell(0);
				haplotype = curCell.getStringCellValue().trim();

				if(haplotypeActivityScoreColumnNo == 0)
					activityScore = "";
				else
					activityScore = cellToString(curRow.getCell(haplotypeActivityScoreColumnNo-1));

				curCell = curRow.getCell(haplotypePhenotypeColumnNo-1);
				if(curCell == null)
					phenotype = "";
				else
					phenotype = curCell.getStringCellValue().trim();
				if(phenotype == null)
					phenotype = "";


				haplotypePhenotypeMap.put(haplotype,  phenotype);
				haplotypeActivityScoreMap.put(haplotype, activityScore);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private void readDiplotypePhenotypeFile() {
		File file = clinpgxTable("%s_Diplotype_Phenotype_Table.xlsx");
		if(file == null)
			return;
		try (XSSFWorkbook workbook = new XSSFWorkbook(file);){
			XSSFSheet curSheet;
			XSSFRow curRow;
			XSSFCell curCell;
			curSheet = workbook.getSheetAt(0);

			// Locate columns by header label (header is on row 1; data starts row 2).
			int diplotypeActivityScoreColumnNo = findColumnByHeader(curSheet, 1, "activity score");
			int diplotypePhenotypeColumnNo = findColumnByHeader(curSheet, 1, "phenotype");
			if(diplotypePhenotypeColumnNo == 0)
				return;

			for(int i=1;i<curSheet.getPhysicalNumberOfRows();i++) {
				String diplotype = "";
				String activityScore = "";
				String phenotype = "";
				curRow = curSheet.getRow(i);
				curCell = curRow.getCell(0);
				diplotype = curCell.getStringCellValue().trim();

				if(diplotypeActivityScoreColumnNo == 0)
					activityScore = "";
				else
					activityScore = cellToString(curRow.getCell(diplotypeActivityScoreColumnNo-1));

				curCell = curRow.getCell(diplotypePhenotypeColumnNo-1);
				if(curCell == null)
					phenotype = "";
				else
					phenotype = curCell.getStringCellValue().trim();
				if(phenotype == null)
					phenotype = "";


				//CYP2C19 Intermediate metabolizer -> Intermediate metabolizer
				phenotype = phenotype.replace(name,  "");
				phenotype = phenotype.trim();
				phenotype = RootController.addAbbreviation(phenotype);

				diplotypePhenotypeMap.put(diplotype,  phenotype);
				diplotypePhenotypeMap.put(getReversedDiplotype(diplotype),  phenotype);
				diplotypeActivityScoreMap.put(diplotype, activityScore);
				diplotypeActivityScoreMap.put(getReversedDiplotype(diplotype), activityScore);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private void readFrequencyFile() {
		File file = clinpgxTable("%s_frequency_table.xlsx");
		if(file == null)
			return;
		String ethnicity = RootController.selectedEthnicity;
		try (XSSFWorkbook workbook = new XSSFWorkbook(file);){
			XSSFSheet curSheet;
			XSSFRow curRow;
			XSSFCell curCell;

			// Diplotype frequencies: locate the sheet by name and the ethnicity
			// column by header (row 2). If the gene's table has no column for the
			// chosen ethnicity, frequencies are left blank.
			XSSFSheet diplotypeSheet = findSheetByName(workbook, "diplotype frequency");
			int diplotypeFrequencyColumn = findEthnicityColumn(diplotypeSheet, 2, ethnicity);
			if(diplotypeSheet != null && diplotypeFrequencyColumn != 0) {
				curSheet = diplotypeSheet;
				for(int i=2;i<curSheet.getPhysicalNumberOfRows();i++) {
					String diplotype = "";
					String frequency = "";
					curRow = curSheet.getRow(i);
					curCell = curRow.getCell(0);
					diplotype = curCell.getStringCellValue().trim();

					curCell = curRow.getCell(diplotypeFrequencyColumn-1);
					if(curCell == null)
						continue;

					//System.out.println(String.format("gene : %s, position : (%d, %d)", name, i,  frequencyColumn-1));				
					//Handle the case where the cell type is numeric.
					try {
						frequency = curCell.getStringCellValue().trim();
					}
					catch (IllegalStateException iex) {
						frequency = String.format("%.7f",  curCell.getNumericCellValue());
					}

					if(frequency == null) 
						continue; 
					diplotypeFrequencyMap.put(diplotype, frequency);
					diplotypeFrequencyMap.put(getReversedDiplotype(diplotype), frequency);
					//System.out.println(String.format("%s -> %s",  diplotype, getReversedDiplotype(diplotype)));
				}
			}

			// Haplotype (allele) frequencies: located on the 'Allele frequency' sheet.
			XSSFSheet alleleSheet = findSheetByName(workbook, "allele frequency");
			int haplotypeFrequencyColumn = findEthnicityColumn(alleleSheet, 2, ethnicity);
			if(alleleSheet != null && haplotypeFrequencyColumn != 0) {

				curSheet = alleleSheet;
				for(int i=2;i<curSheet.getPhysicalNumberOfRows();i++) {
					String haplotype = "";
					String frequency = "";
					curRow = curSheet.getRow(i);
					curCell = curRow.getCell(0);
					haplotype = curCell.getStringCellValue().trim();

					curCell = curRow.getCell(haplotypeFrequencyColumn-1);
					if(curCell == null)
						continue;

					//System.out.println(String.format("gene : %s, position : (%d, %d)", name, i,  frequencyColumn-1));				
					//Handle the case where the cell type is numeric.
					try {
						frequency = curCell.getStringCellValue().trim();
					}
					catch (IllegalStateException iex) {
						frequency = String.format("%.7f",  curCell.getNumericCellValue());
					}

					if(frequency == null) 
						continue; 
					haplotypeFrequencyMap.put(haplotype, frequency);
					//System.out.println(String.format("%s -> %s",  diplotype, getReversedDiplotype(diplotype)));
				}

				Set<String> keySet = haplotypeFrequencyMap.keySet();


				for(String key : keySet) {
					String s_freq = haplotypeFrequencyMap.get(key);
					double d_freq = 0;
					try {
						d_freq = Double.parseDouble(s_freq)*100;
					}
					catch (NumberFormatException ex) {
						continue;
					}
					if(d_freq<0.1) 
						continue;

					HaplotypeFreq hf = new HaplotypeFreq(key, d_freq);
					hfList.add(hf);
					//System.out.println(String.format("Current input : %s (%.1f)", key, d_freq));
				}
				Collections.sort(hfList);

				/*
				for(int j=0; j<topN; j++) {
					HaplotypeFreq hf = hfList.get(j);
					System.out.println(String.format("%s (%.1f)", hf.name, hf.freq));
				}
				 */

			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private String makeDiplotype(String allele1, String allele2) {
		try {
			if(allele1.charAt(0) == '*' && allele2.charAt(0) == '*') {
				int i_allele1 = Integer.parseInt(allele1.substring(1));
				int i_allele2 = Integer.parseInt(allele2.substring(1));
				if(i_allele2<i_allele1) 
					return allele2 + "/" + allele1;
			}
		}
		catch(Exception ex) {
		}

		return allele1 + "/" + allele2;
	}


	private void readAlleleDefTable() {
		//Read the Allele Def table and build two maps (alleleToVariant, variantToAllele).
		// A hand-"Modified" table carries a "Modified" label row with already-anchored
		// positions/sequences; a freshly downloaded ("raw") ClinPGx table instead
		// carries HGVS-g notation that is converted on the fly against a bundled
		// per-gene hg38 reference slice (resources/refseq/{gene}.txt).
		// Tables are sourced from ./gene_tables/ (unmodified ClinPGx downloads); a
		// ./resources/ copy is used as a fallback for backward compatibility.
		File file = clinpgxTable("%s_allele_definition_table.xlsx");
		if(file == null)
			return;
		try (XSSFWorkbook workbook = new XSSFWorkbook(file);){
			XSSFSheet curSheet = workbook.getSheetAt(0);
			if(findRowByLabel(curSheet, "Modified") > 0)
				buildMapsFromModified(curSheet);
			else
				buildMapsFromRaw(curSheet);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}

		Set<String> keySet = alleleToVariantMap.keySet();
		Iterator iter = keySet.iterator();

		String[] starAlleleArray = new String[keySet.size()];

		int cnt = 0;
		while(iter.hasNext()) {
			String iterKey = (String)iter.next();
			starAlleleArray[cnt++] = iterKey;
			//System.out.println(iterKey);
			//System.out.println(alleleToVariantMap.get(iterKey));
		}

		for(int i=0;i<starAlleleArray.length;i++) {
			for(int j=i;j<starAlleleArray.length;j++) {

				Vector<String> variantList = new Vector<String>();
				variantList.addAll(alleleToVariantMap.get(starAlleleArray[i]));
				variantList.addAll(alleleToVariantMap.get(starAlleleArray[j]));

				/*
				//Add variantList2 to variantList; if there is an overlap, append '_second' to the ID before adding.
				Vector<String> variantList2 = new Vector<String>(); 
				variantList2.addAll(alleleToVariantMap.get(starAlleleArray[j]));

				for(String targetVariant : variantList2) {
					if(variantList.contains(targetVariant))
						targetVariant += "_second";
					variantList.add(targetVariant);						
				}
				 */

				String diplotypeName = makeDiplotype(starAlleleArray[i], starAlleleArray[j]);
				
				//handling for NUDT *3_sub : create a duplicate Diplotype Meta object that differs only in the variant list but otherwise holds the same values, and add it to diplotypeMetaList (Vector). Since access uses linear search, duplicates cause no problem.
				//alleleToVariantMap is searched by the original haplotypeName, while the rest of the information is searched with '_sub' removed.
				String diplotypeName_sub_removed = diplotypeName.replace("_sub",  "");
				String haplotype1_sub_removed = starAlleleArray[i].replace("_sub",  "");
				String haplotype2_sub_removed = starAlleleArray[j].replace("_sub",  "");
				
				String phenotype = diplotypePhenotypeMap.get(diplotypeName_sub_removed);
				//System.out.println(String.format("gene, diplotype, phenotype : %s, %s, %s", name, diplotypeName, phenotype));
				String phenotypeDescription="";
				if(phenotype!=null) {
					
					String key = name + ":" + phenotype;
					phenotypeDescription = RootController.phenotypeDescriptionMap.get(key);
					//System.out.println("phenotypeDescription : " + phenotypeDescription);
				}

				DiplotypeMeta diplotypeMeta = 
						new DiplotypeMeta(diplotypeName_sub_removed, variantList, diplotypeFrequencyMap.get(diplotypeName_sub_removed), phenotype, phenotypeDescription, diplotypeActivityScoreMap.get(diplotypeName_sub_removed), 
								haplotype1_sub_removed, alleleToVariantMap.get(starAlleleArray[i]), haplotypeFrequencyMap.get(haplotype1_sub_removed), haplotypePhenotypeMap.get(haplotype1_sub_removed), haplotypeActivityScoreMap.get(haplotype1_sub_removed),
								haplotype2_sub_removed, alleleToVariantMap.get(starAlleleArray[j]), haplotypeFrequencyMap.get(haplotype2_sub_removed), haplotypePhenotypeMap.get(haplotype2_sub_removed), haplotypeActivityScoreMap.get(haplotype2_sub_removed)								
								);


				diplotypeMetaList.add(diplotypeMeta);
			}
		}
	}

	// ==== Allele-definition map builders =======================================

	/** Returns the 1-based row index whose first column contains the given
	 *  substring (case-insensitive), or 0 if none. */
	private int findRowByLabel(XSSFSheet sheet, String substring) {
		if(sheet == null) return 0;
		String needle = substring.toLowerCase();
		for(int r=0; r<=sheet.getLastRowNum(); r++) {
			XSSFRow row = sheet.getRow(r);
			if(row == null) continue;
			String v = cellToString(row.getCell(0));
			if(!v.isEmpty() && v.toLowerCase().contains(needle))
				return r+1;
		}
		return 0;
	}

	/** Builds the two maps from a hand-"Modified" table, where the position row
	 *  (GRCh38RowNo) and reference-allele row (referenceRowNo) hold already-anchored
	 *  values. This is the original behaviour and is preserved byte-for-byte. */
	private void buildMapsFromModified(XSSFSheet curSheet) {
		String refAllele = null;
		XSSFRow GRCh38Row, curRow, refRow;
		XSSFCell GRCh38Cell, curCell, starCell;
		GRCh38Row = curSheet.getRow(GRCh38RowNo-1);
		for(int i=1;i<GRCh38Row.getLastCellNum();i++) {
			GRCh38Cell = GRCh38Row.getCell(i);
			String gDescription = GRCh38Cell.getStringCellValue().trim();

			//Extract only the numeric part of the GRCh38 description to build the genomic location.
			String keyFront ="";
			for(int j=2;j<gDescription.length();j++) {
				if(gDescription.charAt(j)>='0' && gDescription.charAt(j)<='9') {
					keyFront+=gDescription.charAt(j);
				}
				else
					break;
			}

			// Prepend the chromosome to the genomic location to build the key prefix, e.g. "19-41012393-".
			keyFront = "chr" + chromosome + "-" + keyFront + "-";

			refRow = curSheet.getRow(referenceRowNo-1);
			String refSequence = refRow.getCell(i).getStringCellValue().trim();
			keyFront = keyFront + refSequence + "-";

			refAllele = refRow.getCell(0).getStringCellValue().trim();
			alleleToVariantMap.put(refAllele,  new Vector<String>());

			for(int j=referenceRowNo;j<curSheet.getPhysicalNumberOfRows();j++) {
				curRow = curSheet.getRow(j);
				curCell = curRow.getCell(i);
				if(curCell == null)
					continue;
				String cellValue = curCell.getStringCellValue().trim();
				if(cellValue != null && cellValue.length()>0) {

					//variantToAlleleMap
					String key = keyFront + cellValue;
					String starAlleleList = "";
					if(variantToAlleleMap.containsKey(key))
						starAlleleList = variantToAlleleMap.get(key);
					starCell = curRow.getCell(0);
					String starAllele = starCell.getStringCellValue().trim();
					starAlleleList += starAllele + ", ";
					variantToAlleleMap.put(key,  starAlleleList);

					//alleleToVariantMap
					Vector<String> variantList = new Vector<String>();
					if(alleleToVariantMap.containsKey(starAllele))
						variantList = alleleToVariantMap.get(starAllele);
					variantList.add(key);
					alleleToVariantMap.put(starAllele, variantList);
				}
			}
		}
	}

	// ---- Raw ClinPGx converter ------------------------------------------------
	// Parses HGVS-g notation in the "Position at NC..." row plus the per-allele
	// cell sequences (SNV / del / dup / ins / delins / repeat[n]) and left-aligns
	// indels against a bundled hg38 reference slice to emit the same
	// chr-pos-ref-alt keys the modified tables produce.

	private int[] refWinStart = new int[0];
	private String[] refWinSeq = new String[0];

	private static final java.util.regex.Pattern P_SNV    = java.util.regex.Pattern.compile("^g\\.\\d+[ACGT]>[ACGT]$");
	private static final java.util.regex.Pattern P_DEL    = java.util.regex.Pattern.compile("^g\\.(\\d+)(?:_(\\d+))?del$");
	private static final java.util.regex.Pattern P_DUP    = java.util.regex.Pattern.compile("^g\\.(\\d+)(?:_(\\d+))?dup$");
	private static final java.util.regex.Pattern P_INS    = java.util.regex.Pattern.compile("^g\\.(\\d+)_(\\d+)ins([ACGT]+)$");
	private static final java.util.regex.Pattern P_DELINS = java.util.regex.Pattern.compile("^g\\.(\\d+)(?:_(\\d+))?delins([ACGT]+)$");
	private static final java.util.regex.Pattern P_REP    = java.util.regex.Pattern.compile("^g\\.(\\d+)([ACGT]+)\\[(\\d+)\\]$");
	private static final java.util.regex.Pattern P_GPOS   = java.util.regex.Pattern.compile("g\\.(\\d+)");
	private static final java.util.regex.Pattern P_PAREN  = java.util.regex.Pattern.compile("\\((\\d+)\\)");

	private static class Norm { int pos; String ref, alt; Norm(int p,String r,String a){pos=p;ref=r;alt=a;} }

	/** Loads resources/refseq/{gene}.txt: a "chrN" header line then one or more
	 *  "<1-based-start>\t<UPPERCASE-SEQUENCE>" windows around each indel. */
	private void loadRefSlice() {
		refWinStart = new int[0]; refWinSeq = new String[0];
		File f = new File(AppPaths.resourcesDir(), String.format("refseq/%s.txt", name));
		if(!f.exists()) return;
		Vector<Integer> starts = new Vector<Integer>();
		Vector<String> seqs = new Vector<String>();
		try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f))) {
			br.readLine(); // chrN header
			String line;
			while((line = br.readLine()) != null) {
				line = line.trim();
				if(line.isEmpty()) continue;
				String[] t = line.split("\t");
				if(t.length < 2) continue;
				starts.add(Integer.parseInt(t[0].trim()));
				seqs.add(t[1].trim().toUpperCase());
			}
		}
		catch(Exception ex) { ex.printStackTrace(); return; }
		refWinStart = new int[starts.size()];
		refWinSeq = new String[seqs.size()];
		for(int i=0;i<starts.size();i++) { refWinStart[i]=starts.get(i); refWinSeq[i]=seqs.get(i); }
	}

	/** Reference base at a 1-based genomic position. */
	private char gbase(int pos) {
		for(int i=0;i<refWinStart.length;i++) {
			int s = refWinStart[i];
			if(s<=pos && pos < s+refWinSeq[i].length())
				return refWinSeq[i].charAt(pos-s);
		}
		throw new RuntimeException("no ref for pos " + pos);
	}

	/** Reference substring spanning 1-based inclusive [a,b]. */
	private String gsub(int a, int b) {
		StringBuilder sb = new StringBuilder();
		for(int p=a;p<=b;p++) sb.append(gbase(p));
		return sb.toString();
	}

	private static String repeat(String unit, int n) {
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<n;i++) sb.append(unit);
		return sb.toString();
	}

	/** bcftools/vt-style left-align + parsimony. pos = 1-based of ref[0]. */
	private Norm normalize(int pos, String ref, String alt) {
		ref = ref.toUpperCase(); alt = alt.toUpperCase();
		if(ref.equals(alt)) return new Norm(pos, ref, alt); // avoid infinite roll
		while(ref.charAt(ref.length()-1) == alt.charAt(alt.length()-1)) {
			if(ref.length()==1 || alt.length()==1) {
				char b = gbase(pos-1);
				ref = b + ref; alt = b + alt; pos--;
			}
			ref = ref.substring(0, ref.length()-1);
			alt = alt.substring(0, alt.length()-1);
		}
		while(ref.length()>=2 && alt.length()>=2 && ref.charAt(0)==alt.charAt(0)) {
			ref = ref.substring(1); alt = alt.substring(1); pos++;
		}
		return new Norm(pos, ref, alt);
	}

	private int colPos(String hgvs) {
		java.util.regex.Matcher m = P_GPOS.matcher(hgvs.trim());
		if(m.find()) return Integer.parseInt(m.group(1));
		return -1;
	}

	private static java.util.List<Integer> parenCounts(String s) {
		java.util.List<Integer> out = new java.util.ArrayList<Integer>();
		java.util.regex.Matcher m = P_PAREN.matcher(s);
		while(m.find()) out.add(Integer.parseInt(m.group(1)));
		return out;
	}

	/** Adds one chr-pos-ref-alt key for an allele to both maps.
	 *  Keys where ref==alt represent the reference state (no real variant) at that
	 *  position. The genotyping matcher (Sample.getScore/AUPAC_compare) only strips
	 *  such "no-variant" keys when the ref is an IUPAC ambiguity code; for plain
	 *  bases (A/C/G/T) they would survive as phantom variants and break every
	 *  diplotype that includes the reference allele. So we simply never emit them. */
	private void emitKey(int pos, String ref, String alt, String allele) {
		if(ref.equalsIgnoreCase(alt)) return;   // reference state -> not a variant
		String key = "chr" + chromosome + "-" + pos + "-" + ref + "-" + alt;
		String list = variantToAlleleMap.containsKey(key) ? variantToAlleleMap.get(key) : "";
		list += allele + ", ";
		variantToAlleleMap.put(key, list);
		Vector<String> vl = alleleToVariantMap.containsKey(allele) ? alleleToVariantMap.get(allele) : new Vector<String>();
		vl.add(key);
		alleleToVariantMap.put(allele, vl);
	}

	/** Builds the two maps from a raw ClinPGx allele_definition_table. */
	private void buildMapsFromRaw(XSSFSheet sheet) {
		loadRefSlice();
		int posRow = findRowByLabel(sheet, "Position at NC");
		int alleleHdr = findRowByLabel(sheet, name + " Allele");
		if(alleleHdr == 0) alleleHdr = findRowByLabel(sheet, "Allele");
		if(posRow == 0 || alleleHdr == 0) return;
		int refRowNo = alleleHdr + 1;                 // 1-based reference-allele row
		XSSFRow posR = sheet.getRow(posRow-1);
		XSSFRow refR = sheet.getRow(refRowNo-1);
		if(posR == null || refR == null) return;
		int lastRow = sheet.getLastRowNum();

		// Pre-register ONLY the reference allele (first allele row) with an empty
		// variant list, so its diplotype pairings (e.g. *1/*1, *1/*2) are produced
		// even though it carries no variants. Every other allele enters the map only
		// when it emits a real variant key (via emitKey). Alleles defined solely by a
		// "Structural Variation" column or any non-SNV/indel descriptor the converter
		// cannot anchor therefore stay out of the candidate set instead of becoming
		// phantom reference-equivalents that tie with the true genotype.
		{
			XSSFRow refNameRow = sheet.getRow(refRowNo-1);
			if(refNameRow != null) {
				String refAllele = cellToString(refNameRow.getCell(0));
				if(!refAllele.isEmpty() && !alleleToVariantMap.containsKey(refAllele))
					alleleToVariantMap.put(refAllele, new Vector<String>());
			}
		}

		for(int c=1; c<posR.getLastCellNum(); c++) {
			try {
				String hraw = cellToString(posR.getCell(c));
				if(hraw.isEmpty()) continue;
				java.util.List<String> parts = new java.util.ArrayList<String>();
				for(String p : hraw.split(";")) { p = p.trim(); if(!p.isEmpty()) parts.add(p); }
				if(parts.isEmpty()) continue;
				String first = parts.get(0);
				String refcell = cellToString(refR.getCell(c));

				// ---- SNV column: every part is a simple substitution ----
				boolean allSNV = true;
				for(String p : parts) if(!P_SNV.matcher(p).matches()) { allSNV=false; break; }
				if(allSNV) {
					int pos = colPos(first);
					for(int r=refRowNo-1; r<=lastRow; r++) {
						XSSFRow row = sheet.getRow(r); if(row==null) continue;
						String an = cellToString(row.getCell(0));
						String cv = cellToString(row.getCell(c));
						if(an.isEmpty() || cv.isEmpty()) continue;
						emitKey(pos, refcell, cv, an);
					}
					continue;
				}

				java.util.regex.Matcher md=P_DEL.matcher(first), mdu=P_DUP.matcher(first),
						mi=P_INS.matcher(first), mdi=P_DELINS.matcher(first), mr=P_REP.matcher(first);

				if(md.matches()) {
					int a=Integer.parseInt(md.group(1));
					int b=md.group(2)!=null?Integer.parseInt(md.group(2)):a;
					int pos0=a-1;
					Norm n=normalize(pos0, gbase(pos0)+gsub(a,b), ""+gbase(pos0));
					for(int r=refRowNo-1;r<=lastRow;r++) {
						XSSFRow row=sheet.getRow(r); if(row==null) continue;
						String an=cellToString(row.getCell(0)), cv=cellToString(row.getCell(c));
						if(an.isEmpty()||cv.isEmpty()) continue;
						if(cv.toLowerCase().startsWith("del")) emitKey(n.pos,n.ref,n.alt,an);
						else emitKey(n.pos,n.ref,n.ref,an);
					}
					continue;
				}
				if(mdu.matches()) {
					int a=Integer.parseInt(mdu.group(1));
					int b=mdu.group(2)!=null?Integer.parseInt(mdu.group(2)):a;
					String ins=gsub(a,b); int pos0=b;
					Norm n=normalize(pos0, ""+gbase(b), gbase(b)+ins);
					for(int r=refRowNo-1;r<=lastRow;r++) {
						XSSFRow row=sheet.getRow(r); if(row==null) continue;
						String an=cellToString(row.getCell(0)), cv=cellToString(row.getCell(c));
						if(an.isEmpty()||cv.isEmpty()) continue;
						if(cv.length()>refcell.length()) emitKey(n.pos,n.ref,n.alt,an);
						else emitKey(n.pos,n.ref,n.ref,an);
					}
					continue;
				}
				if(mi.matches()) {
					int a=Integer.parseInt(mi.group(1)); String seq=mi.group(3); int pos0=a;
					Norm n=normalize(pos0, ""+gbase(a), gbase(a)+seq);
					for(int r=refRowNo-1;r<=lastRow;r++) {
						XSSFRow row=sheet.getRow(r); if(row==null) continue;
						String an=cellToString(row.getCell(0)), cv=cellToString(row.getCell(c));
						if(an.isEmpty()||cv.isEmpty()) continue;
						if(cv.toLowerCase().startsWith("ins")) emitKey(n.pos,n.ref,n.alt,an);
						else emitKey(n.pos,n.ref,n.ref,an);
					}
					continue;
				}
				if(mdi.matches()) {
					int a=Integer.parseInt(mdi.group(1));
					int b=mdi.group(2)!=null?Integer.parseInt(mdi.group(2)):a;
					String seq=mdi.group(3); int pos0=a;
					Norm n=normalize(pos0, gsub(a,b), seq);
					for(int r=refRowNo-1;r<=lastRow;r++) {
						XSSFRow row=sheet.getRow(r); if(row==null) continue;
						String an=cellToString(row.getCell(0)), cv=cellToString(row.getCell(c));
						if(an.isEmpty()||cv.isEmpty()) continue;
						String lc=cv.toLowerCase();
						if(lc.startsWith("delins")||lc.startsWith("del")) emitKey(n.pos,n.ref,n.alt,an);
						else emitKey(n.pos,n.ref,n.ref,an);
					}
					continue;
				}
				if(mr.matches()) {
					int P=Integer.parseInt(mr.group(1)); String unit=mr.group(2); int pos0=P-1;
					java.util.List<Integer> mref=parenCounts(refcell);
					int refcount = mref.isEmpty()?1:mref.get(0);
					// Pass 1: gather (name,count) for every allele cell in the column.
					java.util.List<String> names=new java.util.ArrayList<String>();
					java.util.List<Integer> ks=new java.util.ArrayList<Integer>();
					for(int r=refRowNo-1;r<=lastRow;r++) {
						XSSFRow row=sheet.getRow(r); if(row==null) continue;
						String an=cellToString(row.getCell(0)), cv=cellToString(row.getCell(c));
						if(an.isEmpty()||cv.isEmpty()) continue;
						java.util.List<Integer> counts=parenCounts(cv);
						if(counts.isEmpty()) counts=java.util.Collections.singletonList(1);
						for(int idx=0; idx<counts.size(); idx++) {
							names.add(idx==0?an:an+"_sub"); ks.add(counts.get(idx));
						}
					}
					// Pass 2a: emit each variant (count != refcount), collect distinct anchors.
					java.util.List<Integer> anPos=new java.util.ArrayList<Integer>();
					java.util.List<String> anRef=new java.util.ArrayList<String>();
					for(int t=0;t<names.size();t++) {
						int k=ks.get(t); if(k==refcount) continue;
						Norm n=normalize(pos0, gbase(pos0)+repeat(unit,refcount), gbase(pos0)+repeat(unit,k));
						emitKey(n.pos,n.ref,n.alt,names.get(t));
						boolean seen=false;
						for(int q=0;q<anPos.size();q++) if(anPos.get(q)==n.pos && anRef.get(q).equals(n.ref)) { seen=true; break; }
						if(!seen) { anPos.add(n.pos); anRef.add(n.ref); }
					}
					// Pass 2b: reference-count alleles emit a ref/ref key at each anchor.
					for(int t=0;t<names.size();t++) {
						if(ks.get(t)!=refcount) continue;
						for(int q=0;q<anPos.size();q++) emitKey(anPos.get(q),anRef.get(q),anRef.get(q),names.get(t));
					}
					continue;
				}
				System.err.println("readAlleleDefTable["+name+"] col"+c+" UNHANDLED hgvs="+first+" ref="+refcell);
			}
			catch(Exception ex) {
				System.err.println("readAlleleDefTable["+name+"] col"+c+" EXCEPTION: "+ex.getMessage());
			}
		}
	}
}
