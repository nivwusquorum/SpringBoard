package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class Tools {
	
	public static boolean pointsEqual(Point a, Point b) {
		if (a == null || b == null)
			return a == null && b == null;
		return a.getX() == b.getX() && a.getY() == b.getY();
	}

	public static double pointsDistanceSquared(Point a, Point b) {
		double dx = a.getX() - b.getX();
		double dy = a.getY() - b.getY();
		return dx*dx + dy*dy;
	}

}
