package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import uk.ac.cam.cl.ss958.IntegerGeometry.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.sun.xml.internal.ws.api.pipe.NextAction;

import cern.jet.random.Zeta;
import cern.jet.random.engine.MersenneTwister;

public class SpringBoardUser extends SocialUser {
	// STATIC DATA
	public static final int MESSAGES_CAPACITY_OTHER = 500;
	public static final int MESSAGES_CAPACITY_FRIEND = 500;
	public static final int MESSAGES_TOOTHER_INV_PROBABILITY = 10;
	public static final boolean USE_WIFI = true;
	public static final boolean USE_BLUETOOTH = true;
	public static final int WIFI_MIN_RANGE = 30;
	public static final int WIFI_MAX_RANGE = 50;
	public static final int WIFI_STATIONS_NUMBER = RealisticModel.SIMULATION_COMMUNITIES;
	public static final int WIFI_MAX_CONNECTIONS = 10;
	public static final int WIFI_QUERY_FREQUENCY = 100;
	public static final int WIFI_MESSAGES_PER_TICK = 7000;
	public static final int BLUETOOTH_QUERY_FREQUENCY = 100;
	public static final int BLUETOOTH_MESSAGES_PER_TICK = 60;
	public static final boolean USE_PRIORITY_BOX = false;
	public static final MessageExchangeProtocol EXCHANGE = new BloomFilterMessageExchange();
														   // new NaiveMessageExchange();
														   // new NakMessageProtocol();
														   // new LocationBasedMessageExchange();
														   // new SecondLocationBasedMessageExchange();
														   // new ThirdLocationBasedMessageExchange();
														   // new FourthLocationBasedMessageExchange();
	static Integer trackedMessageNumber;
		
	private static final Map<Integer, SpringBoardUser> users;
	private static final Zeta messagesPerDayDistribution =
			new Zeta(1.07, 0.0, new MersenneTwister((int)System.currentTimeMillis()));
	
	private static Random springboardGenerator; 
	public static SpringBoardMessageFactory mf;

	static class AccessPointNetwork {
		public class AccessPoint {
			public final Point location;
			public final int range;
			private Set<SpringBoardUser> connectedUsers;
			public AccessPoint() {
				location = new Point(springboardGenerator.nextInt(model.getWidth()),
						 springboardGenerator.nextInt(model.getHeight()));
				range = WIFI_MIN_RANGE +
						springboardGenerator.nextInt(WIFI_MAX_RANGE - WIFI_MIN_RANGE);
				connectedUsers = new HashSet<SpringBoardUser>();
			}
			
			public boolean inRange(Point where) {
				return Tools.pointsDistanceSquared(where, location) <= (double)range*range;
			}
			
			public boolean connect(SpringBoardUser u) {
				if (connectedUsers.size() < WIFI_MAX_CONNECTIONS) {
					connectedUsers.add(u);
					return true;
				} else {
					return false;
				}
			}
			
			public void disconnect(User u) {
				connectedUsers.remove(u);
			}
			
			public Set<SpringBoardUser> getConnectedUsers() {
				return connectedUsers;
			}
		}
		
		private RealisticModel model;

		List<AccessPoint> APs;
		
		public List<AccessPoint> getAccessPoints() {
			return APs;
		}
		
		public AccessPoint getClosestAccessPoint(Point toWhere) {
			double  minDist = -1;
			AccessPoint best = null;
			for (AccessPoint ap : APs) {
				double dist = Tools.pointsDistanceSquared(ap.location, toWhere);
				if (minDist == -1 || dist < minDist) {
					best = ap;
					minDist = dist;
				}
			}
			return best;
		}
		
		public AccessPointNetwork(RealisticModel model) {
			this.model = model;
			APs = new ArrayList<AccessPoint>();
			if (!USE_WIFI) return;
			for (int i=0; i<WIFI_STATIONS_NUMBER; ++i) {
				APs.add(new AccessPoint());
			}
		}
	}
	
	public static AccessPointNetwork wifi;
	
