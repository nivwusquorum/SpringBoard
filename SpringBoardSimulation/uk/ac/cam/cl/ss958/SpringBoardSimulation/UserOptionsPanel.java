package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.cam.cl.ss958.SpringBoardSimulation.SimulationModel.UserInModel;

public class UserOptionsPanel extends JPanel {

	SimulationModel model;
	int displayedUser = -2;
			
	UserOptionsPanel(SimulationModel mainModel) {
		super(new GridBagLayout());
		this.model = mainModel;
		maybeUpdate();
	}
	
	public void maybeUpdate() {
		if (model.getSelectedUser() == displayedUser) return;

		displayedUser = model.getSelectedUser();
		removeAll();
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		c.anchor = GridBagConstraints.NORTH;

		if (model.getSelectedUser() == -1) {
			c.gridx = 0; c.gridy = 0; c.weighty = 0.0;
			add(new JLabel("No user selected."), c);
		} else {
			final UserInModel user = model.getUsers().get(displayedUser);
			c.gridx = 0; c.gridy = 0; c.weighty = 0.0;
			add(new JLabel("User ID: " + model.getSelectedUser()), c);
			
			c.gridx = 0; c.gridy = 1; c.weighty = 0.5;
			add(new JLabel("Range"),c);
			final JSpinner range = new JSpinner(new SpinnerNumberModel(user.user.getRange(),10,500,1));
			c.gridx = 1; c.gridy = 1; c.weighty = 0.0;
			add(range,c);
			
			range.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					int newValue = (Integer)range.getValue();
					if (user.user.getRange() != newValue) {
						user.user.setRange(newValue);
						model.onChange();
					}
				}

			});
		}
		revalidate();
	}
	
}
