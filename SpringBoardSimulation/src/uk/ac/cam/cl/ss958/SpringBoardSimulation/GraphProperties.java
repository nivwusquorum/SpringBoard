package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.math.plot.Plot2DPanel;

import sun.security.provider.certpath.AdjacencyList;

public class GraphProperties {
	public static class Graph {
		public int nodes;
		public int edges;
		List<List<Integer>> adjacencyList;
		
		public Graph(int n, List<List<Integer>> g) {
			nodes = n;
			adjacencyList = g;
		} 
	}
	
	private double avgDistance;
	private int effectiveDiameter;
	private double clusteringCoefficient;
	private double averageDegree;
	private List<Integer> triangles;
	private final int INF = 1000000000;

	Graph g;

	private static class DiameterCheckPoint {
		private long t;
		private double d;
		public DiameterCheckPoint(long t, double d) {
			this.t = t;
			this.d = d;
		}
		public long getT() { return t; }
		public double getD() { return d; }
	}
	static List<DiameterCheckPoint> diameterCheckPoints;
	
	public GraphProperties(Graph graph) {
		this.g = graph;
		computeDistances();
		computeTriangles();
		computeAverageDegree();
	}
	
	private void computeAverageDegree() {
		double ret = 0;
		for (List<Integer> l : g.adjacencyList) {
			ret+=l.size();
		}
		ret/=g.adjacencyList.size();
		averageDegree = ret;
	}
	
	private static class BfsQueueElement {
		int distance;
		int node;
		public BfsQueueElement(int n, int d) {
			distance = d;
			node = n;
		}
	}
	
	// this only considers connected pairs of nodes.
	private void computeDistances() {
		double ret = 0;
		int numberOfAnalyzedPaths = 0;
		
		List<Integer> distances = new ArrayList<Integer>();
		
		// each path will be analyzed twice, but it does not matter because,
		// we are taking the average.
		
		boolean [] visited = new boolean[g.nodes];
		for (int i=0; i< g.nodes; ++i) {
			for(int j=0; j<g.nodes; ++j) visited[j] = false;
			Queue<BfsQueueElement> bfs =
					new ArrayBlockingQueue<BfsQueueElement>(g.nodes);
			bfs.add(new BfsQueueElement(i, 0));
			visited[i] = true;
			while(bfs.size() > 0) {
				BfsQueueElement next = bfs.poll();
				if (next.distance > 0) {
					++numberOfAnalyzedPaths;
					ret += next.distance;
					distances.add(next.distance);
				}
				for (int neighbour : g.adjacencyList.get(next.node)) {
					if(!visited[neighbour]) {
						visited[neighbour] = true;
						bfs.add(new BfsQueueElement(neighbour, next.distance+1));
					}
				}
			}
			
			for (int j=0; j< g.nodes; ++j) {
				if (visited[j] == false) 
					distances.add(INF);
			}
		}
		//System.out.println("lol: " + numberOfAnalyzedPaths);
		assert numberOfAnalyzedPaths%2 == 0;
		avgDistance = numberOfAnalyzedPaths == 0 ? 0 : ret/numberOfAnalyzedPaths;
		Collections.sort(distances);
		effectiveDiameter = distances.get((int)(0.9*distances.size()));
	}
	
	private void computeTriangles() {
		double clustering = 0;
		triangles = new ArrayList<Integer>();
		
		boolean [] isFriend = new boolean[g.nodes];
		for (int i=0; i<g.nodes; ++i) isFriend[i] = false;
		for (int i=0; i<g.nodes; ++i) {
			for (int friend : g.adjacencyList.get(i))
				isFriend[friend] = true;
			int noOfFriendsConnected = 0;
			for (int friend : g.adjacencyList.get(i)) {
				for (int friendOfFriend : g.adjacencyList.get(friend)) {
					if (isFriend[friendOfFriend]) {
						++noOfFriendsConnected;
					}
				}
			}
			assert noOfFriendsConnected %2 == 0;
			triangles.add(noOfFriendsConnected/2);
			
			int fSize = g.adjacencyList.get(i).size();
			if (fSize >= 2)
				clustering += (double)noOfFriendsConnected / ((double)fSize*(fSize-1));
			for (int friend : g.adjacencyList.get(i))
				isFriend[friend] = false;
		}
		clusteringCoefficient =  clustering / g.nodes;
	}
	
	public void checkPointDynamicProperties(long timestamp) {
		if (diameterCheckPoints == null) {
			diameterCheckPoints = new ArrayList<DiameterCheckPoint>();
		}
		
		diameterCheckPoints.add(new DiameterCheckPoint(timestamp, avgDistance));
	}
	
	public void display(JPanel parent) {
		
		
		new DisplayProperties(null, "Social graph properties");
	}
	
	
	
	public class DisplayProperties extends JDialog {

		private void addBorder(JComponent component, String title) {
			Border etch = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
			Border tb = BorderFactory.createTitledBorder(etch,title);
			component.setBorder(tb);
		}
		
