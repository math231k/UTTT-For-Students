/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.easv.bll.bot;

import dk.easv.bll.field.Field;
import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameManager;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;
import dk.easv.bll.move.Move;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author fauxtistic
 */
public class TestBot4 implements IBot {

    private static final String BOTNAME = "Testbot4";
    private MonteCarloTreeSearch mcts;
    
    protected int[][] prefMove =
    {
        {0, 0}, {2, 0}, {2, 2}, {1, 0}, {2, 1}, {1 , 1}
    };

    @Override
    public IMove doMove(IGameState state) {
        
        
        
        mcts = new MonteCarloTreeSearch();
        int playerNo = (state.getMoveNumber() % 2 == 0) ? 0 : 1;
        IMove move = mcts.findNextMove(new Board(state), playerNo, state).getLastMove();
        
        return move;
       
        
    }

    @Override
    public String getBotName() {
        return BOTNAME;
    }

    public class MonteCarloTreeSearch {

        private static final int WIN_SCORE = 10;
        private static final int MILLISECONDS_TO_FIND_MOVE = 1000;
        private int opponent;
        private UCT uct;
        private int simulationCounter = 0;

        public MonteCarloTreeSearch() {
            this.uct = new UCT();
        }        
        
        public Board findNextMove(Board board, int playerNo, IGameState state) {
            long start = System.currentTimeMillis();
            long end = start + 100;
            

            opponent = 1 - playerNo;
            Tree tree = new Tree();
            Node rootNode = tree.getRoot();
            rootNode.getState().setBoard(board);
            rootNode.getState().setPlayerNo(opponent);

            while (System.currentTimeMillis() < end) {
                // Phase 1 - Selection
                Node promisingNode = selectPromisingNode(rootNode);
                // Phase 2 - Expansion
                if (promisingNode.getState().getBoard().checkStatus() == Board.IN_PROGRESS) {                    
                    expandNode(promisingNode,state);
                }

                // Phase 3 - Simulation
                Node nodeToExplore = promisingNode;
                if (promisingNode.getChildArray().size() > 0) {
                    nodeToExplore = promisingNode.getRandomChildNode();
                }
                int playoutResult = simulateRandomPlayout(nodeToExplore, state);
                simulationCounter++;
                // Phase 4 - Update
                backPropogation(nodeToExplore, playoutResult);
            }

            Node winnerNode = rootNode.getChildWithMaxScore();
            tree.setRoot(winnerNode);
            System.out.println("Simulations performed: " + simulationCounter);
            return winnerNode.getState().getBoard();
        }

        private Node selectPromisingNode(Node rootNode) {
            Node node = rootNode;
            while (node.getChildArray().size() != 0) {
                node = uct.findBestNodeWithUCT(node);
            }
            return node;
        }

        private void expandNode(Node node, IGameState gameState) {
            List<State> possibleStates = node.getState().getAllPossibleStates(gameState);
            
            for (State state : possibleStates) {               
            
                Node newNode = new Node(state);
                newNode.setParent(node);
                newNode.getState().setPlayerNo(node.getState().getOpponent());
                node.getChildArray().add(newNode);
                if(node.getState().getBoard().getLastMove() == gameState.getField().getAvailableMoves().get(0)){
                    break;
                }
            }
            
        }

        private void backPropogation(Node nodeToExplore, int playerNo) {
            Node tempNode = nodeToExplore;
            while (tempNode != null) {
                tempNode.getState().incrementVisit();
                if (tempNode.getState().getPlayerNo() == playerNo) {
                    tempNode.getState().addScore(WIN_SCORE);
                }
                tempNode = tempNode.getParent();
            }
        }

        private int simulateRandomPlayout(Node node, IGameState state) {
            
            Node tempNode = new Node(node);
            State tempState = tempNode.getState();
            int boardStatus = tempState.getBoard().checkStatus();

            if (boardStatus == opponent) {
                tempNode.getParent().getState().setWinScore(Integer.MIN_VALUE);
                return boardStatus;
            }
            while (boardStatus == Board.IN_PROGRESS) {
                tempState.togglePlayer();
                if(!tempState.randomPlay(state)){
                    break;
                }
                boardStatus = tempState.getBoard().checkStatus();
                
            }
            
            
            return boardStatus;
        }

    }

    public class Tree {

        Node root;

        public Tree() {
            root = new Node();
        }

        public Tree(Node root) {
            this.root = root;
        }

        public Node getRoot() {
            return root;
        }

        public void setRoot(Node root) {
            this.root = root;
        }

        public void addChild(Node parent, Node child) {
            parent.getChildArray().add(child);
        }

    }

    public class Node {

        State state;
        Node parent;
        List<Node> childArray;

        public Node() {
            this.state = new State();
            childArray = new ArrayList<>();
        }

        public Node(State state) {
            this.state = state;
            childArray = new ArrayList<>();
        }

        public Node(State state, Node parent, List<Node> childArray) {
            this.state = state;
            this.parent = parent;
            this.childArray = childArray;
        }

