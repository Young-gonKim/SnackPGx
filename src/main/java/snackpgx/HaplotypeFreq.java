package snackpgx;

public class HaplotypeFreq implements Comparable<HaplotypeFreq>{
	
	public double freq = 0;
	public String name = "";
	
	
	public int compareTo(HaplotypeFreq hf) {
		if(freq > hf.freq) return -1;
		else if (freq == hf.freq) return 0; 
		else return 1;
	}


	public HaplotypeFreq(String name, double freq) {
		super();
		this.freq = freq;
		this.name = name;
	}
}
