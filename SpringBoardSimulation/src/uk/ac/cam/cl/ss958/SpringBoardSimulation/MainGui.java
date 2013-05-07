package uk.ac.cam.cl.ss958.SpringBoardSimulation;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

public class MainGui extends JFrame {
	private SimulationPanel simulationPanel;
	private GlobalOptionsPanel globalOptionsPanel;
	private UserOptionsPanel userOptionsPanel;
	private JPanel optionsPanel;
	private SimulationModel model;
	
	MainGui() throws Exception {
		super("SpringBoard Network Simulation");
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		setLayout(new BorderLayout());

		/*
		model = new RandomModel(740,670) {
			@Override
			protected void onChange() {
				modelChanged();
			}
		};
		*/
		model = new RealisticModel(740, 670, 74, 67) {
			@Override
			protected void onChange() {
				if (optionsPanel != null && globalOptionsPanel.shouldUpdateUI()) {
					if(simulationPanel != null && !simulationPanel.draw) {
						simulationPanel.draw = true;						
					}
					modelChanged();
				} else {
					if(simulationPanel != null && simulationPanel.draw) {
						simulationPanel.draw = false;
						modelChanged();
					}
				}
			}
		};
		simulationPanel = new SimulationPanel(model);
		add(createScrollPanel(simulationPanel, Strings.PANEL_SIMULATION),BorderLayout.CENTER);

		optionsPanel = createOptionsPanel();
		add(optionsPanel,BorderLayout.EAST);
		setSize(1324,768);
		
		Simulator.getInstance().setModel(model);
	}

	private void addBorder(JComponent component, String title) {
		Border etch = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		Border tb = BorderFactory.createTitledBorder(etch,title);
		component.setBorder(tb);
	}
	
	private JComponent createScrollPanel(JPanel panel, String name) {
		JPanel holder = new JPanel();
		addBorder(holder, name);
		holder.add(panel);
		return new JScrollPane(holder);
	}
	
	private void addUsersToModel(int n) {
		for(int i=0; i<n; ++i) {
			if(!model.AddRandomUser()) {
				System.out.println("Cannot place user");
			}
		}
	}
	
	private JPanel createOptionsPanel() {
		JPanel result = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTH;
		c.weighty = 0.0;
		c.insets = new Insets(2,2,2,2);
		globalOptionsPanel = new GlobalOptionsPanel(model);
		userOptionsPanel = new UserOptionsPanel(model);
		addBorder(globalOptionsPanel, Strings.PANEL_GLOBALOPT);
		c.gridx = 0; c.gridy = 0;
		result.add(globalOptionsPanel, c);
		c.gridx = 0; c.gridy = 1; c.weighty = 1.0;
		result.add(createScrollPanel(userOptionsPanel, Strings.PANEL_USEROPT), c);
		return result;
	}


	public static void main(String[] args) throws Exception {
		MainGui hello = new MainGui();
		hello.setVisible(true);
	}
	
	public void modelChanged() {
		if(simulationPanel != null) {
			simulationPanel.repaint();
		}
		if(userOptionsPanel != null) {
			userOptionsPanel.maybeUpdate();
			optionsPanel.revalidate();
		}
	}
}

