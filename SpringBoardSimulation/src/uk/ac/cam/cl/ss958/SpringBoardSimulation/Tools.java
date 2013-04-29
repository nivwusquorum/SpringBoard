package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class Tools {
	
	public static boolean pointsEqual(Point a, Point b) {
		if (a == null || b == null)
			return a == null && b == null;
		return a.getX() == b.getX() && a.getY() == b.getY();
	}


}
