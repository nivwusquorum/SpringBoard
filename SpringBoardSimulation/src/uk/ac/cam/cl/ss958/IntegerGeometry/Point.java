package uk.ac.cam.cl.ss958.IntegerGeometry;

public class Point {
	int x;
	int y;
	
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	public Point(java.awt.Point p) {
		this.x = p.x;
		this.y = p.y;
	}
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public void setX(int x) {
		this.x = x;
	}
	public void setY(int y) {
		this.y = y;
	}
	
	public Point add(Point p) {
		return new Point(x+p.x, y+p.y);
	}
	public Point sub(Point p) {
		return new Point(x-p.x, y-p.y);
	}
}

