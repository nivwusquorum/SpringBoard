package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.sun.org.apache.xerces.internal.impl.xpath.XPath.Step;
import com.sun.xml.internal.bind.v2.runtime.reflect.Accessor.SetterOnlyReflection;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;

public class RealisticModel extends SimulationModel {
	static Random generator =  new Random (System.currentTimeMillis());

	public static final int SIMULATION_WAITING_INVLAMBDA = 400;
	public static final int SIMULATION_STEP_LENGH_MS = 1;

	public static final int SIMULATION_DAY = 10000;
	public static final int SIMULATION_MORNING = 5000;
	public static final int SIMULATION_EVENING = 5000;
	public static final int SIMULATION_COMMUNITIES = 50;
	public static final int SIMULATION_START_USERS = 1000;

	private final static boolean DEBUG_LATEST_USER = false;
	Map<Integer, RealisticUser> users;

	private int squareWidth;
	private int squareHeight;

	public int getSquareWidth() { return squareWidth; }
	public int getSquareHeight() { return squareHeight; }

	private static long simulationSteps = 0;

	public static long getStepsExecuted() {
		return simulationSteps;
	}

	public int getStepLengthMs() {
		return SIMULATION_STEP_LENGH_MS;
	}

	Community [] communities;

	public static RealisticModel me;
	
	public static RealisticModel getLatestInstance() {
		return me;
	}
	
	public RealisticModel(int width, int height, int squareWidth, int squareHeight) throws Exception {
		super(width, height);
		assert SIMULATION_MORNING + SIMULATION_EVENING == SIMULATION_DAY;

		users = new HashMap<Integer, RealisticUser>();

		assert width % squareWidth == 0 && height % squareHeight == 0;

		this.squareWidth = squareWidth;
		this.squareHeight = squareHeight;

		communities = new Community[SIMULATION_COMMUNITIES];

		for (int i=0; i<SIMULATION_COMMUNITIES; ++i) {
			communities[i] = new Community(this);
		}

		for(int i=0; i < SIMULATION_START_USERS; ++i) {
			AddRandomUser();
		}

		System.out.println("Total number of users placed: " + User.getNumerOfUsers());
		
		me = this;
		running = new AtomicBoolean(false);

	}

	private JLabel timeLabel;

	private JLabel executionTime;

	private JCheckBox drawSocialGraph;
	private JCheckBox drawAP;
	private JCheckBox showRanges;
	
	private JLabel trackedMessage;
	private Integer trackedMessageNumber;
	private JButton disableTracking;
	
	SpringBoardUser trackedMessageTarget;
	
	public void setTrackedMessage(Integer tMsg) {
		
		trackedMessageNumber = tMsg;
		if (tMsg == null) {
			trackedMessageTarget = null;
			trackedMessage.setText("No message tracked.");
			disableTracking.setEnabled(false);
		} else {
			String str = "Tracking message: " + tMsg;
			if (SpringBoardUser.mf != null) {
				str += SpringBoardUser.mf.wasMessageDelivered(tMsg) ? 
												" (delivered)" : " (not delivered)";
				trackedMessageTarget = SpringBoardUser.mf.getTarget(tMsg);
			}
			trackedMessage.setText(str);
			disableTracking.setEnabled(true);
		}
		SpringBoardUser.setTrackedMessage(tMsg);
		onChange();
	}
	
	public void updateTrackedMessage() {
		setTrackedMessage(trackedMessageNumber);
	}
	
