package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.math.plot.Plot2DPanel;

import java.awt.Point;


public class SpringBoardMessageFactory {

	private class Message {
		public final SpringBoardUser from;
		public final SpringBoardUser to;
		public final double startingDistance;
		public double time;
		int noOfCopies;
		// if false was send to smb not on friend list.
		public final boolean toFriend;
		// if false means that message disappeared before it was delivered.
		boolean wasDelivered;
		
		public Message(SpringBoardUser from, SpringBoardUser to) {
			this.from = from;
			this.to = to;
			double dx = from.getLocation().getX() - to.getLocation().getX();
			double dy = from.getLocation().getY() - to.getLocation().getY();
			startingDistance = Math.sqrt(dx*dx + dy*dy);
			toFriend = from.getFriends().contains(to);
			time = model.getStepsExecuted();
			wasDelivered = false;
			noOfCopies = 1;
		}
	}

	private RealisticModel model;
	
	public SpringBoardMessageFactory(RealisticModel m) {
		messages = new HashMap<Integer, Message>();
		processedMessages = new ArrayList<Message>();
		model = m;
	}
	
	int noOfMessages;
	Map<Integer, Message> messages;
	List<Message> processedMessages;

	public Integer getMessage(SpringBoardUser from, SpringBoardUser to) {
		messages.put(noOfMessages, new Message(from, to));
		return noOfMessages++;
	}
	
	// code is still responsible to put this messages id in the to's list.
	public synchronized void deliverMessage(Integer mId, SpringBoardUser to) {
		assert mId < noOfMessages;
		Message m = messages.get(mId);
		if (m != null) {
			m.noOfCopies++;
			if(m.to.getID() == to.getID()) {
				m.wasDelivered = true;
				m.time = model.getStepsExecuted() - m.time;
				processedMessages.add(m);
				messages.remove(mId);
			}
		}
		// if m == null then messages was probably just delivered
	}
	
