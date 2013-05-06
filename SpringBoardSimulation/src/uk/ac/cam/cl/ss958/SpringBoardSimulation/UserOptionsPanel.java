package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class UserOptionsPanel extends JPanel {

	SimulationModel model;
	int displayedUser = -2;
			
	UserOptionsPanel(SimulationModel mainModel) {
		super(new GridBagLayout());
		this.model = mainModel;
		maybeUpdate();
	}
	
	private GridBagConstraints c;
	private int lastAddedY = 0;
	
	public void maybeUpdate() {
		if (model.getSelectedUser() == displayedUser) return;

		if (displayedUser >=0)
			model.getUsers().get(displayedUser).setOptionsPanel(null);
		displayedUser = model.getSelectedUser();
		removeAll();
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		c.anchor = GridBagConstraints.NORTH;
		c.gridx = 0; c.gridy = 0; c.weighty = 0.0;
		lastAddedY = 0;

		if (model.getSelectedUser() == -1) {
			
			addElement(new JLabel("No user selected."));
		} else {
			final User user = model.getUsers().get(displayedUser);
			
			addElement(new JLabel("User ID: " + model.getSelectedUser()));
			
			
			final JSpinner range = new JSpinner(new SpinnerNumberModel(user.getRange(),10,500,1));
			
			addElement(range, "Range: ");
			
			range.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					int newValue = (Integer)range.getValue();
					if (user.getRange() != newValue) {
						user.setRange(newValue);
						model.onChange();
					}
				}

			});
			user.setOptionsPanel(this);
		}
		revalidate();
	}
	
	
	public void addElement(JComponent e, String label) {
		c.gridy = lastAddedY++;
		if(label != null) {
			c.gridx = 0;
			JLabel l = new JLabel(label);
			l.setLabelFor(e);
			add(l,c);
			c.gridx = 1;
			add(e,c);
		} else {
			c.gridx = 0;
			add(e,c);
		}
	}
	
	public void addElement(JComponent e) {
		addElement(e,null);
	}
	
}
