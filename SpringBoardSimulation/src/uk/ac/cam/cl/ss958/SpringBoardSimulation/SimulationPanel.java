package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class SimulationPanel extends JPanel {
    private int width = 1; //Width of simulation board in pixels
    private int height = 1; //Height of simulation board in pixels

    
    private final SimulationModel model;
    
	SimulationPanel(SimulationModel mainModel) throws Exception {
		this.model = mainModel;

		computeSize();
		
		addMouseListener(new MouseAdapter(){ 
			public void mousePressed(MouseEvent me) { 
				Point p = new Point(me.getPoint());
				model.maybeSelectUser(p);
			}
			
			public void mouseReleased(MouseEvent me) {
				model.movingFinished();
			}
			
			public void mouseExited(MouseEvent me) {
				model.movingFinished();
			}
		});
		
		addMouseMotionListener(new MouseAdapter(){ 
			public void mouseDragged(MouseEvent me) { 
				Point p = new Point(me.getPoint());
				model.maybeMoveUser(p);
			}
		});
		
	}
	
	public SimulationModel getModel() {
		return model;
	}

	public Dimension getPreferredSize() {
		return new Dimension(width, height);
	}
	
	protected void paintComponent(Graphics g) {
		if (model.isSoftError())
			g.setColor(Colors.BACKGROUD_SOFT_ERROR_COLOR);
		else
			g.setColor(Colors.BACKGROUND_COLOR);
		
		g.fillRect(0, 0, width, height);

		model.prepaint(g);
		
		for(Integer id : model.getUsers().keySet()) {
			model.getUsers().get(id).draw(g, id == model.getSelectedUser());
		}
		
		model.postpaint(g);
	}
	
	private void computeSize() {
		int newWidth = model.getWidth();
		int newHeight = model.getHeight();
		if (newWidth != width || newHeight != height) {
			width = newWidth;
			height = newHeight;
			revalidate(); //trigger the GamePanel to re-layout its components
		}
		
	}

}
