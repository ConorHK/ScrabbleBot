import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Gooses implements BotAPI {

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
    private boolean isPoolUpdated;
    private HashMap<Character, Integer> tilePriority;


    Gooses(PlayerAPI me, OpponentAPI opponent, BoardAPI board, UserInterfaceAPI ui, DictionaryAPI dictionary)
            throws FileNotFoundException {
        this.me = me;
        this.ui = ui;
        this.board = board;
        this.dictionary = dictionary;
        turnCount = 0;
        this.gaddag = new GADDAG();
        this.tilePriority = initializeTilePriority();
    }

    private HashMap<Character, Integer> initializeTilePriority() {
        HashMap<Character, Integer> output = new HashMap<>();
        output.put('S', 1);
        output.put('Z', 2);
        output.put('R', 3);
        output.put('N', 4);
        output.put('C', 5);
        output.put('L', 6);
        output.put('T', 7);
        output.put('E', 8);
        output.put('D', 9);
        output.put('M', 10);
        output.put('H', 11);
        output.put('X', 12);
        output.put('K', 13);
        output.put('A', 14);
        output.put('P', 15);
        output.put('Y', 16);
        output.put('I', 17);
        output.put('G', 18);
        output.put('B', 19);
        output.put('J', 20);
        output.put('O', 21);
        output.put('F', 22);
        output.put('W', 23);
        output.put('U', 24);
        output.put('V', 25);
        output.put('Q', 26);
        output.put('_', 27);
        return output;
    }

    static int boardBoundaries(int position, boolean top) {
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
            for (Place p : places) {
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

    /* Connecting word Validation: */
    public ArrayList<Word> getAllWords(Word mainWord) {
        ArrayList<Word> words = new ArrayList<>();
        words.add(mainWord);
        int r = mainWord.getFirstRow();
        int c = mainWord.getFirstColumn();
        for (int i = 0; i < mainWord.length(); i++) {
            if (isAdditionalWord(r, c, mainWord.isHorizontal())) {
                Square[][] possibleBoard = duplicateBoard(mainWord);
                words.add(getAdditionalWord(r, c, mainWord.isHorizontal(), possibleBoard));
            }
            if (mainWord.isHorizontal()) {
                c++;
            } else {
                r++;
            }
        }
        return words;
    }

    private boolean isAdditionalWord(int r, int c, boolean isHorizontal) {
        if ((isHorizontal && ((r > 0 && board.getSquareCopy(r - 1, c).isOccupied()) || (r < Board.BOARD_SIZE - 1 && board.getSquareCopy(r + 1, c).isOccupied()))) || (!isHorizontal && ((c > 0 && board.getSquareCopy(r, c - 1).isOccupied()) || (c < Board.BOARD_SIZE - 1 && board.getSquareCopy(r, c + 1).isOccupied())))) {
            return true;
        }
        return false;
    }

    private Word getAdditionalWord(int mainWordRow, int mainWordCol, boolean mainWordIsHorizontal, Square[][] possibleBoard) {
        int firstRow = mainWordRow;
        int firstCol = mainWordCol;
        // search up or left for the first letter
        while (firstRow >= 0 && firstCol >= 0 && possibleBoard[firstRow][firstCol].isOccupied()) {
            if (mainWordIsHorizontal) {
                firstRow--;
            } else {
                firstCol--;
            }
        }
        // went too far
        if (mainWordIsHorizontal) {
            firstRow++;
        } else {
            firstCol++;
        }
        // collect the letters by moving down or right
        StringBuilder letters = new StringBuilder();
        int r = firstRow;
        int c = firstCol;
        while (r < Board.BOARD_SIZE && c < Board.BOARD_SIZE && possibleBoard[r][c].isOccupied()) {
            letters.append(possibleBoard[r][c].getTile().getLetter());
            if (mainWordIsHorizontal) {
                r++;
            } else {
                c++;
            }
        }
        return new Word(firstRow, firstCol, !mainWordIsHorizontal, letters.toString());
    }


    private Square[][] duplicateBoard(Word word) {
        Square[][] output = new Square[15][15];
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                output[i][j] = board.getSquareCopy(i, j);
            }
        }
        int r = word.getFirstRow();
        int c = word.getFirstColumn();
        for (int i = 0; i < word.length(); i++) {
            if (!output[r][c].isOccupied()) {
                char letter = word.getLetter(i);
                Tile tile = new Tile(letter);
                output[r][c].add(tile);
            }
            if (word.isHorizontal()) {
                c++;
            } else {
                r++;
            }
        }
        return output;
    }

    /* --------------------------  */
    /* POINTS */
    private int getWordPoints(Word word) {
        int wordValue = 0;
        int wordMultipler = 1;
        int r = word.getFirstRow();
        int c = word.getFirstColumn();
        for (int i = 0; i < word.length(); i++) {
            int letterValue = (new Tile(word.getLetter(i)).getValue());
            wordValue = wordValue + letterValue * board.getSquareCopy(r, c).getLetterMuliplier();
            wordMultipler = wordMultipler * board.getSquareCopy(r, c).getWordMultiplier();
        }
        if (word.isHorizontal()) {
            c++;
        } else {
            r++;
        }
        return wordValue * wordMultipler;
    }

    public int getAllPoints(ArrayList<Word> words) {
        int points = 0;
        for (Word word : words) {
            points = points + getWordPoints(word);
        }
        if (words.get(0).length() == Frame.MAX_TILES) {
            points = points + 50;
        }
        return points;
    }

    /* ------ */
    private ArrayList<Character> parseFrame(String frame) {
        frame = frame.substring(1, frame.length() - 1);
        frame = frame.replaceAll(", ", "");
        ArrayList<Character> output = new ArrayList<>();
        for (char c : frame.toCharArray()) {
            output.add(c);
        }
        return output;
    }

    private Word makeWordFromMove(Move move) {
        StringBuilder letters = new StringBuilder();
        for (Place p : move) {
            letters.append(p.letter);
        }
        return new Word(move.places.get(0).getRow(), move.places.get(0).getColumn(), (move.places.get(0).getRow() == move.places.get(1).getRow()), letters.toString());
    }


    private List<Move> validMoves(List<Move> possibleMoves) {
        List<Move> output = new ArrayList<>();
        Frame frame = new Frame();

        // constructing frame from string
        ArrayList<Tile> tiles = new ArrayList<>();
        String frameStringClean = me.getFrameAsString();
        frameStringClean = frameStringClean.substring(1, frameStringClean.length() - 1);
        frameStringClean = frameStringClean.replaceAll(", ", "");
        for (char c : frameStringClean.toCharArray()) {
            tiles.add(new Tile(c));
        }
        frame.addTiles(tiles);

        // constructing word from move
        for (Move m : possibleMoves) {
            ArrayList<Word> checkWord = new ArrayList<>();
            checkWord.add(makeWordFromMove(m));
            if (board.isLegalPlay(frame, makeWordFromMove(m)) && dictionary.areWords(checkWord)) {
                output.add(m);
            }
        }
        return output;
    }

    public Move getBestMove(String frame, BoardExtended boardExtended) {
        ArrayList<Character> frameArray = parseFrame(frame);
        boardExtended.getCrossSets(boardExtended, gaddag.getRoot(), this.board);
        boardExtended.getAnchors();
        List<Move> moves = validMoves(this.gaddag.findMoves(gaddag.getRoot(), frameArray, boardExtended, this.board));
        Move bestMove = null;
        int bestMoveScore = 0;
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
            ArrayList<Word> allWords = getAllWords(makeWordFromMove(move));
            if (dictionary.areWords(allWords)) {
                int tempScore = getAllPoints(allWords);
                List<Place> tempPlaces = move.places;
                if (frame.contains("Q") || frame.contains("U")) {
                    boolean isQ = false;
                    boolean isU = false;
                    for (Place tempPlace : tempPlaces) {

                        if (tempPlace.toString().equals("Q")) {
                            isQ = true;
                        }

                        if (tempPlace.toString().equals("U")) {
                            isU = true;
                        }
                    }
                    if (isQ && isU)
                        tempScore += 10;
                }
                frameArray = parseFrame(frame);
                for (Place tempPlace : tempPlaces) {
                    for (int j = 0; j < frameArray.size(); j++) {

                        if (tempPlace.toString().toCharArray()[0] == frameArray.get(j)) {

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
                if (tempScore > bestMoveScore) {
                    bestMoveScore = tempScore;
                    bestMove = move;
                }
            }
        }
        if(bestMoveScore < 4) {
            return null;
        }
        return bestMove;
    }


    private String getLastMove() {
        String[] lastPlayArr = ui.getAllInfo().substring(ui.getAllInfo().lastIndexOf('>') + 2).split("\n");
        return lastPlayArr[0];
    }

    private int getPool() {
        String[] lastPlayArr = ui.getAllInfo().substring(ui.getAllInfo().lastIndexOf('>') + 2).split("\n");
        String lastPlay = lastPlayArr[1];

        return Integer.parseInt(lastPlay.split(" ")[2]);
    }

    public String getExchangeCommand() {
        ArrayList<Character> frameArray = parseFrame(me.getFrameAsString());
        StringBuilder command = new StringBuilder();
        System.out.println(me.getFrameAsString());

        // PREFIX /SUFFIX STUFF


        int threshold = 27;
        while(command.toString().isEmpty()) {
            for (int i = 0; i < frameArray.size(); i++) {
                if (tilePriority.get(frameArray.get(i)) < threshold) {
                    command.append(frameArray.remove(i));
                }
            }
            threshold--;
        }

        isPoolUpdated = false;
        return command.toString();

    }


    private Boolean doubleDigits(String toMatch) {
        Pattern pat = Pattern.compile("^[A-O]\\d{1,2}");
        Matcher match = pat.matcher(toMatch);
        return match.find() && match.group().length() == 3;
    }

    private boolean challenge() {
        String check = getLastMove();
        if (!check.matches("[A-O](\\d){1,2}( )+[A,D]( )+([A-Z]){1,15}")) {
            return false;
        }
        boolean doubleDigits = doubleDigits(check);
        String word;
        int column = check.charAt(0) - 'A';
        int row;
        String rowString;
        char direction;
        boolean isHorizontal;
        if (doubleDigits) {
            rowString = check.substring(1, 3);
            row = Integer.parseInt(rowString) - 1;
            direction = check.charAt(4);
            word = check.substring(6);
        } else {
            rowString = check.substring(1, 2);
            row = Integer.parseInt(rowString) - 1;
            direction = check.charAt(3);
            word = check.substring(5);
        }

        isHorizontal = direction == 'A';
        Word words = new Word(row, column, isHorizontal, word);
        ArrayList<Word> toCheck = getAllWords(words);
        return !dictionary.areWords(toCheck);
    }

    public String getCommand() {
        String command;
        if (turnCount == 0) {
            isPoolUpdated = false;
            command = "NAME gooses";
        } else {
            BoardExtended boardExtended = new BoardExtended(this.board);
            if (challenge()) {
                command = "CHALLENGE";
            } else {
                try {
                    command = getBestMove(me.getFrameAsString(), boardExtended).toString();
                } catch (NullPointerException ex) {
                    if (isPoolUpdated) {

                        String tilesToReplace = getExchangeCommand();
                        if (getPool() >= tilesToReplace.length()) {
                            command = "EXCHANGE " + tilesToReplace;
                        } else {
                            command = "PASS";
                        }
                    } else {
                        command = "POOL";
                        isPoolUpdated = true;
                    }
                }
            }
        }
        turnCount++;
        return command;
    }
}

