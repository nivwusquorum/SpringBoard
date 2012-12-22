
package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.Color;
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

	private SimulationModel model;
	
	GlobalOptionsPanel(SimulationModel mainModel) {
		super(new GridBagLayout());        
		this.model = mainModel;
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		c.anchor = GridBagConstraints.NORTH;
		c.weighty = 0.8; 
		c.gridx = 0;      
		c.gridy = 0; add(clearBoard = new JButton("Clear board"),c); 
		c.gridy = 1; add(addUser = new JButton("Add user"),c);
		c.gridy = 2; add(randomSimulation = new JButton(Strings.START_SIMULATION),c); 
		
		clearBoard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				model.clearUsers();
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
		setSize(200, 300);
	}
}
