import java.io.*;
import java.util.*;

import sun.swing.UIAction;

public class GoosesOld implements BotAPI {

    // The public API of Bot must not change
    // This is ONLY class that you can edit in the program
    // Rename Bot to the name of your team. Use camel case.
    // Bot may not alter the state of the game objects
    // It may only inspect the state of the board and the player objects

    private PlayerAPI me;
    private UserInterfaceAPI ui;
    private BoardAPI board;
    private DictionaryAPI dictionary;
    private int turnCount;
    private GADDAG gaddag;

    GoosesOld(PlayerAPI me, OpponentAPI opponent, BoardAPI board, UserInterfaceAPI ui, DictionaryAPI dictionary)
            throws FileNotFoundException {
    	this.ui = ui;
        this.me = me;
        this.board = board;
        this.dictionary = dictionary;
        turnCount = 0;
        this.gaddag = new GADDAG();
    }

    public static int boardBoundaries(int position, boolean top) {
        if (top) {
            return Math.max(position - 1, 0);
        } else {
            return Math.min(position + 1, Board.BOARD_SIZE - 1);
        }
    }

    //nested extension for square class
    private static class SquareExtended {
        public Set<Character> legalHorizontalSet, legalVerticalSet;
        public static String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        public SquareExtended() {
            this.legalHorizontalSet = new HashSet<>();
            this.legalVerticalSet = new HashSet<>();
            for (char c = 'A'; c <= 'Z'; c++) {
                this.legalHorizontalSet.add(c);
                this.legalVerticalSet.add(c);
            }
        }

        public void addAllToLegalHorizontal(Set<Character> endSet) {
            for (Character t : endSet) {
                if (alphabet.contains(t.toString()))
                    legalHorizontalSet.add(t);
            }
        }

        public void addAllToLegalVertical(Set<Character> endSet) {
            for (Character t : endSet) {
                if (alphabet.contains(t.toString()))
                    legalVerticalSet.add(t);
            }
        }

        public boolean legalHorizontal(Character c) {
            return legalHorizontalSet.contains(c);
        }

        public boolean legalVertical(Character c) {
            return legalVerticalSet.contains(c);
        }

        public Set<Character> getLegalHorizontalSet() {
            return legalHorizontalSet;
        }

        public Set<Character> getLegalVerticalSet() {
            return legalVerticalSet;
        }


        public void addLegalHorizontalSet(char t) {
            this.getLegalHorizontalSet().add(t);
        }

        public void addLegalVerticalSet(char t) {
            this.getLegalVerticalSet().add(t);
        }


    }

    //nested extension for board class
    private static class BoardExtended extends Board {
        private SquareExtended[][] duplicateSquares;
        private boolean[][] anchors = new boolean[Board.BOARD_SIZE][Board.BOARD_SIZE];
        private BoardAPI board;

        public BoardExtended(BoardAPI board) {
            this.board = board;
            this.duplicateSquares = new SquareExtended[Board.BOARD_SIZE][Board.BOARD_SIZE];
            for (int i = 0; i < Board.BOARD_SIZE; i++) {
                for (int j = 0; j < Board.BOARD_SIZE; j++) {
                    this.duplicateSquares[i][j] = new SquareExtended();
                }
            }
        }

        public SquareExtended getSquareExtended(int i, int j) {
            return duplicateSquares[i][j];
        }

        public boolean isAnchor(int row, int column) {
            return anchors[row][column];
        }

        private boolean isValidAnchor(int row, int column) {
            if (!this.board.getSquareCopy(row, column).isOccupied()) {
                if (row == Board.BOARD_CENTRE && column == Board.BOARD_CENTRE) {
                    return true;
                }
                int boxTop = Math.max(row - 1, 0);
                int boxBottom = Math.min(row + 1, BOARD_SIZE - 1);
                int boxLeft = Math.max(column - 1, 0);
                int boxRight = Math.min(column + 1, BOARD_SIZE - 1);
                return this.board.getSquareCopy(boxTop, column).isOccupied() || this.board.getSquareCopy(boxBottom, column).isOccupied() || this.board.getSquareCopy(row, boxLeft).isOccupied() || this.board.getSquareCopy(row, boxRight).isOccupied();
            }
            return false;
        }

        public void getAnchors() {
            for (int i = 0; i < Board.BOARD_SIZE; i++) {
                for (int j = 0; j < Board.BOARD_SIZE; j++) {
                    this.anchors[i][j] = isValidAnchor(i, j);
                }
            }
        }