        public Node(Node node) {
            this.childArray = new ArrayList<>();
            this.state = new State(node.getState());
            if (node.getParent() != null) {
                this.parent = node.getParent();
            }
            List<Node> childArray = node.getChildArray();
            for (Node child : childArray) {
                this.childArray.add(new Node(child));
            }
        }

        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public List<Node> getChildArray() {
            return childArray;
        }

        public void setChildArray(List<Node> childArray) {
            this.childArray = childArray;
        }

        public Node getRandomChildNode() {
            int noOfPossibleMoves = this.childArray.size();
            int selectRandom = (int) (Math.random() * noOfPossibleMoves);
            return this.childArray.get(selectRandom);
        }

        public Node getChildWithMaxScore() {
            return Collections.max(this.childArray, Comparator.comparing(c -> {
                return c.getState().getVisitCount();
            }));
        }

    }

    public class State {

        private Board board;
        private int playerNo;
        private int visitCount;
        private double winScore;

        public State() {
            board = new Board();
        }

        public State(State state) {
            this.board = new Board(state.getBoard());
            this.playerNo = state.getPlayerNo();
            this.visitCount = state.getVisitCount();
            this.winScore = state.getWinScore();
        }

        public State(Board board) {
            this.board = new Board(board);
        }

        Board getBoard() {
            return board;
        }

        void setBoard(Board board) {
            this.board = board;
        }

        int getPlayerNo() {
            return playerNo;
        }

        void setPlayerNo(int playerNo) {
            this.playerNo = playerNo;
        }

        int getOpponent() {
            return 1 - playerNo;
        }

        public int getVisitCount() {
            return visitCount;
        }

        public void setVisitCount(int visitCount) {
            this.visitCount = visitCount;
        }

        double getWinScore() {
            return winScore;
        }

        void setWinScore(double winScore) {
            this.winScore = winScore;
        }

        public List<State> getAllPossibleStates(IGameState state) {
            List<State> possibleStates = new ArrayList<>();
            List<IMove> availablePositions = this.board.getEmptyPositions();  
            
            for (IMove m : availablePositions) {
            
                State newState = new State(this.board);
                newState.getBoard().performMove(newState.getPlayerNo(), m);               
                newState.setPlayerNo(1 - this.playerNo);                          
                possibleStates.add(newState);
                
                if (newState.getBoard().getLastMove() == state.getField().getAvailableMoves().get(0)) {
                    break;
                }
            }
            
            
                            
            
            return possibleStates;
        }

        void incrementVisit() {
            this.visitCount++;
        }

        void addScore(double score) {
            if (this.winScore != Integer.MIN_VALUE) {
                this.winScore += score;
            }
        }

        boolean randomPlay(IGameState state) {
            try{
            List<IMove> availablePositions = this.board.getEmptyPositions();
            int totalPossibilities = availablePositions.size();
            int selectRandom = (int) (Math.random() * totalPossibilities);
            this.board.performMove(this.playerNo, availablePositions.get(selectRandom));  
            }
            catch(IndexOutOfBoundsException iob){
                System.out.println("Out of bounds");
                this.board.performMove(this.playerNo, state.getField().getAvailableMoves().get(0));
                return false;
            }
            return true;
        }

        void togglePlayer() {
            this.playerNo = 1 - this.playerNo;
            
        }

    }

    public class Board{

        public static final int IN_PROGRESS = -2;
        public static final int DRAW = -1;
        public static final int P1 = 0;
        public static final int P2 = 1;

        private GameManager gm;
        private IMove lastMove;
        private int currentPlayer;
        //private Board board;

        public Board() {
            gm = new GameManager(new GameState());
        }

        public Board(Board board) {
            
            gm = new GameManager(new GameState(board.getGM().getCurrentState()));
            
            

            
        }

        public Board(IGameState gamestate) {
            this.gm = new GameManager(gamestate);
        }
        
        public GameManager getGM()
        {
            return gm;
        }

        public void performMove(int player, IMove m) {
            currentPlayer = player;
                if (gm.updateGame(m)) {
                    lastMove = m;
                }
            

        }

        public List<IMove> getEmptyPositions() {
            return gm.getCurrentState().getField().getAvailableMoves();
        }

        public IMove getLastMove() {
            return lastMove;
        }

        public int checkStatus() {
            String status = gm.getGameOver().name();
            if (status.equals("Win")) {
                return currentPlayer;
            } else if (status.equals("Tie")) {
                return -1;
            }

            return -2;
        }

    }

    public class UCT {

        public double uctValue(int totalVisit, double nodeWinScore, int nodeVisit) {
            if (nodeVisit == 0) {
                return Integer.MAX_VALUE;
            }
            return (nodeWinScore / (double) nodeVisit) + 1.41 * Math.sqrt(Math.log(totalVisit) / (double) nodeVisit);
        }

        public Node findBestNodeWithUCT(Node node) {
            int parentVisit = node.getState().getVisitCount();
            return Collections.max(
                    node.getChildArray(),
                    Comparator.comparing(c -> uctValue(parentVisit, c.getState().getWinScore(), c.getState().getVisitCount())));
        }
    }
}


