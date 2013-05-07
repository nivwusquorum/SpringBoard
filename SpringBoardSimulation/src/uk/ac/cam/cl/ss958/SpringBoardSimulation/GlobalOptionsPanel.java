
package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class GlobalOptionsPanel extends JPanel {
	private JButton clearBoard;
	private JButton addUser;
	private JButton randomSimulation;
	private JCheckBox showRanges;
	private JCheckBox updateUI;

	private SimulationModel model;
	
	private int preferredWidth;
	private int preferredHeight;
	private int lastYOnGrid;
	private int lastXOnGrid;
	private GridBagConstraints c;
	
	public boolean shouldUpdateUI() {
		return updateUI.isSelected();
	}
	
	GlobalOptionsPanel(SimulationModel mainModel) {
		super(new GridBagLayout());        
		this.model = mainModel;
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		c.anchor = GridBagConstraints.NORTH;
		c.weighty = 0.8; 
		c.gridx = 0;      
		c.gridy = 0; add(clearBoard = new JButton("Clear board"),c); 
		c.gridy = 1; add(addUser = new JButton("Add user"),c);
		c.gridy = 2; add(randomSimulation = new JButton(Strings.START_SIMULATION),c);
		c.gridy = 3; add(showRanges = new JCheckBox("Show radio ranges"),c);
		c.gridy = 4; add(updateUI = new JCheckBox("Draw Simulation (uncheck for performance)"),c);
		
		clearBoard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				model.clearUsers();
				User.resetCounter();
			}
		});
		final JPanel self = this;
		addUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(!model.AddRandomUser())
					JOptionPane.showMessageDialog(self, "Not enough space to place user.");
			}
		});
		
		randomSimulation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Simulator simulator = Simulator.getInstance();
				if (simulator.isRunning()) {
					simulator.stop();
					randomSimulation.setText(Strings.START_SIMULATION);
				} else {
					simulator.start();
					randomSimulation.setText(Strings.STOP_SIMULATION);
				}
			}
		});
		
		showRanges.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				model.setDrawRanges(showRanges.isSelected());
			}
		});
		showRanges.setSelected(false);
		model.setDrawRanges(false);
		
		updateUI.setSelected(true);
			
		preferredWidth = 350;
		preferredHeight = 230;
		
		setPreferredSize(new Dimension(preferredWidth,preferredHeight));
		//setSize(400, 500);

		lastYOnGrid = c.gridy;
		lastXOnGrid = 0;
		
		model.addToOptionsMenu(this);
		
	}
	
	public void addElement(JComponent e, int preferredElementHeight) {
		c.gridy = ++lastYOnGrid;
		lastXOnGrid = 0;
		this.preferredHeight += preferredElementHeight;
		add(e, c);
		setPreferredSize(new Dimension(preferredWidth, preferredHeight));
	}
	
	public void addElementToTheRight(JComponent e) {
		c.gridy = lastYOnGrid;
		c.gridx = ++ lastXOnGrid;
		add(e,c);
	}
}