        public void getCrossSets(BoardExtended boardDupe, Node g, BoardAPI board) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                for (int i = 0; i < Board.BOARD_SIZE; i++) {
                    if (boardDupe.isAnchor(i, j)) {
                        if (board.getSquareCopy(i + 1, j).isOccupied() || board.getSquareCopy(i - 1, j).isOccupied()) {
                            boardDupe.getSquareExtended(i, j).getLegalHorizontalSet().clear();
                            getHorizontalCrossSet(i, j, g, board, boardDupe);
                        }
                        if (board.getSquareCopy(i, j + 1).isOccupied() || board.getSquareCopy(i, j - 1).isOccupied()) {
                            boardDupe.getSquareExtended(i, j).getLegalVerticalSet().clear();
                            computeVerticalCrossSet(i, j, g, board, boardDupe);
                        }
                    }
                }
            }
        }

        private void getHorizontalCrossSet(int row, int column, Node root, BoardAPI board, BoardExtended boardExtended) {
            Node word = root;
            // gets prefix hook
            if (board.getSquareCopy(row - 1, column).isOccupied() && board.getSquareCopy(row + 1, column).isOccupied()) { // if there is a tile left & right of position
                int adjacentRow = row - 1;
                while (board.getSquareCopy(adjacentRow, column).isOccupied()) { // go to start of prefix
                    word = word.getChild(board.getSquareCopy(adjacentRow, column).getTile().getLetter());
                    if (word == null) {
                        return;
                    }
                    adjacentRow--;
                }

                // construct suffix
                word = word.getChild('$');
                if (word != null) {
                    Node base = word;
                    for (char c = 'A'; c <= 'Z'; c++) {
                        word = base;
                        word = word.getChild(c);
                        adjacentRow = row + 1;
                        while (word != null && board.getSquareCopy(adjacentRow + 1, column).isOccupied()) {
                            word = word.getChild(board.getSquareCopy(adjacentRow, column).getTile().getLetter());
                            adjacentRow++;
                        }
                        if (word != null) {
                            if (word.isEnd(board.getSquareCopy(adjacentRow, column).getTile().getLetter())) {
                                duplicateSquares[row][column].addLegalHorizontalSet(c);
                            }
                        }
                    }
                }
            } else if (board.getSquareCopy(row - 1, column).isOccupied()) { // if left is occupied
                int adjacentRow = row - 1;
                while (board.getSquareCopy(adjacentRow, column).isOccupied()) {
                    word = word.getChild(board.getSquareCopy(adjacentRow, column).getTile().getLetter());
                    if (word == null) {
                        return;
                    }
                    adjacentRow--;
                }
                word = word.getChild('$');
                if (word != null) { //i.e word complete - no suffix
                    boardExtended.getSquareExtended(row, column).addAllToLegalHorizontal(word.getEndSet());
                }
            } else if (board.getSquareCopy(row + 1, column).isOccupied()) { // if right is occupied
                int adjacentRow = row + 1;
                while (board.getSquareCopy(adjacentRow + 1, column).isOccupied()) { // go to end of occupied right-side tiles
                    adjacentRow++;
                }
                while (adjacentRow > row) { // get all letters for prefix
                    word = word.getChild(board.getSquareCopy(adjacentRow, column).getTile().getLetter());
                    if (word == null) {
                        return;
                    }
                    adjacentRow--;
                }
                boardExtended.getSquareExtended(row, column).addAllToLegalHorizontal(word.getEndSet());
            }
        }

        // works same as above
        private void computeVerticalCrossSet(int i, int j, Node root, BoardAPI board, BoardExtended boardDupe) {
            Node word = root;
            if (board.getSquareCopy(i, j - 1).isOccupied() && board.getSquareCopy(i, j + 1).isOccupied()) {
                int adjacentColumn = j - 1;
                while (board.getSquareCopy(i, adjacentColumn).isOccupied()) {
                    word = word.getChild(board.getSquareCopy(i, adjacentColumn).getTile().getLetter());
                    if (word == null) {
                        return;
                    }
                    adjacentColumn--;
                }
                word = word.getChild('$');
                if (word != null) {
                    Node base = word;
                    for (char c = 'A'; c <= 'Z'; c++) {
                        word = base;
                        word = word.getChild(c);
                        adjacentColumn = j + 1;
                        while (word != null && board.getSquareCopy(i, adjacentColumn + 1).isOccupied()) {
                            word = word.getChild(board.getSquareCopy(i, adjacentColumn).getTile().getLetter());
                            adjacentColumn++;
                        }
                        if (word != null) {
                            if (word.isEnd(board.getSquareCopy(i, adjacentColumn).getTile().getLetter())) {
                                boardDupe.getSquareExtended(i, j).addLegalVerticalSet(c);
                            }
                        }
                    }
                }
            } else if (board.getSquareCopy(i, j - 1).isOccupied()) {
                int adjacentColumn = j - 1;
                while (board.getSquareCopy(i, adjacentColumn).isOccupied()) {
                    word = word.getChild(board.getSquareCopy(i, adjacentColumn).getTile().getLetter());
                    if (word == null) {
                        return;
                    }
                    adjacentColumn--;
                }
                word = word.getChild('$');
                if (word != null) {
                    boardDupe.getSquareExtended(i, j).addAllToLegalVertical(word.getEndSet());
                }
            } else if (board.getSquareCopy(i, j + 1).isOccupied()) {
                int adjacentColumn = j + 1;
                while (board.getSquareCopy(i, adjacentColumn + 1).isOccupied()) {
                    adjacentColumn++;
                }
                while (adjacentColumn > j) {
                    word = word.getChild(board.getSquareCopy(i, adjacentColumn).getTile().getLetter());
                    if (word == null) {
                        return;
                    }
                    adjacentColumn--;
                }
                boardDupe.getSquareExtended(i, j).addAllToLegalVertical(word.getEndSet());
            }
        }
    }
    /* -------------------- BOARD EXTENSION COMPLETE ------------- */

    //nested class for place data
    private static class Place {
        private int row;
        private int column;
        private char letter;

        public Place(int row, int column, char letter) {
            this.row = row;
            this.column = column;
            this.letter = letter;
        }

        //getters and setters
        public int getRow() {
            return this.row;
        }

        public int getColumn() {
            return this.column;
        }

        public String toString() {
            return this.letter + "";
        }
    }
    /* ---------------- PLACE CLASS COMPLETE */

    // nested class for potential moves
    private static class Move implements Iterable<Place> {
        private List<Place> places;
        private int points;
        private boolean usesAllTiles;

        public Move() {
            this.places = new ArrayList<>();
            this.points = -1;
        }

        public Move(Move input) {
            this.places = new ArrayList<>(input.places);
            this.points = input.points;
            this.usesAllTiles = input.usesAllTiles;
        }

        public void addPlay(int row, int column, char letter) {
            this.places.add(new Place(row, column, letter));
        }

        public String toString() {
            StringBuilder command = new StringBuilder();
            command.append((char) (places.get(0).getColumn() + 'A'));
            command.append(places.get(0).getRow() + 1);
            if (places.get(0).getRow() == places.get(1).getRow()) {
                command.append(" A ");
            } else {
                command.append(" D ");
            }
            for(Place p : places) {
                command.append(p.letter);
            }
            return command.toString();
        }

        @Override
        public Iterator<Place> iterator() {
            return places.iterator();
        }
    }

    // nested node class for GADDAG data structure
    private static class Node {
        private Node[] children;
        private char[] letters;
        private char[] end;
        byte numChildren = 0;
        byte endSize = 0;

        public Node() {
            this.children = new Node[1];
            this.letters = new char[1];
            this.end = new char[1];
        }

        public Node addNode(char inputLetter, Node node) {
            Node child = this.getChild(inputLetter);
            if (child == null) {
                children = ensureSpace(children, numChildren);
                letters = ensureSpace(letters, numChildren);
                letters[numChildren] = inputLetter;
                children[numChildren] = node;
                numChildren++;
                return node;
            } else {
                return child;
            }
        }

        public Node addNode(char inputLetter) {
            return this.addNode(inputLetter, new Node());
        }

        public Node getChild(char inputLetter) {
            for (int i = 0; i < numChildren; i++) {
                if (letters[i] == inputLetter) {
                    return children[i];
                }
            }
            return null;
        }

        public void addToEndSet(char endChar) {
            end = ensureSpace(end, endSize);
            end[endSize] = endChar;
            endSize++;
        }

        public boolean isEnd(char endChar) {
            return contains(end, endChar);
        }

        private boolean contains(char[] array, char endChar) {
            for (char c : array) {
                if (c == endChar)
                    return true;
            }
            return false;
        }

        public char[] getLetters() {
            return letters;
        }

        private char[] ensureSpace(char[] array, int size) {
            if (size >= array.length) {
                return Arrays.copyOf(array, array.length * 2);
            }
            return array;
        }

        private <T> T[] ensureSpace(T[] array, int insertionPoint) {
            if (insertionPoint >= array.length) {
                return Arrays.copyOf(array, array.length * 2);
            }
            return array;
        }

        @Override
        public String toString() {
            return " Trans: " + Arrays.toString(this.getLetters())
                    + "\nEnd: " + Arrays.toString(this.getEnd()) + "\n";
        }

        public char[] getEnd() {
            return end;
        }

        public Set<Character> getEndSet() {
            Set<Character> endSet = new HashSet<>();
            for (char c : end) {
                endSet.add(c);
            }
            return endSet;
        }
    }
    /* END OF NESTED NODE CLASS */

    // Nested Class for GADDAG Implementation
    private static class GADDAG {
        Node rootMin;

        public GADDAG() throws FileNotFoundException {
            Scanner initialize = new Scanner(new File("csw.txt"));
            rootMin = build(initialize);
        }

        public Node getRoot() {
            return rootMin;
        }

        public static Node build(Scanner words) {
            System.out.println("Building GADDAG...");
            Node root = new Node();
            while (words.hasNext()) {
                Node node = root;
                String word = words.next();
                String reversedWord = reverse(word);
                buildBranch(node, reversedWord);
                String reversedWordWithSeparator = reverse(word.substring(0, word.length() - 1)) + "$" + word.substring(word.length() - 1);
                node = buildBranch(root, reversedWordWithSeparator);
                for (int m = word.length() - 3; m >= 0; m--) {
                    Node temp = node;
                    node = root;
                    for (int i = m; i >= 0; i--) {
                        node = node.addNode(word.charAt(i));
                    }
                    node = node.addNode('$');
                    node.addNode(word.charAt(m + 1), temp);
                }
            }
            System.out.println("GADDAG done!");
            return root;
        }

        private static Node buildBranch(Node root, String word) {
            Node traverse = root;
            for (int i = 0; i < word.length() - 1; i++) {
                traverse = traverse.addNode(word.charAt(i));
                if (i == word.length() - 2) {
                    traverse.addToEndSet(word.charAt(word.length() - 1));
                }
            }
            return traverse;
        }

        private static String reverse(String substring) {
            return (new StringBuffer(substring)).reverse().toString();
        }

        // FIND MOVES METHODS
        public List<Move> findMoves(Node root, ArrayList<Character> rack, BoardExtended boardExtended, BoardAPI board) {
            List<Move> moves = new ArrayList<>();
            for (int row = 0; row < Board.BOARD_SIZE; row++) {
                for (int column = 0; column < Board.BOARD_SIZE; column++) {
                    if (boardExtended.isAnchor(row, column)) { // i.e for all anchors
                        findRowMoves(0, row, column, new Move(), rack, root, board, moves, true, boardExtended);
                        findColumnMoves(0, row, column, new Move(), rack, root, board, moves, true, boardExtended);
                    }
                }
            }
            return moves;
        }

        public void findRowMoves(int offset, int anchorRow, int anchorColumn, Move workingMove, ArrayList<Character> frame, Node currNode, BoardAPI board, List<Move> moves, boolean reverse, BoardExtended boardExtended) {
            if (anchorRow + offset >= Board.BOARD_SIZE || anchorRow + offset < 0) {
                return;
            }

            if (board.getSquareCopy(anchorRow + offset, anchorColumn).isOccupied()) { // include any letters on left of anchor
                char letter = board.getSquareCopy(anchorRow + offset, anchorColumn).getTile().getLetter();
                Node nextNode = currNode.getChild(letter);
                Move newMove = new Move(workingMove);
                newMove.addPlay(anchorRow + offset, anchorColumn, letter);
                findRowMovesNextPosition(offset, anchorRow, anchorColumn, letter, newMove, frame, currNode, nextNode, board, moves, reverse, boardExtended); //move on to the next square
            } else if (!frame.isEmpty()) {
                for (Character c : frame) {
                    if (c != Tile.BLANK && boardExtended.getSquareExtended(anchorRow + offset, anchorColumn).legalVertical(c)) { //if not blank + is legal to play in next square
                        ArrayList<Character> newRack = new ArrayList<>(frame);
                        newRack.remove(c);
                        Node nextNode = currNode.getChild(c);
                        Move newMove = new Move(workingMove);
                        newMove.addPlay(anchorRow + offset, anchorColumn, c);
                        findRowMovesNextPosition(offset, anchorRow, anchorColumn, c, newMove, newRack, currNode, nextNode, board, moves, reverse, boardExtended);
                    }
                }
            }
        }

        public void findRowMovesNextPosition(int offset, int anchorRow, int anchorColumn, char letter, Move workingMove, ArrayList<Character> frame, Node currNode, Node nextNode, BoardAPI board, List<Move> moves, boolean reverse, BoardExtended boardExtended) {
            if (offset <= 0) { //if making prefix
                if (currNode.isEnd(letter) && !board.getSquareCopy(boardBoundaries(anchorRow + offset - 1, true), anchorColumn).isOccupied() && !board.getSquareCopy(boardBoundaries(anchorRow + 1, false), anchorColumn).isOccupied()) { // if its a valid move
                    if (frame.isEmpty())
                        workingMove.usesAllTiles = true;
                    moves.add(workingMove);
                }

                if (nextNode != null) { // generate next prefix
                    findRowMoves(offset - 1, anchorRow, anchorColumn, workingMove, frame, nextNode, board, moves, reverse, boardExtended);

                    nextNode = nextNode.getChild('$');
                    if (nextNode != null && !board.getSquareCopy(boardBoundaries(anchorRow + offset - 1, true), anchorColumn).isOccupied()) { // generate suffixes
                        findRowMoves(1, anchorRow, anchorColumn, workingMove, frame, nextNode, board, moves, false, boardExtended);
                    }
                }
            } else if (offset > 0) {//if making suffix
                if (currNode.isEnd(letter) && !board.getSquareCopy(boardBoundaries(anchorRow + offset + 1, false), anchorColumn).isOccupied()) { // if its a valid move
                    if (frame.isEmpty()) {
                        workingMove.usesAllTiles = true;
                    }
                    moves.add(workingMove);
                }
                if (nextNode != null && !board.getSquareCopy(boardBoundaries(anchorRow + offset + 1, false), anchorColumn).isOccupied()) { // generate suffixes
                    currNode = nextNode;
                    findRowMoves(offset + 1, anchorRow, anchorColumn, workingMove, frame, currNode, board, moves, reverse, boardExtended);
                }
            }
        }

        public void findColumnMoves(int offset, int anchorRow, int anchorColumn, Move workingMove, ArrayList<Character> rack, Node currNode, BoardAPI board, List<Move> moves, boolean reverse, BoardExtended boardExtended) {
            if (anchorColumn + offset >= Board.BOARD_SIZE || anchorColumn + offset < 0) {
                return;
            }

            if (board.getSquareCopy(anchorRow, anchorColumn + offset).isOccupied()) { // include any letters above anchor
                char l = board.getSquareCopy(anchorRow, anchorColumn + offset).getTile().getLetter();
                Node nextNode = currNode.getChild(l);
                Move newMove = new Move(workingMove);
                newMove.addPlay(anchorRow, anchorColumn + offset, l);
                findColumnMovesNextPosition(offset, anchorRow, anchorColumn, l, newMove, rack, currNode, nextNode, board, moves, reverse, boardExtended); //move on to the next square
            } else if (!rack.isEmpty()) {
                for (Character c : rack) {
                    if (c != Tile.BLANK && boardExtended.getSquareExtended(anchorRow, anchorColumn + offset).legalHorizontal(c)) { //if not blank + is legal to play in next square
                        ArrayList<Character> newRack = new ArrayList<>(rack);
                        newRack.remove(c);
                        Node nextNode = currNode.getChild(c);
                        Move newMove = new Move(workingMove);
                        newMove.addPlay(anchorRow, anchorColumn + offset, c);
                        findColumnMovesNextPosition(offset, anchorRow, anchorColumn, c, newMove, newRack, currNode, nextNode, board, moves, reverse, boardExtended);
                    }
                }
            }
        }

        public void findColumnMovesNextPosition(int offset, int anchorRow, int anchorColumn, char letter, Move workingMove, ArrayList<Character> rack, Node currNode, Node nextNode, BoardAPI board, List<Move> moves, boolean reverse, BoardExtended boardExtended) {
            if (offset <= 0) {    //if making prefix
                if (currNode.isEnd(letter) && !board.getSquareCopy(boardBoundaries(anchorRow + offset - 1, true), anchorColumn).isOccupied() && !board.getSquareCopy(anchorRow, boardBoundaries(anchorColumn + 1, false)).isOccupied()) {
                    if (rack.isEmpty())
                        workingMove.usesAllTiles = true;
                    moves.add(workingMove);
                }
                if (nextNode != null) { // generate prefixes
                    findColumnMoves(offset - 1, anchorRow, anchorColumn, workingMove, rack, nextNode, board, moves, reverse, boardExtended);
                    nextNode = nextNode.getChild('$');
                    if (nextNode != null && !board.getSquareCopy(anchorRow, boardBoundaries(anchorColumn + offset - 1, true)).isOccupied()) { // generate suffixes
                        findColumnMoves(1, anchorRow, anchorColumn, workingMove, rack, nextNode, board, moves, false, boardExtended);
                    }
                }
            } else if (offset > 0) { // generate suffixes
                if (currNode.isEnd(letter) && !board.getSquareCopy(anchorRow, boardBoundaries(anchorColumn + offset + 1, false)).isOccupied()) {
                    if (rack.isEmpty()) {
                        workingMove.usesAllTiles = true;
                    }
                    moves.add(workingMove);
                }
                if (nextNode != null && !board.getSquareCopy(anchorRow, boardBoundaries(anchorColumn + offset + 1, false)).isOccupied()) { // generate suffixes
                    currNode = nextNode;
                    findColumnMoves(offset + 1, anchorRow, anchorColumn, workingMove, rack, currNode, board, moves, reverse, boardExtended);
                }
            }
        }
    }
    /* END OF NESTED GADDAG CLASS */

    private ArrayList<Character> parseFrame(String frame) {
        frame = frame.substring(1, frame.length() - 1);
        frame = frame.replaceAll(", ", "");
        ArrayList<Character> output = new ArrayList<>();
        for (char c : frame.toCharArray()) {
            output.add(c);
        }
        return output;
    }

    public Move testGADDAG(String frame, BoardExtended boardExtended) {
        ArrayList<Character> frameArray = parseFrame(frame);
        boardExtended.getCrossSets(boardExtended, gaddag.getRoot(), this.board);
        boardExtended.getAnchors();
        long searchTime = System.currentTimeMillis();
        List<Move> moves = this.gaddag.findMoves(gaddag.getRoot(), frameArray, boardExtended, this.board);
        String highestWord = null;
        int highestScoringPoints = 0;
        Move highestScoringMove = new Move();
        for (Move move : moves) {
            StringBuilder word = new StringBuilder();
            for (Place place : move) {
                word.append(place.letter);
            }
            ArrayList<Word> check = new ArrayList<>();
            Word maxWord = new Word(0, 0, true, word.toString());
            check.add(maxWord);
            int score = calculateScore(move, boardExtended, this.board);
            if (score > highestScoringPoints && dictionary.areWords(check)) {
                highestScoringPoints = score;
                highestScoringMove = move;
                highestWord = word.toString();
            }
        }
        searchTime = System.currentTimeMillis() - searchTime;
        System.out.println("\n" + moves.size() + " moves found (including duplicates).");
        System.out.println("Highest scoring move: ");
        System.out.println("Start row: " + highestScoringMove.places.get(0).getRow() + " Start column: " + highestScoringMove.places.get(0).getColumn());
        System.out.println("Next row: " + highestScoringMove.places.get(1).getRow() + " Next column: " + highestScoringMove.places.get(1).getColumn());
        System.out.print(highestWord + "\t" + highestScoringPoints + " points\n");
        System.out.println("Time taken: " + searchTime + "ms.");
        System.out.println("Command: " + highestScoringMove.toString());
        return highestScoringMove;
    }

    private int calculateScore(Move move, BoardExtended boardExtended, BoardAPI board) {
        int total = 0;
        int wordMultiplier = 1;
        int wordScore = 0;

        for (Place place : move) {
            Tile placement = new Tile(place.letter);
            if (boardExtended.isAnchor(place.getRow(), place.getColumn())) {
                int connectingWordsScore = 0;
                boolean connectingWords = false;
                int left = Math.max(place.getRow() - 1, 0);
                int right = Math.min(place.getRow() + 1, Board.BOARD_SIZE - 1);
                int up = Math.max(place.getColumn() - 1, 0);
                int down = Math.min(place.getColumn() + 1, Board.BOARD_SIZE - 1);

                while (board.getSquareCopy(boardBoundaries(left, true), boardBoundaries(place.getColumn(), false)).isOccupied()) {
                    connectingWordsScore += board.getSquareCopy(boardBoundaries(left, true), boardBoundaries(place.getColumn(), false)).getTile().getValue();
                    left--;
                    connectingWords = true;
                    if (boardBoundaries(left, false) == 14) {
                        break;
                    }
                }
                while (board.getSquareCopy(boardBoundaries(right, false), boardBoundaries(place.getColumn(), false)).isOccupied()) {
                    connectingWordsScore += board.getSquareCopy(boardBoundaries(right, false), boardBoundaries(place.getColumn(), false)).getTile().getValue();
                    right++;
                    connectingWords = true;
                    if (boardBoundaries(right, false) == 14) {
                        break;
                    }
                }
                while (board.getSquareCopy(boardBoundaries(place.getRow(), false), boardBoundaries(up, true)).isOccupied()) {
                    connectingWordsScore += board.getSquareCopy(boardBoundaries(place.getRow(), false), boardBoundaries(up, true)).getTile().getValue();
                    up++;
                    connectingWords = true;
                    if (boardBoundaries(up, false) == 14) {
                        break;
                    }
                }
                while (board.getSquareCopy(boardBoundaries(place.getRow(), false), boardBoundaries(down, false)).isOccupied()) {
                    connectingWordsScore += board.getSquareCopy(boardBoundaries(place.getRow(), false), boardBoundaries(down, false)).getTile().getValue();
                    down++;
                    connectingWords = true;
                    if (boardBoundaries(down, false) == 14) {
                        break;
                    }
                }
                connectingWordsScore += placement.getValue() * board.getSquareCopy(boardBoundaries(place.getRow(), false), boardBoundaries(place.getColumn(), false)).getLetterMuliplier();

                if (connectingWords) {
                    connectingWordsScore = connectingWordsScore * board.getSquareCopy(boardBoundaries(place.getRow(), false), boardBoundaries(place.getColumn(), false)).getWordMultiplier();
                    total += connectingWordsScore;
                }

                wordMultiplier *= board.getSquareCopy(boardBoundaries(place.getRow(), false), boardBoundaries(place.getColumn(), false)).getWordMultiplier();
            } else {
                wordMultiplier = wordMultiplier * board.getSquareCopy(boardBoundaries(place.getRow(), false), boardBoundaries(place.getColumn(), false)).getWordMultiplier();
            }
            wordScore += placement.getValue() * board.getSquareCopy(boardBoundaries(place.getRow(), false), boardBoundaries(place.getColumn(), false)).getLetterMuliplier();
        }
        if (move.usesAllTiles) {
            total += 50;
        }
        return total + wordScore * wordMultiplier;
    }

    private void generateMoves(List<Move> moves) {
        ArrayList<Word> wordsToCheck = new ArrayList<Word>();

        for (int i = 0; i < moves.size(); i++) {
            Move convert = moves.get(i);
            int row = convert.places.get(0).getRow();
            int column = convert.places.get(0).getColumn();
            char rowLetter = (char) (row);
            boolean isHorizontal;
            if (convert.places.get(0).row == convert.places.get(1).row) {
                isHorizontal = true;
            } else
                isHorizontal = false;

            StringBuilder sb = new StringBuilder();
            sb.append(moves.get(i));
            String letters = sb.toString();
            letters = letters.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\, ", "");

            Word insert = new Word(rowLetter, column, isHorizontal, letters);
            wordsToCheck.add(insert);
        }

        ArrayList<Word> wordsToCompare = new ArrayList<Word>();
        for (int k = 0; k < wordsToCheck.size(); k++) {
            if (!wordsToCompare.toString().contains(wordsToCheck.get(k).toString()))
                wordsToCompare.add(wordsToCheck.get(k));
        }

        System.out.println("duplicates?" + wordsToCheck.size());
        System.out.println("no duplicates?" + wordsToCompare.size());

        for (int j = 0; j < wordsToCompare.size(); j++) {

            ArrayList<Word> subWordsToCheck = new ArrayList<Word>();
            subWordsToCheck.add(wordsToCompare.get(j));
            if (dictionary.areWords(subWordsToCheck) == false) {
                wordsToCompare.removeAll(subWordsToCheck);

            }
            subWordsToCheck.clear();
        }

        System.out.println("Filtered words: " + wordsToCompare.toString());
        System.out.println("Filtered words: " + wordsToCompare.size());

    }

    private List<Move> validMoves(List<Move> possibleMoves) {
        List<Move> output = new ArrayList<>();
        Frame frame = new Frame();

        // constructing frame from string
        ArrayList<Tile> tiles = new ArrayList<>();
        String frameStringClean = me.getFrameAsString();
        frameStringClean = frameStringClean.substring(1, frameStringClean.length() - 1);
        frameStringClean = frameStringClean.replaceAll(", ", "");
        for(char c : frameStringClean.toCharArray()) {
            tiles.add(new Tile(c));
        }
        frame.addTiles(tiles);

        // constructing word from move
        for(Move m : possibleMoves) {
            StringBuilder wordString = new StringBuilder();
            for(Place p: m) {
                wordString.append(p.letter);
            }
            Word word = new Word(m.places.get(0).getRow(), m.places.get(0).getColumn(), (m.places.get(0).getRow() == m.places.get(1).getRow()), wordString.toString());
            ArrayList<Word> wordArrayList = new ArrayList<>();
            if(dictionary.areWords(wordArrayList) && board.isLegalPlay(frame, word) ) {
                if(validateWordPlacement(m))
            	output.add(m);
            }
        }
        return output;
    }
    ArrayList<Word> connectedWords = new ArrayList<Word>();
    public boolean validateWordPlacement(Move move) {
    	
    	connectedWords = new ArrayList<Word>(); 
    	
        for(Place place:move) {
            if(place.getColumn() < 0 || place.getColumn() >= 15) {
                return false;
            }
            if(place.getRow() < 0 || place.getRow() >= 15) {
                return false;
            }
        }
        /*Frame frame = new Frame();
        // constructing frame from string
        ArrayList<Tile> tiles = new ArrayList<>();
        String frameStringClean = me.getFrameAsString();
        frameStringClean = frameStringClean.substring(1, frameStringClean.length() - 1);
        frameStringClean = frameStringClean.replaceAll(", ", "");
        for(char c : frameStringClean.toCharArray()) {
            tiles.add(new Tile(c));
        }
        frame.addTiles(tiles);
        //Constructing Word
        StringBuilder wordString = new StringBuilder();
        for(Place p: move) {
            wordString.append(p.letter);
        }
        Word legalWord = new Word(move.places.get(0).getRow(), move.places.get(0).getColumn(), (move.places.get(0).getRow() == move.places.get(1).getRow()), wordString.toString());
        if(!board.isLegalPlay(frame, legalWord)) {
            return false;
        }*/
        if (move.places.get(0).getRow() == move.places.get(1).getRow()) {
            StringBuilder across = new StringBuilder();
            for (Place place : move) {
                across.append(place.letter);
            }
            if (move.places.get(0).getColumn() - 1 >= 0) {
                for (int i = 0; move.places.get(0).getColumn() - i > 0; i++) {
                    if (board.getSquareCopy(move.places.get(0).getRow(), move.places.get(0).getColumn() - i).isOccupied()) {
                        across.insert(0,board.getSquareCopy(move.places.get(0).getRow(), move.places.get(0).getColumn() - i).getTile().getLetter());
                    } else {
                        break;
                    }
                }
            }
            
            
            if (move.places.get(move.places.size() - 1).getColumn() + 1 < 15) {
                for (int i = 0; move.places.get(move.places.size() - 1).getColumn() + i < 15; i++) {
                    if (board.getSquareCopy(move.places.get(move.places.size()-1).getRow(), move.places.get(move.places.size()-1).getColumn() + i).isOccupied()) {
                        across.append(
                                board.getSquareCopy(move.places.get(move.places.size()-1).getRow(), move.places.get(move.places.size()-1).getColumn() + i).getTile().getLetter());
                    } else {
                        break;
                    }
                }
            }
            if(across.length() > 0)
                connectedWords.add(new Word(0, 0, true, across.toString()));
            
            for (int j = 0;j<move.places.size();j++) {
            	
            	Place place = move.places.get(j); 
                //Horizontal Word
                StringBuilder word = new StringBuilder(place.toString());
                if( place.getRow() + 1 < 15) {
                    if(board.getSquareCopy(place.getRow() + 1, place.getColumn()).isOccupied()) {
                        for(int i = 1;place.getRow() + i < 15 ;i++) {
                            if(board.getSquareCopy(place.getRow() + i, place.getColumn()).isOccupied()) {
                                word.append(board.getSquareCopy(place.getRow() + i, place.getColumn()).getTile().getLetter());
                            } else {
                                break;
                            }

                        }


                    }

                }
                if( place.getRow() - 1 >= 0) {
                    if(board.getSquareCopy(place.getRow() - 1, place.getColumn()).isOccupied()) {
                        for(int i = 1;place.getRow() - i >= 0 ;i++) {
                            if(board.getSquareCopy(place.getRow() - i, place.getColumn()).isOccupied()) {
                                word.insert(0,board.getSquareCopy(place.getRow() - i, place.getColumn()).getTile().getLetter());
                            } else {
                                break;
                            }
                        }
                    }
                }
                if(word.toString().length() > 1)
                    connectedWords.add(new Word(0, 0, true, word.toString()));
            }
        } else  {
            StringBuilder down = new StringBuilder();
           
            
            for (Place place : move) {
                down.append(place.letter);
            
            }
            if (move.places.get(0).getRow() - 1 >= 0) {
                for (int i = 0; move.places.get(0).getRow() - i > 0; i++) {
                    if (board.getSquareCopy(move.places.get(0).getRow() - i, move.places.get(0).getColumn()).isOccupied()) {
                        down.insert(0,board.getSquareCopy(move.places.get(0).getRow()-i, move.places.get(0).getColumn()).getTile().getLetter());
                    } else {
                        break;
                    }
                }
            }
            if (move.places.get(move.places.size() - 1).getRow() + 1 < 15) {
                for (int i = 0; move.places.get(move.places.size() - 1).getRow() + i < 15; i++) {
                    if (board.getSquareCopy(move.places.get(move.places.size()-1).getRow() + i, move.places.get(move.places.size()-1).getColumn()).isOccupied()) {
                        down.append(
                                board.getSquareCopy(move.places.get(move.places.size()-1).getRow() + i, move.places.get(move.places.size()-1).getColumn()).getTile().getLetter());
                    } else {
                        break;
                    }
                }
            }
            if(down.length() > 0)
                connectedWords.add(new Word(0, 0, true, down.toString()));
            
            
            for (int j = 0;j<move.places.size();j++) {
            	
            	Place place = move.places.get(j); 
                //Horizontal Word
                StringBuilder word = new StringBuilder(place.toString());
                if( place.getColumn() + 1 < 15) {
                    if(board.getSquareCopy(place.getRow(), place.getColumn() + 1).isOccupied()) {
                        for(int i = 1;place.getColumn() + i < 15 ;i++) {
                            if(board.getSquareCopy(place.getRow(), place.getColumn() + i).isOccupied()) {
                                word.append(board.getSquareCopy(place.getRow(), place.getColumn() + i).getTile().getLetter());
                            } else {
                                break;
                            }
                        }
                    }
                }
                if( place.getColumn() - 1 >= 0) {
                    if(board.getSquareCopy(place.getRow(), place.getColumn() - 1).isOccupied()) {
                        for(int i = 1;place.getColumn() - i >= 0 ;i++) {
                            if(board.getSquareCopy(place.getRow(), place.getColumn() - i).isOccupied()) {
                                word.insert(0,board.getSquareCopy(place.getRow(), place.getColumn() - i).getTile().getLetter());
                            } else {
                                break;
                            }
                        }
                    }
                }
                if(word.toString().length() > 1)
                    connectedWords.add(new Word(0, 0, true, word.toString()));
            }
        }
        
       
        
        return dictionary.areWords(connectedWords);
    }

    public Move getBestMove(String frame, BoardExtended boardExtended) {
        ArrayList<Character> frameArray = parseFrame(frame);
        boardExtended.getCrossSets(boardExtended, gaddag.getRoot(), this.board);
        boardExtended.getAnchors();
        List<Move> moves = validMoves(this.gaddag.findMoves(gaddag.getRoot(), frameArray, boardExtended, this.board));
        Move bestMove = null;
        int bestMoveScore = 0;
        List<Place> bestMovePlaces;
        int[][] VCMix = {
                {0, 0, -1, -2, -3, -4, -5},
                {-1, 1, 1, 0, -1, -2},
                {-2, 0, 2, 2, 1},
                {-3, -1, 1, 3},
                {-4, -2, 0},
                {-5, -3},
                {-6}
        };
        for (Move move : moves) {
            int tempScore = calculateScore(move, boardExtended, board);
            List<Place> tempPlaces = move.places;
            if (frame.contains("Q") || frame.contains("U") || frame.contains("X") || frame.contains("Z")) {
                boolean isQ = false;
                boolean isU = false;
                boolean isX = false;
                boolean isZ = false;
                 for (int i = 0; i < tempPlaces.size(); i++) {

                    if (tempPlaces.get(i).toString() == "Q") {
                        isQ = true;
                    }

                    if (tempPlaces.get(i).toString() == "U") {
                        isU = true;
                    }
                }
                if (isQ || isU || isZ || isX)
                    tempScore += 10;
            }
            
            
            if(frame.contains("S")) {
            	tempScore -= 10;
            }
            
            
            frameArray = parseFrame(frame);
            for (int i = 0; i < tempPlaces.size(); i++) {
                for (int j = 0; j < frameArray.size(); j++) {

                    if (tempPlaces.get(i).toString().toCharArray()[0] == frameArray.get(j)) {

                        frameArray.remove(j);
                    }
                }
            }
            int vowels = 0;
            if (frameArray.contains('A')) {
                vowels++;
            }
            if (frameArray.contains('I')) {
                vowels++;
            }
            if (frameArray.contains('O')) {
                vowels++;
            }
            if (frameArray.contains('U')) {
                vowels++;
            }
            if (frameArray.contains('E')) {
                vowels++;
            }

            tempScore += VCMix[vowels][frameArray.size() - vowels];

            StringBuilder word = new StringBuilder();
            for (Place place : move) {
                word.append(place.letter);
            }

            ArrayList<Word> check = new ArrayList<>();
            Word maxWord = new Word(0, 0, true, word.toString());
            check.add(maxWord);

            if (tempScore > bestMoveScore && dictionary.areWords(check) &&validateWordPlacement(move)) {
                bestMoveScore = tempScore;
                bestMove = move;
//                System.out.println("-----------------------------------------------------------");
//                System.out.println(bestMoveScore);
//                System.out.println(frameArray.toString());
//                System.out.println(word.toString());
//                System.out.println(bestMove.toString());

            }


        }

        validateWordPlacement(bestMove);
        System.out.println(connectedWords.toString());
        return bestMove;

    }
    
    private String getLastMove() {
    	String[] lastPlayArr = ui.getAllInfo().substring(ui.getAllInfo().lastIndexOf('>') + 2, ui.getAllInfo().length()).split("\n");
    	String lastPlay = lastPlayArr[0];
    	return lastPlay;
    }

    public String getCommand() {
        // Add your code here to input your commands
        // Your code must give the command NAME <botname> at the start of the game

        String command = "";
        switch (turnCount) {
            case 0:
                command = "NAME gooses";
                break;
            case 5:
                command = "CHALLENGE";
                break;
            default:
                BoardExtended boardExtended = new BoardExtended(this.board);
                try {
                    command = getBestMove(me.getFrameAsString(), boardExtended).toString();
                } catch (NullPointerException ex) {
                    command = "PASS";
                }
                break;
        }
        turnCount++;
        return command;
    }
}