	public static List<AccessPointNetwork.AccessPoint> getAccessPoints() {
		if (wifi != null)
			return wifi.getAccessPoints(); 
		else 
			return null;
	}
	
	public static boolean wasMessagesDelivered(Integer x) {
		if (mf != null) {
			return mf.wasMessageDelivered(x);
		} else {
			return false;
		}
	}
	
	public static void displayMessageStatistics(JPanel parent) {
		if (mf != null)
			mf.display(parent);
	}
	
	public static void checkPointMessageStats() {
		if (mf != null) 
			mf.checkPointMessagesStats();
	}
	
	static {
		springboardGenerator = new Random(System.currentTimeMillis());
		users = new HashMap<Integer, SpringBoardUser>();
	}
	
	public static void setTrackedMessage(Integer tMsg) {
		trackedMessageNumber = tMsg;
	}
	
	private final int messagesPerDayTarget;
	
	private interface MessageStorage {
		public Integer addMessage(int msg, double priority);
		public Integer get(int index);
		public int getSize();
		public boolean contains(Integer x);
	}
	
	private class SimpleMessageStorage implements MessageStorage {
		private Integer [] messages;
		private Set<Integer> messagesSet;
		private final int capacity;
		private int nextSlot;
		
		public SimpleMessageStorage(int capacity) {
			this.capacity= capacity;
			messages = new Integer[capacity];
			messagesSet = new HashSet<Integer>();
			nextSlot = 0;
		}
		
		public Integer addMessage(int msg, double priority) {
			Integer ret = null;
			if (!messagesSet.contains(msg)) {
				if (messages[nextSlot] != null) {
					messagesSet.remove(messages[nextSlot]);
					mf.deleteMessage(messages[nextSlot]);
					messages[nextSlot] = null;
				}
				messages[nextSlot] = msg;
				messagesSet.add(msg);
				mf.deliverMessage(msg, SpringBoardUser.this);
				ret = nextSlot;
				nextSlot++;
				if (nextSlot == capacity)
					nextSlot = 0;
			}
			return ret;
		}	
		
		public Integer get(int index) {
			index = nextSlot -1 - index;
			if (index < 0) index +=capacity;
			return messages[index];
		}
		public int getSize() {
			return messagesSet.size();
		}
		
		public boolean contains(Integer x) {
			return messagesSet.contains(x);
		}
	}
	
	private class MessageSmartPriorityBox implements MessageStorage {
		private final int SAMPLE_MESSAGES = 1000;
		private final double [] CAPACITY_DEDICATED = {0.4, 0.3, 0.3};
		private final double [] PRIORITY_PROPORTIONS = {0.5, 0.4, 0.1 };
		
		private double [] priorityLimitForBox;
		MessageStorage [] storage; 
		
		private double [] samples;
		int nextSampleIndex = 0;
		
		private void updatePriorityDistribution(double priority) {
			samples[nextSampleIndex++] = priority;
			if (nextSampleIndex == samples.length) {
				nextSampleIndex = 0;
				Arrays.sort(samples);
				
				double prioritySoFar = 0.0;
				for (int i=0; i<priorityLimitForBox.length; ++i) {
					prioritySoFar += PRIORITY_PROPORTIONS[i];
					priorityLimitForBox[i] = samples[(int)(prioritySoFar*(samples.length-1))];
				}
				
				priorityLimitForBox[priorityLimitForBox.length-1] = 1.0;
			}
		}
		
		public MessageSmartPriorityBox(int capacity) {
			int boxes = CAPACITY_DEDICATED.length;
			storage = new MessageStorage[boxes];
			for (int i=0; i<boxes; ++i) {
				storage[i] = new SimpleMessageStorage((int)(CAPACITY_DEDICATED[i]*capacity));
			}
			priorityLimitForBox = new double [boxes];
			
			
			double sumSoFar = 0.0;
			for (int i=0; i<boxes; ++i) {
				sumSoFar += PRIORITY_PROPORTIONS[i];
				priorityLimitForBox[i] = sumSoFar;

			}
			samples = new double[SAMPLE_MESSAGES];
		}
		
