package snackpgx;

import java.util.Collections;
import java.util.Vector;

public class GeneContent {
	public String name = null;
	public Vector<Variant> variantList = new Vector<Variant>();
	public Vector<Variant> includedVariantList = new Vector<Variant>();
	public Vector<Diplotype> diplotypeList = new Vector<Diplotype>();
	public GeneMetaData geneMetaData = null;
	
	public GeneContent(GeneMetaData geneMetaData) {
		this.geneMetaData = geneMetaData;
		this.name = geneMetaData.name;
	}
	
	//call from readAnnotation file (performed only at the beginning)
	public void addVariant(Variant variant) {
		if(variant.starAlleles!=null && variant.starAlleles.length()>0)
			variant.included = "O";
		variantList.add(variant);
	}
	
	public void updateIncludedVariantList() {
		includedVariantList = new Vector<Variant>();
		for(Variant variant : variantList) {
			if(variant.included.equals("O")) {
				//For a homozygous variant, split it into two heterozygous variants.
				if(variant.zygosity.equals("Hom")) {
					Variant heteroVariant1 = new Variant(variant);
					heteroVariant1.zygosity = "Het";
					Variant heteroVariant2 = new Variant(variant);
					heteroVariant2.zygosity = "Het";
					//heteroVariant2.indexID += "_second"; 
					includedVariantList.add(heteroVariant1);
					includedVariantList.add(heteroVariant2);
				}
				else 
					includedVariantList.add(variant);
			}
		}
	}
	
	
	public void resetDiplotypeList() {
		diplotypeList.clear();
	}
	
	public void addDiplotype(Diplotype diplotype) {
		diplotypeList.add(diplotype);
	}
	
	public void sortDiplotypeList() {
		if(diplotypeList == null || diplotypeList.size()==0)
			return;
		Collections.sort(diplotypeList);
		Vector<Diplotype> temp = (Vector<Diplotype>)diplotypeList.clone();
		diplotypeList = new Vector<Diplotype>();

		Diplotype firstDiplotype = temp.get(0);
		double firstScore = firstDiplotype.score;
		for(Diplotype diplotype : temp) {
			if(diplotype.score != firstScore)
				break;
			diplotypeList.add(diplotype);
		}
	}

	/** Move the first diplotype whose name equals {@code preferredName} to the front
	 *  (used for the NUDT15 *3/*6 special case; no-op if absent). */
	public void preferDiplotype(String preferredName) {
		if(diplotypeList == null || diplotypeList.size() < 2) return;
		for(int i=1;i<diplotypeList.size();i++) {
			if(diplotypeList.get(i).name.equals(preferredName)) {
				Diplotype d = diplotypeList.remove(i);
				diplotypeList.add(0, d);
				return;
			}
		}
	}
}



