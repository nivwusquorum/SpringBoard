package uk.ac.cam.cl.ss958.SpringBoardSimulation;

import uk.ac.cam.cl.ss958.IntegerGeometry.Point;
import java.util.Random;

public class NaturalMoveGenerator {
	private Random generator;
	private int numberOfPossibleMoves;	
	private int [] choices;
	private int movesAfterReset;
	
	protected boolean validate(int move) {
		return true;
	}
	
	protected int suggestedMove() {
		return -1;
	}
	
	public NaturalMoveGenerator(int numberOfPossibleMoves,
								Random generator) {
		this.generator = generator;
		this.numberOfPossibleMoves = numberOfPossibleMoves;
		
		choices = new int[numberOfPossibleMoves];

		movesAfterReset = 0;
	}
	
	public void reset() {
		for(int i=0; i<numberOfPossibleMoves; ++i) {
			choices[i] = 0;
		}
		movesAfterReset = 0;
		
		int sug = suggestedMove();
		if (sug != -1) {
			choices[sug] = 5;
			movesAfterReset = 5;
		}
	}
	
	public int nextStep() throws CannotPlaceUserException {
		// shoot first
		int move = -1;
		
		int chance = generator.nextInt(movesAfterReset+numberOfPossibleMoves);
		for(int i=0; i<numberOfPossibleMoves; ++i) {
			if(chance<choices[i]+1) {
				move = i;
				break;
			}
			chance -=choices[i]+1;
		}
		if (!validate(move)) {
			move = -1;		
			for (int i=0; i<numberOfPossibleMoves; ++i) {
				if(validate(i)) move = i;
			}
		}
		
		if (move != -1) {
			++movesAfterReset;
			int oppositeMove = (move+numberOfPossibleMoves/2)%numberOfPossibleMoves;
			if (choices[oppositeMove] > 0) {
				choices[move]+=2;
				choices[oppositeMove]-=1;
			} else {
				choices[move]+=1;
			}
		}
		return move;

	}
}