		public Integer addMessage(int msg, double priority) {
			updatePriorityDistribution(priority);
			int sizeSoFar = 0;
			for (int i=0; i<storage.length; ++i) {
				if(priority <= priorityLimitForBox[i]) {
					Integer indexReceived = storage[i].addMessage(msg, priority);
					if (indexReceived == null)
						return null;
					else 
						return sizeSoFar + indexReceived;
				}
				sizeSoFar += storage[i].getSize();
			}
			assert false;
			return null;
		}
		
		public int getSize() {
			int size = 0;
			for(int i=0; i<storage.length; ++i) {
				size +=storage[i].getSize();
			}
			return size;
		}

		public Integer get(int index) {

			for(int i=0; i<storage.length; ++i) {
				if (index<storage[i].getSize()) {
					return storage[i].get(index);
				}
				index -= storage[i].getSize();
			}
			assert false;
			return null;
		}
		
		public boolean contains(Integer x) {
			for (int i=0; i<storage.length; ++i) {
				if (storage[i].contains(x))
					return true;
			}
			return false;
		}
		
	}
	
	public class MessageSystem implements ListModel { 
		MessageStorage msgFriends;
		MessageStorage msgOthers;
		
		public MessageSystem() {
			if (USE_PRIORITY_BOX) {
				msgFriends = new MessageSmartPriorityBox(MESSAGES_CAPACITY_FRIEND);
				msgOthers = new MessageSmartPriorityBox(MESSAGES_CAPACITY_OTHER);
			} else {
				msgFriends = new SimpleMessageStorage(MESSAGES_CAPACITY_FRIEND);
				msgOthers = new SimpleMessageStorage(MESSAGES_CAPACITY_OTHER);
			}
		}
		
		
		public void addMessage(int msg, boolean friend, double priority) {
			Integer index = friend ? msgFriends.addMessage(msg, priority) :
				                     msgOthers.addMessage(msg, priority);
			if (index != null) {
				index = friend ? index : msgFriends.getSize() + index;
				for (ListDataListener l : listeners) {
					l.contentsChanged(new ListDataEvent(this,
														ListDataEvent.CONTENTS_CHANGED,
														index,
														index));
				}
			}
		}
		
		public boolean contains(Integer x) {
			return msgFriends.contains(x) || msgOthers.contains(x);
		}

		@Override
		public int getSize() {
			return msgFriends.getSize() + msgOthers.getSize(); 
		}

		@Override
		public Object getElementAt(int index) {
			Integer ret = null;
			if(index<0) {
				ret = null;
			} else if(index<msgFriends.getSize()) {
				ret = msgFriends.get(index);
			} else if(index<msgFriends.getSize() + msgOthers.getSize()) {
				ret = msgOthers.get(index - msgFriends.getSize());
			} else {
				ret = null;
			}
			
			if (ret == null) return "empty";
			else return ret;
		}

		Set<ListDataListener> listeners = new HashSet<ListDataListener>();
		@Override
		public void addListDataListener(ListDataListener l) {
			listeners.add(l);
			
		}

		@Override
		public void removeListDataListener(ListDataListener l) {
			listeners.remove(l);
		}
	}
	
	private class WifiCard {
		private final int DISCONNECTED = 0;
		private final int CONNECTING = 1;
		private final int CONNECTED = 2;
		
		private final int IDLE = 0;
		private final int ACTIVE = 1;
		
		private int state = DISCONNECTED;
		private int protocolState = IDLE;
		private AccessPointNetwork.AccessPoint ap = null;
		private Set<SpringBoardUser> alreadyTriedUsers;
		
		private void disconnect() {
			if (state == CONNECTED)
				ap.disconnect(SpringBoardUser.this);
			state = DISCONNECTED;
			ap = null;
			protocolState = IDLE;
			alreadyTriedUsers = null;
		}
		
		private final int queryOffset;
		public WifiCard() {
			queryOffset = springboardGenerator.nextInt(WIFI_QUERY_FREQUENCY);
		}
		
