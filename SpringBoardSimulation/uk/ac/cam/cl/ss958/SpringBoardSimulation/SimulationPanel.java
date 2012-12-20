package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;
import uk.ac.cam.cl.ss958.SpringBoardSimulation.SimulationModel.UserInModel;

public class SimulationPanel extends JPanel {
    static final Color BACKGROUND_COLOR = new Color(175, 238, 238);
    static final Color USER_COLOR = new Color(0, 206, 209);
    static final Color SELECTED_USER_COLOR = new Color(255, 99, 71);
    static final Color BACKGROUD_SOFT_ERROR_COLOR = new Color(240, 128, 128);

    static final Color RANGE_COLOR = Color.BLUE;
    private int width = 1; //Width of simulation board in pixels
    private int height = 1; //Height of simulation board in pixels

    
    private final SimulationModel model;
    
	SimulationPanel(SimulationModel mainModel) {
		this.model = mainModel;
		
		if(!model.AddUserAtRandomLocation(new User())) {
			System.out.println("Cannot create first user?!");
		}
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
			g.setColor(BACKGROUD_SOFT_ERROR_COLOR);
		else
			g.setColor(BACKGROUND_COLOR);
		g.fillRect(0, 0, width, height);
		g.setColor(USER_COLOR);
		for(int i=0; i < model.getUsers().size(); ++i) {
			
			UserInModel user = model.getUsers().get(i);
			
			g.setColor(USER_COLOR);
			if (i == model.getSelectedUser()) 
				g.setColor(SELECTED_USER_COLOR);
			g.fillOval(user.location.getX() - SimulationModel.USER_RADIUS, 
					   user.location.getY() - SimulationModel.USER_RADIUS,
					   2*SimulationModel.USER_RADIUS,
					   2*SimulationModel.USER_RADIUS);
			
			g.setColor(RANGE_COLOR);
			g.drawOval(user.location.getX()- user.user.getRange(), 
					   user.location.getY()- user.user.getRange(),
					   2*user.user.getRange(),
					   2*user.user.getRange());
		}
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
