package uk.ac.bris.cs.scotlandyard.model;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.DOUBLE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.SECRET;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.BUS;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.TAXI;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.UNDERGROUND;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.CopyOnWriteArrayList;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;


public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {
private final List<Boolean> rounds;
private final Graph<Integer, Transport> graph;
private PlayerConfiguration mrX, firstDetective;
private List<PlayerConfiguration> restOfTheDetectives;
private final List<ScotlandYardPlayer> sPlayers;
private final List<Colour> pColours;	
private Colour currentPlayer;
private int currentRound, lastLocation;
private final List<Spectator> spectators;
private boolean mrXstuck, mrXcaptured,detectivesWon, mrXWon;
private Set<Colour> winningPlayers;
	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {
		spectators = new CopyOnWriteArrayList<>();
		winningPlayers = new HashSet<>();
        this.rounds = requireNonNull(rounds);
		this.graph = requireNonNull(graph);
		
		if (rounds.isEmpty()) throw new IllegalArgumentException("Empty rounds");
		if (graph.isEmpty()) throw new IllegalArgumentException("Empty graph");
		if (mrX.colour != BLACK) throw new IllegalArgumentException("MrX should be Black");
		
		ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
        for (PlayerConfiguration configuration : restOfTheDetectives)
             configurations.add(requireNonNull(configuration));
        configurations.add(0, firstDetective);
        configurations.add(0, mrX);
        
		Set<Integer> set = new HashSet<>();
        Set<Colour> set2 = new HashSet<>();
        for (PlayerConfiguration configuration : configurations) {
	         if (set.contains(configuration.location)) throw new IllegalArgumentException("Duplicate location");
	         set.add(configuration.location);
             if (set2.contains(configuration.colour)) throw new IllegalArgumentException("Wrong colour");
	         set2.add(configuration.colour);
        }
		
		for (PlayerConfiguration configuration : configurations) {
             Set<Ticket> sett = configuration.tickets.keySet();
			 if (!(sett.contains(TAXI) && sett.contains(BUS) && sett.contains(UNDERGROUND) && sett.contains(DOUBLE) && sett.contains(SECRET))) throw new IllegalArgumentException("Ticket missing");
			 if (configuration == configurations.get(0)) continue;
	         if (configuration.tickets.get(DOUBLE) != 0) throw new IllegalArgumentException("Wrong ticket");
	         if (configuration.tickets.get(SECRET) != 0) throw new IllegalArgumentException("Wrong ticket");
	    }
	
	    this.sPlayers = new ArrayList<ScotlandYardPlayer>();
	    for (PlayerConfiguration configuration : configurations) {
		      ScotlandYardPlayer temp = new ScotlandYardPlayer(configuration.player, configuration.colour, configuration.location, configuration.tickets );
	          this.sPlayers.add(temp);
		}
	
	    this.pColours = new ArrayList<Colour>();
	    for (ScotlandYardPlayer sPlayer : sPlayers) {
		      this.pColours.add(sPlayer.colour());
	    }
		this.currentPlayer = pColours.get(0);
	}
	
    //Adds the given spectator.
	@Override
	public void registerSpectator(Spectator spectator) {
		requireNonNull(spectator);
		if (!spectators.contains(spectator)) this.spectators.add(spectator);
		else throw new IllegalArgumentException("This spectator is already registered.");
	}

	//Removes the given spectator.
	@Override
	public void unregisterSpectator(Spectator spectator) {
		requireNonNull(spectator);
		if (!spectators.isEmpty()) this.spectators.remove(spectator);
        else throw new IllegalArgumentException("No spectators to unregister."); 
	}

	//Checks if the given node is unoccupied.
	private boolean checkUnoccupied(Node<Integer> destination){
		boolean check = true;
		for (ScotlandYardPlayer sPlayer : sPlayers){
			if (sPlayer.location() == destination.value()) check = false;
	    }
	    if (sPlayers.get(0).location() == destination.value()) check = true;
	    return check;
	}
	
	//Adds double moves.
	private void addDouble (Edge<Integer, Transport> edge,Set<Move> moves, Colour player) {
        for (Edge<Integer, Transport> edge2 : graph.getEdgesFrom(edge.destination())) {
		   if (checkUnoccupied(edge2.destination()) && currentRound < getRounds().size() - 1) {
		     if (edge.data() != edge2.data() || getPlayerTickets(player,Ticket.fromTransport(edge.data())).get() > 1)
		        moves.add(new DoubleMove(player, Ticket.fromTransport(edge.data()), edge.destination().value(),Ticket.fromTransport(edge2.data()),edge2.destination().value()));
			 if (getPlayerTickets(player,SECRET).get() != 0 ) {
		        moves.add(new DoubleMove(player, SECRET, edge.destination().value(),Ticket.fromTransport(edge2.data()),edge2.destination().value() ));
		        moves.add(new DoubleMove(player, Ticket.fromTransport(edge.data()), edge.destination().value(),SECRET,edge2.destination().value() ));
		     }
		     if (getPlayerTickets(player,SECRET).get() > 1)	   
		        moves.add(new DoubleMove(player, SECRET, edge.destination().value(),SECRET,edge2.destination().value()));
	       }
		}
	}
	
	//Creates and returns the valid moves.
	private Set<Move> validMove(Colour player) {
		Node<Integer> source = graph.getNode(sPlayers.get((pColours.indexOf(player))).location());
        Set<Move> moves = new HashSet<>();
		for (Edge<Integer, Transport> edge : graph.getEdgesFrom(source)){
			if (checkUnoccupied(edge.destination())){
			   if (getPlayerTickets(player,Ticket.fromTransport(edge.data())).get() != 0 )
			      moves.add(new TicketMove(player, Ticket.fromTransport(edge.data()), edge.destination().value()));
		       if (player == BLACK){
			      if (getPlayerTickets(player,SECRET).get() != 0 )	
			         moves.add(new TicketMove(player, SECRET, edge.destination().value()));
			      if (getPlayerTickets(player,DOUBLE).get() != 0 )
			         addDouble(edge, moves, player);
			  }
			}
	    }
	    if (moves.isEmpty())
           if (player != BLACK) moves.add(new PassMove(player));
	    return moves;
	}
	
	//Requests a move from the player.
	public void requestMove() {
		ScotlandYardPlayer player = sPlayers.get((pColours.indexOf(currentPlayer)));
		player.player().makeMove(this,player.location(),validMove(currentPlayer),this);
	}
	
	//Checks what type of move the given move is and implements its logic.
	private void useMove(ScotlandYardPlayer player, Move move){
		if (move instanceof PassMove) notifyMove(move);	
		if (move instanceof TicketMove) useTicket(player,(TicketMove) move);
		if (move instanceof DoubleMove) useDouble(player,(DoubleMove) move);
	}
	
	//Changes the destinations in the given Double Move with the given locations and returns the new Double Move.
	private DoubleMove makeDouble(ScotlandYardPlayer player, DoubleMove move, int location1,int location2){
        DoubleMove hidden = new DoubleMove(player.colour(),move.firstMove().ticket(),location1,move.secondMove().ticket(),location2);
        return hidden;
	}
	
	//Implements the logic when using a double move.
	private void useDouble(ScotlandYardPlayer player, DoubleMove move){
		player.removeTicket(DOUBLE);
		if (!isReveal(currentRound) && !isReveal(currentRound + 1))                            //Both rounds are hidden.
		   notifyMove(makeDouble(player,move,lastLocation,lastLocation));
		else if (!isReveal(currentRound))                                                       //First round is hidden.
		        notifyMove(makeDouble(player,move,lastLocation,move.finalDestination()));
		     else if (!isReveal(currentRound + 1))                                              //Second round is hidden.
		             notifyMove(makeDouble(player,move,move.firstMove().destination(),move.firstMove().destination()));
		          else notifyMove(move);
		useTicket(player,move.firstMove());
		useTicket(player,move.secondMove());	
	}
	
	//Implements the logic when using a ticket move.
	private void useTicket(ScotlandYardPlayer player, TicketMove move){
		player.removeTicket(move.ticket());
		player.location(move.destination());
		if (player.isMrX()) {
		   currentRound++;
		   notifyStart(currentRound);	 
		}
		if (player.isDetective()) { 
		   sPlayers.get(0).addTicket(move.ticket());
		   if (player.location() == sPlayers.get(0).location()) mrXcaptured = true;
		}
		if (!isReveal(currentRound - 1) && player.isMrX()) {
		   TicketMove hidden = new TicketMove(player.colour(),move.ticket(),lastLocation);
		   notifyMove(hidden);
		}
		else notifyMove(move);
	}
	
	//Notifies the spectators when a move is made.
	private void notifyMove(Move move) {
        for (Spectator spectator : spectators) 
            spectator.onMoveMade(this, move);
    }
	
	//Notifies the spectators when a round starts.
	private void notifyStart(int currentRound) {
        for (Spectator spectator : spectators) 
            spectator.onRoundStarted(this, currentRound);
	}
	
	//Notifies the spectators when a rotation is complete.
	private void notifyRotation() {
        if (isGameOver())  notifyGameOver();   	
	    else for (Spectator spectator : spectators) 
                 spectator.onRotationComplete(this);
	}
	
	//Notifies the spectators when the game is over.
	private void notifyGameOver() {
        for (Spectator spectator : getSpectators()) 
            spectator.onGameOver(this, getWinningPlayers());
    }
	
	//Starts the rotation.
	@Override
	public void startRotate() {
  	    if (isGameOver()) throw new IllegalStateException("Game is not over initially.");
		requestMove();
	}

	//Accepts the move from the consumer and implements the rotation logic.
	@Override
	public void accept(Move move) {   
	    ScotlandYardPlayer player = sPlayers.get((pColours.indexOf(currentPlayer)));
        if (!validMove(player.colour()).contains(requireNonNull(move))) throw new IllegalArgumentException("Invalid move");
        if (pColours.indexOf(currentPlayer) == (pColours.size() - 1)) currentPlayer = pColours.get(0);
        else currentPlayer = pColours.get(pColours.indexOf(currentPlayer) + 1);
	    useMove(player,move);	 
	    if (player == sPlayers.get((pColours.size() - 1))) notifyRotation();
        else if (mrXcaptured) notifyGameOver();
             else requestMove();
    }
	
	//Returns the spectators.
	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableList(spectators);
	}

	//Returns the players.
	@Override
	public List<Colour> getPlayers() {
		return Collections.unmodifiableList(pColours);
	}

	//Returns the winning players.
	@Override
    public Set<Colour> getWinningPlayers() {
		if (isGameOver() && detectivesWon)
		   for (Colour pColour : pColours)
			   if (pColour != BLACK)
			      winningPlayers.add(pColour);
		if (mrXWon) winningPlayers.add(BLACK);
		return Collections.unmodifiableSet(winningPlayers);
	}

	//Checks if the given round is a reveal round.
	private boolean isReveal(int round){
		return rounds.get(round);
	}
	
    //Returns the location of the given player.
	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		if (!pColours.contains(colour)) return Optional.empty();
		Integer x = sPlayers.get((pColours.indexOf(colour))).location();
		if (colour == BLACK){
		   if (currentRound != 0 && isReveal(currentRound - 1)) {
			  lastLocation = x;
			  return Optional.of(x);
		   }
		   else return Optional.of(lastLocation);
		}
		return Optional.of(x);
	}

	//Returns the tickets of the given player.
	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		if (!pColours.contains(colour)) return Optional.empty();
		Integer x = sPlayers.get((pColours.indexOf(colour))).tickets().get(ticket);
		return Optional.of(x);
	}

	//Checks if the detectives are stuck.
	private boolean detectiveStuck(){
		for (Colour pColour : pColours)
		    if (pColour != BLACK)    
		       if (!validMove(pColour).contains(new PassMove(pColour))) return false;
		return true;
	}
	
	//Checks if mrX is stuck.
	private boolean mrXStuck(){
		return validMove(BLACK).isEmpty();
	}
	
	//Checks if it`s game over.
	@Override
	public boolean isGameOver() {
	    if (mrXStuck() || (mrXcaptured)) detectivesWon = true;
        else if (currentPlayer == BLACK && currentRound > getRounds().size() - 1 || detectiveStuck()) mrXWon = true;	
             else return false;
        return true;		 
	}

	//Returns the current player.
	@Override
	public Colour getCurrentPlayer() {
		return currentPlayer;
		
	}

	//Returns the current round.
	@Override
	public int getCurrentRound() {
		return currentRound;
	}

	//Returns a list of the rounds.
	@Override
	public List<Boolean> getRounds() {
	    return Collections.unmodifiableList(rounds);
	}

	//Returns the graph.
	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<>(graph);
	}
}