		public void step(long tick) {
			if (!USE_WIFI) 
				return;
			if (state == DISCONNECTED) {
				AccessPointNetwork.AccessPoint near = wifi.getClosestAccessPoint(getLocation());
				if (near == null) return;
				if (near.inRange(getLocation())) {
					state = CONNECTING;
					ap = near;
				}
				return;
			} else if (state == CONNECTING) {
				if (ap.inRange(getLocation())) {
					if (ap.connect(SpringBoardUser.this)) {
						state = CONNECTED;
						// let run
					} else {
						disconnect();
						return;
					}
				} else {
					disconnect();
					return;
				}
			} else if (state == CONNECTED) {
				if (!ap.inRange(getLocation())) {
					disconnect();
					return;
				} else {
					// let run
				}
			}
			
			if  (protocolState == IDLE && tick%WIFI_QUERY_FREQUENCY == queryOffset) {
				alreadyTriedUsers = new HashSet<SpringBoardUser>();
				protocolState = ACTIVE;
			}
			
			if (protocolState == ACTIVE) {
				SpringBoardUser nextToBeContacted = null;
				for(SpringBoardUser u : ap.getConnectedUsers()) {
					if (!alreadyTriedUsers.contains(u) && 
						u.getID() != SpringBoardUser.this.getID()) {
						nextToBeContacted = u;
						break;
					}
				}
				if (nextToBeContacted == null) {
					alreadyTriedUsers = null;
					protocolState = IDLE;
				} else {
					if(EXCHANGE.exchange(SpringBoardUser.this,
									  nextToBeContacted,
									  WIFI_MESSAGES_PER_TICK)) {
						alreadyTriedUsers.add(nextToBeContacted);
					}
				}
			}
		}
	}
	
	private class BluetoothCard {
		private static final int INACTIVE = 0;
		private static final int ACTIVE = 1;
		private static final int CONNECTING = 2;
		private static final int CONNECTED = 3;
		
		private int state = INACTIVE;
		private SpringBoardUser target;
		private int queryOffset;
		private Map<SpringBoardUser, Long> lastContact;
		
		public BluetoothCard() {
			queryOffset = springboardGenerator.nextInt(BLUETOOTH_QUERY_FREQUENCY);
			lastContact = new HashMap<SpringBoardUser, Long>();
		}
		
		private double square(double a) {
			return a*a;
		}
		
		private boolean inRange(User u) {
			return Tools.pointsDistanceSquared(getLocation(), u.getLocation()) <=
					square(Math.min(SpringBoardUser.this.bluetoothRange, u.bluetoothRange));
		}
		
		private List<SpringBoardUser> getNearbyBluetooth() {
			List<User> nearbyUsers = model.getNearbyUsers(SpringBoardUser.this);
			List<SpringBoardUser> ret = new ArrayList<SpringBoardUser>();
			for (User u : nearbyUsers) {
				if (inRange(u) &&
						SpringBoardUser.this.getFriends().contains((SpringBoardUser)u)) {
					ret.add((SpringBoardUser)u);
				}
			}
			return ret;
		}
		
		private void inactivate() {
			state = INACTIVE;
			target = null;
		}
		
		public void step(long step) {
			if (!USE_BLUETOOTH) 
				return;
			if (state == INACTIVE) {
				if (step%BLUETOOTH_QUERY_FREQUENCY == queryOffset) {
					state = ACTIVE;
				} else {
					return;
				}
			}
			
			if (state == ACTIVE) {
				List<SpringBoardUser> near = getNearbyBluetooth();
				if (near.size() == 0) {
					inactivate();
					return;
				} else {
					// contact the node that you haven't contacted for longest time.
					Long best = lastContact.get(near.get(0));
					SpringBoardUser bestUser = near.get(0);
					for (SpringBoardUser u : near) {
						Long lastContactU = lastContact.get(u);
						if (best != null && (lastContactU == null || lastContactU < best)) {
							best = lastContactU;
							bestUser = u;
						}
					}
					
					target = bestUser;
					state = CONNECTING;
					return;
				}
			} else if (state == CONNECTING) {
				if (!inRange(target)) {
					state = ACTIVE;
					return;
				} else {
					state = CONNECTED;
					// let run
				}
			} 
			
			assert state == CONNECTED;
			
			if (EXCHANGE.exchange(SpringBoardUser.this, target, BLUETOOTH_MESSAGES_PER_TICK)) {
				lastContact.put(target, step);
				state = ACTIVE;
				target = null;
			}
			
		}
		
	}
	
