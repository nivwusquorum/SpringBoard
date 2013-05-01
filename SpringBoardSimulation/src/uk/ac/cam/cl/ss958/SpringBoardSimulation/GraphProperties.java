package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

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
	private double clusteringCoefficient;
	Graph g;
	
	public GraphProperties(Graph graph) {
		this.g = graph;
		avgDistance = computeAverageDistance();
		clusteringCoefficient = computeClusteringCoefficient();
	}
	
	@Override
	public String toString() {
		String ans  = "";
		ans += "Average distance between connected pairs of nodes: " 
		       + avgDistance + "\n";
		ans += "Clustering coefficient : " + clusteringCoefficient + "\n";
		return ans;
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
	private double computeAverageDistance() {
		double ret = 0;
		int numberOfAnalyzedPaths = 0;
		
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
				}
				for (int neighbour : g.adjacencyList.get(next.node)) {
					if(!visited[neighbour]) {
						visited[neighbour] = true;
						bfs.add(new BfsQueueElement(neighbour, next.distance+1));
					}
				}
			}
		}
		//System.out.println("lol: " + numberOfAnalyzedPaths);
		assert numberOfAnalyzedPaths%2 == 0;
		return numberOfAnalyzedPaths == 0 ? 0 : ret/numberOfAnalyzedPaths;
	}
	
	private double computeClusteringCoefficient() {
		double ret = 0;
		boolean [] isFriend = new boolean[g.nodes];
		for (int i=0; i<g.nodes; ++i) isFriend[i] = false;
		for (int i=0; i<g.nodes; ++i) {
			for (int friend : g.adjacencyList.get(i))
				isFriend[friend] = true;
			int noOfFriendsConnected = 0;
		    System.out.println("For " + i);
			for (int friend : g.adjacencyList.get(i)) {
				System.out.println("   friend: " + friend);
				for (int friendOfFriend : g.adjacencyList.get(friend)) {
					if (isFriend[friendOfFriend]) {
						System.out.println("      FoF: " + friendOfFriend);
						++noOfFriendsConnected;
					}
				}
			}
			assert noOfFriendsConnected %2 == 0;
			int fSize = g.adjacencyList.get(i).size();
			if (fSize >= 2)
				ret += (double)noOfFriendsConnected / ((double)fSize*(fSize-1));
			for (int friend : g.adjacencyList.get(i))
				isFriend[friend] = false;
		}
		return ret / g.nodes;
	}
	
	private double computedDiameterOfGianatComponent() {
		return 0;
	}
}
