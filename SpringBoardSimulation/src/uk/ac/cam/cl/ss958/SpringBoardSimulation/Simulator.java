package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

public class Simulator extends Timer {
	private static volatile Simulator instance;
	private SimulationModel model;
	
	public static Simulator getInstance() {
		if(instance == null) {
			instance = new Simulator();
		}
		return instance;
	}

	private Simulator() {
		super(Constants.SIMULATION_STEP_LENGH_MS, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				step();
			}
		});
	}
	
	public void setModel(SimulationModel mainModel) {
		model = mainModel;
	}
	
	public static void step() {
		getInstance().model.simulationStep();

	}
}