	private WifiCard userWifi;
	private BluetoothCard userBluetooth;
	public MessageSystem messages = new MessageSystem();
	private static RealisticModel model;
	
	public static Dimension getModelDims() {
		if (model == null ) return null;
		else return new Dimension(model.getWidth(), model.getHeight());
	}
	
	public SpringBoardUser(RealisticModel mainModel)
			throws CannotPlaceUserException {
		super(mainModel);
		this.model = mainModel;
		if (wifi == null) {
			wifi = new AccessPointNetwork(mainModel);
		}
		if (mf == null) {
			mf = new SpringBoardMessageFactory(mainModel) {
				public void onMessageDelivered(Integer mId, SpringBoardUser to) {
					EXCHANGE.messageDelivered(mId, to);
					model.updateTrackedMessage();
				}
				@Override
				public void onMessageCreated(Integer mId, SpringBoardUser to) {
					EXCHANGE.messageCreated(mId, to);
				}
			};
		}
		userWifi = new WifiCard();
		userBluetooth = new BluetoothCard();
		
		messagesPerDayTarget = messagesPerDayDistribution.nextInt();
		users.put(this.getID(), this);
	}

	private SpringBoardUser generateMessageTarget() {
		if (generator.nextInt(MESSAGES_TOOTHER_INV_PROBABILITY) == 0 || friends.size() == 0) {
			SpringBoardUser r = users.get(generator.nextInt(getNumerOfUsers()));
			// Don't message myself
			while ( r.getID() == getID()) {
				r = users.get(generator.nextInt(getNumerOfUsers()));
			}
			return r;
		} else {
			return (SpringBoardUser)friends.get(generator.nextInt(friends.size()));
		}
	}
	
	
	private void maybeGenerateMessage() {
		// Hit this if with probability msg_per_day/steps_in_day. So that it gives
		// expected numer of messges per day as expected.
		if (generator.nextInt(model.SIMULATION_DAY) <= messagesPerDayTarget) {
			Integer msg = mf.getMessage(this, generateMessageTarget());
			messages.addMessage(msg, true, 1.0);
			messages.addMessage(msg, false, 1.0);
		}
	}
	
	@Override
	public void step() {
		super.step();

		maybeGenerateMessage();
		
		userWifi.step(model.getStepsExecuted());
		userBluetooth.step(model.getStepsExecuted());
		
	}
	

	

	
	@Override
	protected void drawMe(Graphics g) {
		if (trackedMessageNumber != null && 
				messages.contains(trackedMessageNumber)) {
			g.setColor(new Color(160,32,240));
			g.fillOval(getLocation().getX() - USER_RADIUS, 
					   getLocation().getY() - USER_RADIUS,
					   2*USER_RADIUS,
					   2*USER_RADIUS);
		} else {
			super.drawMe(g);
		}
	}
	
	@Override
	public void setOptionsPanel(UserOptionsPanel panel) {
		super.setOptionsPanel(panel);
		if (panel != null) {
			panel.addElement(new JLabel("" + messagesPerDayTarget), "maxMessages: ");
			final JList msgList = new JList(messages);
			msgList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			msgList.setLayoutOrientation(JList.VERTICAL_WRAP);
			msgList.setVisibleRowCount(-1);
			msgList.addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					Integer msg = (Integer)msgList.getSelectedValue();
					model.setTrackedMessage(msg);
				}
			});
			JScrollPane listScroller = new JScrollPane(msgList);
			listScroller.setPreferredSize(new Dimension(250,80));
			panel.addElement(new JLabel("messages:"));
			panel.addElement(listScroller);
		}
	}
}
