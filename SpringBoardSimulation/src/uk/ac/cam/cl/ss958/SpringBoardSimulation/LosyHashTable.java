package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import ie.ucd.murmur.MurmurHash;

public class LosyHashTable {
	Integer [] ht;

	public LosyHashTable(int capacity) {
		ht = new Integer[capacity];
	}
	
	private int getHash(int value) {
		byte [] b = new byte[] {
	            (byte)(value >>> 24),
	            (byte)(value >>> 16),
	            (byte)(value >>> 8),
	            (byte)value};
		return MurmurHash.hash32(b, 4);
	}
	
	public void add(int v) {
		int index = Math.abs(getHash(v))%ht.length;
		ht[index] = v;
	}
	
	public boolean definitelyContain(int v) {
		int index = Math.abs(getHash(v))%ht.length;
		return ht[index] != null && ht[index] == v;
	}
	
	public void clear() {
		for (int i=0; i<ht.length; ++i) {
			ht[i] = null;
		}
	}
	
	public static void main(String [] args) {
		FastBloomFilter bf = new FastBloomFilter(1000, 10);
		System.out.println("Query for 2000: " + bf.mightContain(2000));
		System.out.println("Adding 2000");
		bf.add(2000);
		System.out.println("Query for 2000: " + bf.mightContain(2000));

		
	}
}