		private JPanel createTriangePlot() {
			Map<Integer,Integer> histogram =
					new HashMap<Integer,Integer>();
			
			for (int t : triangles) {
				Integer prev = histogram.get(t);
				if (prev != null) {
					histogram.put(t, prev+1);
				} else {
					histogram.put(t,1);
				}
			}
			
			double[] x = new double [histogram.size()];
			double[] y = new double [histogram.size()];
			
			int i = 0;
			for (int t : histogram.keySet()) {
				x[i] = t;
				y[i] = Math.log(histogram.get(t));
				++i;
			}
			

			
			// create your PlotPanel (you can use it as a JPanel)
			Plot2DPanel plot = new Plot2DPanel();
			 
			// add a line plot to the PlotPanel
			plot.addScatterPlot("my plot", x, y);
			 
		
			
			plot.setAxisLabels(new String [] { 
					"Triangles",
					"                                                  " +
					"Log of num of nodes in that number of triangles" 
			});
			
			plot.removePlotToolBar();

			 

			plot.setPreferredSize(new Dimension(400,400));
			// put the PlotPanel in a JFrame, as a JPanel
			JPanel ret = new JPanel();
			ret.setLayout(new FlowLayout());
			ret.add(plot, BorderLayout.CENTER);
			//ret.setPreferredSize(new Dimension(300,300));
			return ret;
		}
		
		private JPanel createDegreePlot() {
			Map<Integer,Integer> histogram =
					new HashMap<Integer,Integer>();
			
			for (List<Integer> list : g.adjacencyList) {
				int t = list.size();
				Integer prev = histogram.get(t);
				if (prev != null) {
					histogram.put(t, prev+1);
				} else {
					histogram.put(t,1);
				}
			}
			
			double[] x = new double [histogram.size()];
			double[] y = new double [histogram.size()];
			
			int i = 0;
			for (int t : histogram.keySet()) {
				x[i] = t;
				y[i] = Math.log(histogram.get(t));
				++i;
			}
			 
			// create your PlotPanel (you can use it as a JPanel)
			Plot2DPanel plot = new Plot2DPanel();
			
			plot.setAxisLabels(new String [] { 
					"Degree",
					"                                             " +
					"Log of number of nodes with that degree" 
			});
			
			plot.removePlotToolBar();
			
			// add a line plot to the PlotPanel
			plot.addScatterPlot("my plot", x, y);
			 
			plot.setPreferredSize(new Dimension(400,400));
			// put the PlotPanel in a JFrame, as a JPanel
			JPanel ret = new JPanel();
			ret.setLayout(new FlowLayout());
			ret.add(plot, BorderLayout.CENTER);
			//ret.setPreferredSize(new Dimension(300,300));
			return ret;
		}
		
		private JPanel createDiameterEvolutionPlot() {
			if (diameterCheckPoints == null) return null;
			double[] x = new double [diameterCheckPoints.size()];
			double[] y = new double [diameterCheckPoints.size()];
			
			int i = 0;
			for (DiameterCheckPoint c : diameterCheckPoints) {
				x[i] = c.getT();
				y[i] = c.getD();
				++i;
			}
			
			// create your PlotPanel (you can use it as a JPanel)
			Plot2DPanel plot = new Plot2DPanel();
			
			plot.setAxisLabels(new String [] { 
					"Time",
					"          " +
					"Average distance" 
			});
			
			plot.removePlotToolBar();
			
			// add a line plot to the PlotPanel
			plot.addScatterPlot("my plot", x, y);
			 
			plot.setPreferredSize(new Dimension(400,400));
			// put the PlotPanel in a JFrame, as a JPanel
			JPanel ret = new JPanel();
			ret.setLayout(new FlowLayout());
			ret.add(plot, BorderLayout.CENTER);
			//ret.setPreferredSize(new Dimension(300,300));
			return ret;
		}
		
		private JPanel createPropertiesPanel() {
			JPanel ret = new JPanel();
			 String ans  = "Properties:\n";
			 	ans += "Total nodes: " + g.nodes + "\n";
				ans += "Average distance between connected pairs of nodes: " 
				       + avgDistance + "\n";
				ans += "Clustering coefficient : " + clusteringCoefficient + "\n";
				String effectiveDiameterString = effectiveDiameter == INF ? "infinite" : "" + effectiveDiameter;
				ans += "Effective diameter: " + effectiveDiameterString + "\n";
				ans += "Average degree: " + averageDegree + "\n";
	        JTextArea text = new JTextArea(ans);
	        text.setEditable(false);
			ret.add(text);
			return ret;
		}

	    private DisplayProperties(JFrame frame, String title) {
	        super(frame, title);
	        setLayout(new GridBagLayout());
	        GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.NORTH;
			c.weighty = 0.0;
			c.insets = new Insets(2,2,2,2);

			c.gridx = 0; c.gridy = 0;
			JPanel prop = createPropertiesPanel();
			addBorder(prop, "Various Metrics");
			add(prop,c);
			
			c.gridx = 1; c.gridy = 0;
			JPanel plot0 = createDiameterEvolutionPlot();
			if (plot0 != null) {
				addBorder(plot0, "Shrinking diameter");
				add(plot0, c);
			}
			
			c.gridx = 0; c.gridy = 1;
			JPanel plot1 = createTriangePlot();
			addBorder(plot1, "Triangle Power Law");
			add(plot1, c);

			c.gridx = 1; c.gridy = 1;
			JPanel plot2 = createDegreePlot();
			addBorder(plot2, "Heavy-tailed degree distribution");
			add(plot2, c);
			
	        pack();
	        setLocationRelativeTo(frame);
	        //setSize(400, 400);
	        setVisible(true);
	    }

	  

	}
}
