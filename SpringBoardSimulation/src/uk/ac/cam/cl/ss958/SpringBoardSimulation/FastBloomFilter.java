package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import ie.ucd.murmur.MurmurHash;

import java.util.BitSet;
import java.util.Random;

public class FastBloomFilter {

	private final BitSet bs;
	
	final int [] hashSeeds;

	final int capacity;
	
	public FastBloomFilter(int slots, int hashFunctions) {
		bs = new BitSet(slots);
		Random r = new Random(System.currentTimeMillis());
		hashSeeds = new int[hashFunctions];
		for (int i=0; i<hashFunctions; ++i) {
			hashSeeds[i] = r.nextInt();
		}
		capacity = slots;
	}
	
	public void add(int value) {
		byte [] b = new byte[] {
	            (byte)(value >>> 24),
	            (byte)(value >>> 16),
	            (byte)(value >>> 8),
	            (byte)value};
		for (int i=0; i<hashSeeds.length; ++i) {
			int h = MurmurHash.hash32(b, 4, hashSeeds[i]);
			bs.set(Math.abs(h)%capacity, true);
		}
	}
	
	public void clear() {
		bs.clear();
	}
	
	public boolean mightContain(int value) {
		byte [] b = new byte[] {
	            (byte)(value >>> 24),
	            (byte)(value >>> 16),
	            (byte)(value >>> 8),
	            (byte)value};
		for (int i=0; i<hashSeeds.length; ++i) {
			int h = MurmurHash.hash32(b, 4, hashSeeds[i]);
			try {
				if(!bs.get(Math.abs(h)%capacity)) {
					return false;
				}
			} catch(java.lang.IndexOutOfBoundsException e) {
				System.out.println("h: " + h);
				System.out.println("abs(h): " + Math.abs(h));
				System.out.println("bs: " + capacity);
				System.out.println("abs(h): " + Math.abs(h)%capacity);
			}
		}
		
		return true;
	}
	
	public static int byteArrayToInt(byte[] b) 
	{
		return   b[3] & 0xFF |
				(b[2] & 0xFF) << 8 |
				(b[1] & 0xFF) << 16 |
				(b[0] & 0xFF) << 24;
	}

	public static byte[] intToByteArray(int a)
	{
		return new byte[] {
				(byte) ((a >> 24) & 0xFF),
				(byte) ((a >> 16) & 0xFF),   
				(byte) ((a >> 8) & 0xFF),   
				(byte) (a & 0xFF)
		};
	}
	

	
	public static void main(String [] args) {
		FastBloomFilter bf = new FastBloomFilter(1000, 10);
		System.out.println("Query for 2000: " + bf.mightContain(2000));
		System.out.println("Adding 2000");
		bf.add(2000);
		System.out.println("Query for 2000: " + bf.mightContain(2000));

		int v = 93316;
		
		System.out.println(byteArrayToInt(intToByteArray(v)));

		
	}
}
