import java.util.Collection;
import java.util.TreeMap;

public class Gooses implements BotAPI {

    // The public API of Bot must not change
    // This is ONLY class that you can edit in the program
    // Rename Bot to the name of your team. Use camel case.
    // Bot may not alter the state of the game objects
    // It may only inspect the state of the board and the player objects

    private PlayerAPI me;
    private OpponentAPI opponent;
    private BoardAPI board;
    private UserInterfaceAPI info;
    private DictionaryAPI dictionary;
    private int turnCount;

    Gooses(PlayerAPI me, OpponentAPI opponent, BoardAPI board, UserInterfaceAPI ui, DictionaryAPI dictionary) {
        this.me = me;
        this.opponent = opponent;
        this.board = board;
        this.info = ui;
        this.dictionary = dictionary;
        turnCount = 0;
    }

    // nested node class for GADDAG data structure
    private static class Node implements Comparable<Node>{
        private boolean finite;
        private char value;
        public TreeMap<Character, Node> tree = new TreeMap<>();

        public Node(char value) {
            this.value = value;
            this.finite = false;
        }

        // getters and setters
        public char getValue() {
            return this.value;
        }
        public void setFinite(boolean finite) {
            this.finite = finite;
        }
        public boolean getFinite() {
            return finite;
        }
        public Collection<Character> getKeySet() {
            return tree.keySet();
        }
        public Collection<Node> getChildrenValues(){
            return tree.values();
        }
        public Node getChild(char value) {
            return tree.get(value);
        }

        // info methods
        public boolean hasChild(char value) {
            return tree.containsKey(value);
        }

        // modifier methods
        public void addChild(char value) {
            tree.put(value, new Node(value));
        }

        @Override
        public boolean equals(Object obj) {
           if(obj instanceof Node){
               return (this.value == ((Node) obj).getValue());
           } else {
               return false;
           }
        }

        @Override
        public int compareTo(Node node) {
            return this.value - node.value;
        }
    }
    /* END OF NESTED NODE CLASS */

    public String getCommand() {
        // Add your code here to input your commands
        // Your code must give the command NAME <botname> at the start of the game
        String command = "";
        switch (turnCount) {
            case 0:
                command = "NAME gooses";
                break;
            case 1:
                //TODO make move
        }
        turnCount++;
        return command;
    }

}


