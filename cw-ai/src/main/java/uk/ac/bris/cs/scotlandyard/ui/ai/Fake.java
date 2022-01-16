package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.TicketMove;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.scotlandyard.model.Transport;
import java.util.List;
import java.util.LinkedList;
import java.util.Optional;


@ManagedAI("Fake")
public class Fake implements PlayerFactory {

	
	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	
	private static class MyPlayer implements Player {
		private final Random random = new Random();
		private boolean check;
		static private ArrayList<Colour> colours;
		private ScotlandYardView view;
		private int locationMr;
		
		
		
		//Checks if the location is occupied.
		private void check(int location){
			for(Colour colour : colours)
				if(location == view.getPlayerLocation(colour).get())
					check = true;
		} 
		
		//Calculates the score of the given location.
		private int score(int location){
			check = false;
			int score;
			Node<Integer> node = view.getGraph().getNode(location);
			score = 0;
			Node<Integer> root,child,firstNode;
			boolean visited[] = new boolean[200];
			LinkedList<Node<Integer>> queue = new LinkedList<>();
			queue.add(node);
			firstNode = node;
		    visited[node.value()] = true;
			while(queue.size() != 0){
				root = queue.poll();
				for(Edge<Integer, Transport> edge : view.getGraph().getEdgesFrom(root)){
			        child = edge.destination();	
			        if(!visited[child.value()]){	
				       check(child.value());
			           if(child.value() != locationMr) queue.add(child);
					   if(root == firstNode){
						firstNode = edge.destination();
					    if(!check) score ++;
					   }
					}
				}
			    if(check) queue.clear();
			}
			return score;
		}
		
		//Returns the destination of a TicketMove.
		private int destinationT(TicketMove move){
			return move.destination();
		}
		
		//Returns the destination of a DoubleMove.
		private int destinationD(DoubleMove move){
			return move.finalDestination();
		}
		
		//Finds the best move.
		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
				Consumer<Move> callback) {
			colours =new ArrayList<>(view.getPlayers());
		    colours.remove(BLACK);
			locationMr = location;
			this.view = view;
			int indexMove = 0;
			int maxScore = 0;
			int moveScore = 0;
			for(Move move : moves){
				if (move instanceof TicketMove) moveScore = score(destinationT((TicketMove) move));
		        if (move instanceof DoubleMove) moveScore = score(destinationD((DoubleMove) move));
				if(moveScore > maxScore){
					maxScore = moveScore;
				    indexMove = new ArrayList<>(moves).indexOf(move);
				}
			}
			callback.accept(new ArrayList<>(moves).get(indexMove));
        }
	}
}
