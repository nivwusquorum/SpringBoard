package uk.ac.cam.cl.ss958.IntegerGeometry;


public class Compute {
	public static int square(int x) {
		return x*x;
	}
	
	public static int euclideanDistanceSquared(Point a, Point b) {
		return square(a.getX()-b.getX()) + square(a.getY()-b.getY());
	}
}