	public synchronized void deleteMessage(Integer mId) {
		assert mId < noOfMessages;
		Message m = messages.get(mId);
		if(m != null) {
			m.noOfCopies--;
			if (m.noOfCopies == 0) {				
				m.wasDelivered = false;
				m.time = model.getStepsExecuted() - m.time;
				processedMessages.add(m);
				messages.remove(mId);
				
			}
		}
		// if m == null then messages was probably just delivered
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
		/*
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
		*/
		
		private JPanel createPropertiesPanel() {
			if (processedMessages.size() == 0) {
				JPanel ret = new JPanel();
				ret.add(new JLabel("Not enough data."));
				return ret;
			}
			JPanel ret = new JPanel();
			double averageDistance = 0.0;
			double averageTimeDays = 0.0;
			int deliveredMessages = 0;
			int sentToFriends = 0;
			int total = processedMessages.size();
			for (Message m : processedMessages) {
				averageDistance += m.startingDistance;
				averageTimeDays += m.time;
				if (m.toFriend) ++sentToFriends;
				if (m.wasDelivered) ++deliveredMessages;
			}
			
			averageDistance/=processedMessages.size();
			averageTimeDays/=processedMessages.size()*RealisticModel.SIMULATION_DAY;
			
			 String ans  = "Properties:\n";
			 	ans += "Total messages: " + processedMessages.size() + ", in which:\n";
				ans += "  - " + deliveredMessages + " delivered (" + (deliveredMessages*100+total/2)/total + "%)\n";
				ans += "  - " + (total - deliveredMessages) + " not delivered (" + ((total-deliveredMessages)*100+total/2)/total + "%)\n";
				ans += "  - " + sentToFriends + " to friends (" + (sentToFriends*100+total/2)/total + "%)\n";
				ans += "  - " + (total - sentToFriends) + " to others (" + ((total-sentToFriends)*100+total/2)/total + "%)\n";
				ans += "Average time (days): " + averageTimeDays +"\n";
				ans += "Average distance: " + averageDistance +"\n";
	        JTextArea text = new JTextArea(ans);
	        text.setEditable(false);
			ret.add(text);
			return ret;
		}
		
		private class PlotPoint {
			 final double x;
			 final double y;
			 public PlotPoint(double a, double b) {
				 x = a;
				 y = b;
			 }
		}
		
		private PlotPoint linearRegression(double [] x, double [] y) {
			assert x.length == y.length;
			double n = x.length;
			double sxy = 0.0;
			double sx = 0.0;
			double sx2 = 0.0;
			double sy = 0.0;
			for(int i=0; i<x.length; ++i) {
				sxy += x[i]*y[i];
				sx += x[i];
				sx2 += x[i]*x[i];
				sy += y[i];
			}
			
			double b = (n*sxy -sx*sy)/(n*sx2 - sx*sx);
			double a = (sy - b*sx)/n;
			return new PlotPoint(b,a);
		}
		
		private JPanel createTimeOfDeliveryPlot() {
			List<PlotPoint> toOthers = new ArrayList<PlotPoint>();
			List<PlotPoint> toFriends = new ArrayList<PlotPoint>();
			
			for (Message msg : processedMessages) {
				if (msg.wasDelivered) {
					if (msg.toFriend) {
						toFriends.add(new PlotPoint(msg.startingDistance, (double)msg.time/model.SIMULATION_DAY));
					} else {
						toOthers.add(new PlotPoint(msg.startingDistance, (double)msg.time/model.SIMULATION_DAY));
					}
				}
			}
			// create your PlotPanel (you can use it as a JPanel)
			Plot2DPanel plot = new Plot2DPanel();
			
			double [] x = null;
			double [] y = null;
			// adding data for friendonly
			if (toFriends.size() > 0) {
				x = new double[toFriends.size()];
				y = new double[toFriends.size()];
				int i = 0;
				for(PlotPoint p : toFriends) {
					x[i] = p.x;
					y[i] = p.y;
					++i;
				}
				Color friendsColorLight = new Color(135,206,250);
				Color friendsColorStrong = new Color(30, 144, 255);
				plot.addScatterPlot("friends", friendsColorLight, x, y);
				if (toFriends.size() > 5) {
					PlotPoint reg = linearRegression(x, y);
					y = new double[toFriends.size()];
					for (i=0; i<y.length; ++i) {
						y[i] = reg.x*x[i] + reg.y;
					}
					plot.addLinePlot("friends (regression)", friendsColorStrong, x, y);
				}
				
			}
			
			if (toOthers.size() > 0) {
				x = new double[toOthers.size()];
				y = new double[toOthers.size()];
				int i = 0;
				for(PlotPoint p : toOthers) {
					x[i] = p.x;
					y[i] = p.y;
					++i;
				}
				Color othersColorLight = new Color(132, 112, 255);
				Color othersColorStrong = new Color(106, 90, 205);
				plot.addScatterPlot("others", othersColorLight, x, y);
				if (toOthers.size() > 5) {
					PlotPoint reg = linearRegression(x, y);
					y = new double[toOthers.size()];
					for (i=0; i<y.length; ++i) {
						y[i] = reg.x*x[i] + reg.y;
					}
					plot.addLinePlot("others (regression)", othersColorStrong,x, y);
				}
					
			}
						
			plot.setAxisLabels(new String [] { 
					"distance",
					"          " +
					"time (days)" 
			});
			plot.removePlotToolBar();
			plot.addLegend("SOUTH");
			// add a line plot to the PlotPanel
			
			 
			plot.setPreferredSize(new Dimension(400,400));
			// put the PlotPanel in a JFrame, as a JPanel
			JPanel ret = new JPanel();
			ret.setLayout(new FlowLayout());
			ret.add(plot, BorderLayout.CENTER);
			//ret.setPreferredSize(new Dimension(300,300));
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
			JPanel plot0 = createTimeOfDeliveryPlot();
			if (plot0 != null) {
				addBorder(plot0, "Shrinking diameter");
				add(plot0, c);
			}
			/*
			c.gridx = 0; c.gridy = 1;
			JPanel plot1 = createTriangePlot();
			addBorder(plot1, "Triangle Power Law");
			add(plot1, c);

			c.gridx = 1; c.gridy = 1;
			JPanel plot2 = createDegreePlot();
			addBorder(plot2, "Heavy-tailed degree distribution");
			add(plot2, c);
			*/
	        pack();
	        setLocationRelativeTo(frame);
	        setVisible(true);
	    }

	  

	}
	
}