	@Override
	public void addToOptionsMenu(final GlobalOptionsPanel o) {
		super.addToOptionsMenu(o);
		
		o.addElement(showRanges = new JCheckBox("Show bluetooth ranges"),30);

		
		showRanges.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				setDrawRanges(showRanges.isSelected());
			}
		});
		showRanges.setSelected(false);
		setDrawRanges(false);
		
		timeLabel = new JLabel("");
		o.addElement(timeLabel, 30);

		executionTime = new JLabel("");
		o.addElement(executionTime, 30);
		updateTimeLabel();

		drawSocialGraph = new JCheckBox();
		drawSocialGraph.setSelected(false);
		drawSocialGraph.setText("Draw Social Graph");
		drawSocialGraph.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onChange();
			}
		});
		o.addElement(drawSocialGraph, 30);
		
		
		
		drawAP = new JCheckBox();
		drawAP.setSelected(false);
		drawAP.setText("Draw Access Points");
		
		drawAP.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onChange();
			}
		});
		o.addElement(drawAP, 30);
		
		JButton socialStats = new JButton("Social Graph Statistics");
		
		o.addElement(socialStats, 30);
		
		socialStats.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				displaySocialGraphStats(o);
			}
		});
		
		JButton messageStats = new JButton("Message Statistics");
		
		o.addElement(messageStats,30);
		
		messageStats.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SpringBoardUser.displayMessageStatistics(o);
			}
		});
		
		trackedMessage = new JLabel("");
		disableTracking =new JButton("x");
		disableTracking.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setTrackedMessage(null);
			}
		});
		JPanel tracking = new JPanel();
		tracking.setLayout(
				new BoxLayout(tracking, BoxLayout.X_AXIS));
		tracking.add(trackedMessage);
		tracking.add(new JLabel("    "));
		tracking.add(disableTracking);
		o.addElement(tracking, 30);
		setTrackedMessage(null);
	}

	public void updateTimeLabel() {
		if (timeLabel == null) return;
		long stepsInDay = getStepsExecuted()%SIMULATION_DAY;
		long days = getStepsExecuted()/SIMULATION_DAY;
		boolean isMorning = stepsInDay < SIMULATION_MORNING;
		timeLabel.setText("Time: day " + days + " - " + stepsInDay +"/" + SIMULATION_DAY + " " +
				(isMorning ? "(morning)" : "(evening)") + "");

		executionTime.setText("ms per step: " +
				String.format("%.3G", 
				new Double(Math.max(0, averageStepExecutionTime))));
	}

	AtomicBoolean running;

	double averageStepExecutionTime = -1;

	public void simulationStep() {
		updateTimeLabel();

		if(running.compareAndSet(false, true)) {
			long startTime = System.nanoTime();
			++simulationSteps;

	
			for (Integer i : users.keySet()) {
				RealisticUser u = users.get(i);
				u.step();
			}

			if (getStepsExecuted()%500 == 0)
				checkPointGraphProperties();
			
			if (getStepsExecuted()%500 == 250)
				SpringBoardUser.checkPointMessageStats();
			
			SpringBoardUser.EXCHANGE.step();
			
			onChange();
			running.set(false);
			
			long executionTime = System.nanoTime() - startTime;
			double executionTimeMS = (double)executionTime/1000000.0;
			if (averageStepExecutionTime == -1) {
				averageStepExecutionTime = executionTimeMS;
			} else {
				averageStepExecutionTime = 0.999*averageStepExecutionTime + 0.001 * executionTimeMS;
			}
		}
	}

	public boolean AddRandomUser() {
		try {
			int communityIndex = generator.nextInt(communities.length);
			RealisticUser u = new RealisticUser(this, communities[communityIndex]);
			users.put(u.getID(), u);
			onChange();
			return true;
		} catch(CannotPlaceUserException e) {
			onChange();
			return false;
		}
	}

	public void clearUsers() {
		users.clear();
		selectedUser = -1;
		onChange();
	}

	@Override
	public Map<Integer, ? extends User> getUsers() {
		return users;
	}

	@Override
	public void prepaint(Graphics g) {
		g.setColor(new Color(220, 220, 220));
		int numW = width/squareWidth;
		int numH = height/squareHeight;
		for(int i=1; i<numW; ++i) {
			g.drawLine(i*squareWidth, 0, i*squareWidth, height-1);
		}
		for(int j=1; j<numH; ++j) {
			g.drawLine(0, j*squareHeight, width-1, j*squareHeight);
		}
		
		if (SpringBoardUser.getAccessPoints() != null && drawAP.isSelected()) {
			for(SpringBoardUser.AccessPointNetwork.AccessPoint ap : SpringBoardUser.getAccessPoints()) {
				g.setColor(Color.BLACK);
				g.fillOval(ap.location.getX() - 5, 
						ap.location.getY() - 5,
						   2*5,
						   2*5);
				g.setColor(Color.BLUE);
				g.drawOval(ap.location.getX() - ap.range,
						   ap.location.getY() - ap.range,
						   2*ap.range,
						   2*ap.range);
			}
		}
	}

	@Override
	public void postpaint(Graphics g) {
		if (drawSocialGraph.isSelected()) {
			if (selectedUser>=0) {
				g.setColor(new Color(255, 0, 0));
			} else {
				g.setColor(new Color(255.0f/255,64.0f/255,64.0f/255,0.25f));
			}
			
			for (Integer i : users.keySet()) {
				if (selectedUser >= 0 && i != selectedUser) continue;
				SocialUser u = users.get(i);
				for (User friend : u.getFriends()) {
					if (u.getID() < friend.getID() || selectedUser >=0) {
						g.drawLine(u.getLocation().getX(), u.getLocation().getY(),
								friend.getLocation().getX(), friend.getLocation().getY());
					}
				}
			}
		}
		if (showRanges.isSelected()) {
			boolean drawSpecificUser = selectedUser >=0;
			RealisticUser specificUser = null;
			if (drawSpecificUser) {
				specificUser = users.get(selectedUser);
				assert specificUser != null;
			}
			
			g.setColor(Colors.RANGE_COLOR);
			for (Integer i : users.keySet()) {
				SocialUser u = users.get(i);
				if (drawSpecificUser && u.getID() != selectedUser &&
					 !specificUser.getFriends().contains(u))
					continue;
				g.drawOval(u.getLocation().getX()- u.bluetoothRange,
						u.getLocation().getY()- u.bluetoothRange,
						2*u.bluetoothRange,
						2*u.bluetoothRange);
			}
		}
		if (trackedMessageTarget != null) {
			g.setColor(Color.RED);
			Point l = trackedMessageTarget.getLocation();
			g.fillOval(l.getX() - 7, l.getY() - 7,14,14);
		}
		
		super.postpaint(g);
	}
	
	private GraphProperties.Graph generateSocialGraph() {
		List<List<Integer>> adjacency =
				new ArrayList<List<Integer>>(User.getNumerOfUsers());

		for (int i = 0; i<User.getNumerOfUsers(); ++i) {
			adjacency.add(new ArrayList<Integer>());
		}
		
		for (Integer i : users.keySet()) {
			SocialUser u = users.get(i);
			List<Integer> friends = adjacency.get(u.getID());
			for (User friend : u.getFriends()) {
				friends.add(friend.getID());
			}
		}
		return new GraphProperties.Graph(User.getNumerOfUsers(), adjacency);
	}

	public void displaySocialGraphStats(JPanel parent) {
		
		
		 (new GraphProperties(generateSocialGraph())).display(parent);
	}

	public void checkPointGraphProperties() {
		(new GraphProperties(generateSocialGraph())).
		checkPointDynamicProperties(getStepsExecuted());
	}

	private static class Community {
		// top left corner of that square
		private Point morningHome, morningRemote;
		private Point eveningHome, eveningRemote;

		private int pHome, pRoam; // probability multiplied by 1000.

		private RealisticModel m;

		public Community(RealisticModel model) {
			m = model;

			int numW = m.getWidth()/m.getSquareWidth();
			int numH = m.getHeight()/m.getSquareHeight();

			morningHome = 
					new Point(generator.nextInt(numW)*m.getSquareWidth(),
							generator.nextInt(numH)*m.getSquareHeight());

			morningRemote =
					new Point(generator.nextInt(numW)*m.getSquareWidth(),
							generator.nextInt(numH)*m.getSquareHeight());

			eveningHome = 
					new Point(generator.nextInt(numW)*m.getSquareWidth(),
							generator.nextInt(numH)*m.getSquareHeight());

			eveningRemote =
					new Point(generator.nextInt(numW)*m.getSquareWidth(),
							generator.nextInt(numH)*m.getSquareHeight());

			pRoam = generator.nextInt(100)+1; // between 0.001 and 0.1
			pHome = 1000-pRoam;

			Point [] arr = new Point[] {morningHome, morningRemote,
					eveningHome, eveningRemote };
			/*
			System.out.println("Generating community");
			for (int i=0; i<arr.length; ++i) {
				System.out.println("" + arr[i].getX()+","+arr[i].getY());
			}
			System.out.println();
			 */
		}

		public Point getSquareForNextTarget() {
			long stepsInDay = m.getStepsExecuted()%SIMULATION_DAY;
			boolean isMorning = stepsInDay < SIMULATION_MORNING;

			Point offset = new Point(generator.nextInt(m.getSquareWidth()),
					generator.nextInt(m.getSquareHeight()));

			Point ret;

			if (generator.nextInt(1000)>=pHome) { // roam
				if (isMorning) ret = morningRemote;
				else ret = eveningRemote;
			} else { // home
				if (isMorning) ret = morningHome;
				else ret = eveningHome;
			}

			ret = ret.add(offset);

			return new Point(
					Math.min(
							Math.max(ret.getX(), User.USER_RADIUS+1),
							m.getWidth()-User.USER_RADIUS-1
							),
							Math.min(
									Math.max(ret.getY(), User.USER_RADIUS+1),
									m.getHeight()-User.USER_RADIUS-1)
					);
		}
	}

	private static class RealisticUser extends SpringBoardUser {

		private long wakeMeUpAt = 0;
		private RealisticMoveGenerator moveGenerator;

		private Community community;

		private RealisticModel model;

		RealisticUser(final RealisticModel m, Community c) throws CannotPlaceUserException {
			super(m);

			model = m;

			community = c;

			moveGenerator = new RealisticMoveGenerator(possibleSteps) {
				protected boolean validate(int move) {
					return model.validatePosition(getLocation().add(possibleSteps[move]), id);
				}

				@SuppressWarnings("unused")
				protected Point getCurrentLocation() {
					return getLocation();
				}

				@SuppressWarnings("unused")
				protected Point getNewTarget() {
					return community.getSquareForNextTarget();
				}

				protected RealisticModel getModel() {
					return model;
				}
			};
		}

		private JLabel locationLabel;
		private JLabel targetLabel;

		@Override
		public void setLocation(Point location) throws CannotPlaceUserException {
			super.setLocation(location);
			maybeSetLabels();
		}

		private void maybeSetLabels() {
			if (optionsPanel == null || 
					locationLabel == null ||
					targetLabel == null) return;

			Point t = moveGenerator.getTarget();
			if(wakeMeUpAt <= model.getStepsExecuted() && t != null) {
				targetLabel.setText(""+t.getX()+","+t.getY());
			} else {
				targetLabel.setText("waiting");
			}
			locationLabel.setText("" + getLocation().getX() + "," + getLocation().getY());

		}

		@Override
		public void setOptionsPanel(UserOptionsPanel panel) {
			super.setOptionsPanel(panel);
			if (panel != null) {
				locationLabel = new JLabel("");
				panel.addElement(locationLabel, "Location: ");

				targetLabel = new JLabel("");
				panel.addElement(targetLabel, "Target: ");

				maybeSetLabels();
			}
		}

		private static final Point [] possibleSteps = { 
			new Point(-1,0),
			new Point(0,-1),
			new Point(1,0),
			new Point(0,1)
		};

		// returns 0 if moving otherwise returns time of next wakeUp
		public void step() {
			super.step();
			if(wakeMeUpAt <= model.getStepsExecuted()) {
				try {
					int step = moveGenerator.advance();
					if (step == -1) throw new CannotPlaceUserException();
					setLocation(getLocation().add(possibleSteps[step]));
				} catch (CannotPlaceUserException e) { }
				if (moveGenerator.movingDone()) {
					wakeMeUpAt = model.getStepsExecuted() +
							(int)generator.nextExponential(
									1.0/SIMULATION_WAITING_INVLAMBDA);
					maybeSetLabels();
				}
			}
		}
	}

	private static abstract class RealisticMoveGenerator {

		private static Point [] possibleSteps;

		public RealisticMoveGenerator(final Point [] possibleSteps) {
			this.possibleSteps = possibleSteps;
		}

		private Point target = null;

		public Point getTarget() {
			return target;
		}

		double square(int x) {
			return (double)x * x;
		}

		double getDistanceToTarget(Point from) {
			if (target == null) return 0;
			return square(from.getX()-target.getX()) +
					square(from.getY()-target.getY());

		}

		int stepsMade;
		double startingDistanceToTarget;

		public boolean embarassinglyBadProgress() {
			double distanceTraveled =
					startingDistanceToTarget -
					getDistanceToTarget(getCurrentLocation());
			return stepsMade > 15 && distanceTraveled/square(stepsMade) < 0.5;
		}

		int backOffTime = 0;
		int backOffStep = 0;

		public int advance() {

			if (movingDone())  {
				target = getNewTarget();
				stepsMade = 0;
				startingDistanceToTarget = getDistanceToTarget(getCurrentLocation());
			}

			if(embarassinglyBadProgress()) {
				stepsMade = 0;
				backOffTime = generator.nextInt(100);
				startingDistanceToTarget = getDistanceToTarget(getCurrentLocation());
				backOffStep = generator.nextInt(possibleSteps.length);
			}

			if(backOffTime > 0) {
				backOffTime--;
				if (validate(backOffStep)) {
					return backOffStep;
				} else {
					return -1;
				}
			}

			double minDistance = 1e100;
			int bestMove = -1;
			for (int i=0; i< possibleSteps.length; ++i) {
				if (validate(i)) {
					double curDistance = getDistanceToTarget(getCurrentLocation().add(possibleSteps[i]));
					if(curDistance < minDistance) {
						minDistance = curDistance;
						bestMove = i;
					}
				}
			}
			++stepsMade;
			return bestMove;
		}

		public boolean movingDone() {
			return target == null || Tools.pointsEqual(target, getCurrentLocation());
		}

		protected boolean validate(int move) {
			return false;
		}

		protected Point getCurrentLocation() { return null; }

		protected Point getNewTarget() { return null; }

		protected abstract RealisticModel getModel();
	}


}
