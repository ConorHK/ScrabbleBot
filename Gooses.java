import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

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
	private GADDAG gaddag;
	private Square[][] squares;
	private LinkedList<Integer> anchorColumns;
	private LinkedList<Integer> anchorRows;

	private static final int[][] LETTER_MULTIPLIER = { { 1, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1 },
			{ 1, 1, 1, 1, 1, 3, 1, 1, 1, 3, 1, 1, 1, 1, 1 }, { 1, 1, 1, 1, 1, 1, 2, 1, 2, 1, 1, 1, 1, 1, 1 },
			{ 2, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2 }, { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
			{ 1, 3, 1, 1, 1, 3, 1, 1, 1, 3, 1, 1, 1, 3, 1 }, { 1, 1, 2, 1, 1, 1, 2, 1, 2, 1, 1, 1, 2, 1, 1 },
			{ 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1 }, { 1, 1, 2, 1, 1, 1, 2, 1, 2, 1, 1, 1, 2, 1, 1 },
			{ 1, 3, 1, 1, 1, 3, 1, 1, 1, 3, 1, 1, 1, 3, 1 }, { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
			{ 2, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2 }, { 1, 1, 1, 1, 1, 1, 2, 1, 2, 1, 1, 1, 1, 1, 1 },
			{ 1, 1, 1, 1, 1, 3, 1, 1, 1, 3, 1, 1, 1, 1, 1 }, { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 } };
	private static final int[][] WORD_MULTIPLIER = { { 3, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 3 },
			{ 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1 }, { 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1 },
			{ 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1 }, { 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1 },
			{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
			{ 3, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 3 }, { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
			{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, { 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1 },
			{ 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1 }, { 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1 },
			{ 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1 }, { 3, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 3 } };

	Gooses(PlayerAPI me, OpponentAPI opponent, BoardAPI board, UserInterfaceAPI ui, DictionaryAPI dictionary)
			throws FileNotFoundException {
		this.me = me;
		this.opponent = opponent;
		this.board = board;
		this.info = ui;
		this.dictionary = dictionary;
		turnCount = 0;
		this.gaddag = buildGADDAG();
	}

	// nested node class for GADDAG data structure
	private static class Node implements Comparable<Node> {
		private boolean leaf;
		private char value;
		public TreeMap<Character, Node> tree = new TreeMap<>();

		public Node(char value) {
			this.value = value;
			this.leaf = false;
		}

		// getters and setters
		public char getValue() {
			return this.value;
		}

		public void setLeaf(boolean leaf) {
			this.leaf = leaf;
		}

		public boolean isLeaf() {
			return leaf;
		}

		public Collection<Character> getKeySet() {
			return tree.keySet();
		}

		public Collection<Node> getChildrenValues() {
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
			if (obj instanceof Node) {
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

	// Nested Class for Gaddag Implementation
	private static class GADDAG {

		private static char separator = '$';

		private Node root;
		private int depth;

		public GADDAG() {
			root = new Node('*');
			this.depth = 0;
		}

		private void add(String word) {
			if (word.length() == 0) {
				return;
			}
			System.out.println("ADDING: " + word);
			Node traverse = this.root;
			for (int pos = 0; pos < word.length(); pos++) {
				if (!traverse.hasChild(word.charAt(pos))) {
					traverse.addChild(word.charAt(pos));
				}
				traverse = traverse.getChild(word.charAt(pos)); // move down tree
			}
			traverse.setLeaf(true);
			if (word.length() > this.depth) {
				this.depth = word.length();
			}
		}

		private boolean contains(String word) {
			Node traverse = this.root;
			for (int pos = 0; pos < word.length(); pos++) {
				if (traverse.hasChild(word.charAt(pos))) {
					traverse = traverse.getChild(word.charAt(pos));
				} else {
					return false;
				}
			}
			return traverse.isLeaf();
		}

		private Node find(String prefix) {
			Node traverse = this.root;
			for (int pos = 0; pos < prefix.length(); pos++) {
				if (traverse.hasChild(prefix.charAt(pos))) {
					traverse = traverse.getChild(prefix.charAt(pos));
				} else {
					return null;
				}
			}
			return traverse;
		}

		private ArrayList<String> getValidWords() {
			ArrayList<String> words = new ArrayList<>();
			collectWords("", root, words);
			return words;
		}

		private void collectWords(String word, Node traverse, ArrayList<String> words) {
			if (traverse.isLeaf()) {
				words.add(word);
			} else {
				if (traverse.getChildrenValues() != null) {
					for (Node node : traverse.getChildrenValues()) {
						collectWords(word + node.getValue(), node, words);
					}
				}
			}
		}

		// GADDAG METHODS
		public void GAdd(String word) {
			if (word.length() == 0) {
				return;
			}
			String prefix;
			word = word.toUpperCase();
			for (int i = 1; i < word.length(); i++) {
				prefix = word.substring(0, i);
				StringBuilder reversed = new StringBuilder();
				reversed.append(prefix);
				reversed.reverse();
				this.add(reversed.toString() + separator + word.substring(i));
			}
			StringBuilder wordReversed = new StringBuilder();
			wordReversed.append(word);
			this.add(wordReversed.reverse().toString() + separator + word.substring(word.length() - 1));
		}

		public HashSet<String> findWords(String frame, char anchor) {
			HashSet<String> words = new HashSet<>();
			frame = frame.replaceAll("\\W", "");
			ArrayList<Character> frameLetters = new ArrayList<>();
			for (char c : frame.toCharArray()) {
				frameLetters.add(c);
			}
			if (anchor == ' ') {
				while (frameLetters.size() > 0) {
					findWordsRecursively(words, "", frameLetters, frameLetters.remove(0), this.root, true);
				}
			} else {
				findWordsRecursively(words, "", frameLetters, anchor, root, true);
			}
			return words;
		}

		private void findWordsRecursively(HashSet<String> words, String word, ArrayList<Character> frameLetters,
				char anchor, Node traverse, boolean direction) {
			Node anchorNode = traverse.getChild(anchor);
			if (anchorNode == null) {
				return;
			}
			String letter;
			if (anchor == separator) {
				letter = "";
			} else {
				letter = String.valueOf(anchor);
			}

			if (direction) {
				word = letter + word;
			} else {
				word = word + letter;
			}

			if (anchorNode.isLeaf()) {
				words.add(word);
			}

			for (char nodeKey : anchorNode.getKeySet()) {
				if (nodeKey == separator) {
					findWordsRecursively(words, word, frameLetters, separator, anchorNode, false);
				} else if (frameLetters.contains(nodeKey)) {
					ArrayList<Character> updatedFrameLetters = (ArrayList<Character>) frameLetters.clone();
					updatedFrameLetters.remove((Character) nodeKey);
					findWordsRecursively(words, word, updatedFrameLetters, nodeKey, anchorNode, direction);
				}
			}
		}
	}
	/* END OF NESTED GADDAG CLASS */

	private GADDAG buildGADDAG() throws FileNotFoundException {
		GADDAG output = new GADDAG();
		Scanner initialize = new Scanner(new File("csw.txt"));
		while (initialize.hasNext()) {
			output.GAdd(initialize.nextLine());
		}
		return output;
	}

	public void testGADDAG(String frame) {
		long startTime = System.nanoTime();
		// frame = frame + "_";
		// frame = "";
		ArrayList<String> validWords = new ArrayList<String>(gaddag.findWords(frame, ' '));
		if (frame.contains("_")) {
			for (char letter = 'A'; letter <= 'Z'; letter++) {
				String duplicateFrame = frame;
				System.out.println("LETTER " + letter);
				duplicateFrame = duplicateFrame.replaceAll("[_]", letter + "");
				System.out.println("DUPLICATE: " + duplicateFrame);
				String[] possible = gaddag.findWords(duplicateFrame, ' ').toString().replaceAll("[,]", "").split(" ");
				for (String string : possible) {
					validWords.add(string);
				}
			}
		}
		long elapsedTime = System.nanoTime() - startTime;
		ArrayList<String> validUniqueWords = removeDuplicates(validWords);
		System.out.println(validUniqueWords.toString());
		System.out.println("Time: " + elapsedTime);
	}

	private ArrayList<String> removeDuplicates(ArrayList<String> input) {
		ArrayList<String> output = new ArrayList<>(input.stream().distinct().collect(Collectors.toList()));
		return output;
	}

//	private int getWordPoints(Word word) { 
//		ArrayList<Coordinates> newLetterCoords = board.getSquareCopy(row, col);
//		int wordValue = 0;
//		int wordMultipler = 1;
//		int r = word.getFirstRow();
//		int c = word.getFirstColumn();
//		for (int i = 0; i<word.length(); i++) {
//			int letterValue = board.getSquareCopy(r, c).getTile().getValue();
//			if (newLetterCoords.contains(new Coordinates(r,c))) {
//				wordValue = wordValue + letterValue * board.getSquareCopy(r, c).getLetterMuliplier();
//				wordMultipler = wordMultipler * board.getSquareCopy(r, c).getWordMultiplier();
//			} else {
//				wordValue = wordValue + letterValue;
//			}
//			if (word.isHorizontal()) {
//				c++;
//			} else {
//				r++;
//			}
//		}
//		return wordValue * wordMultipler;
//	}

	private void boardClone() {
		squares = new Square[15][15];
		for (int r = 0; r < 15; r++) {
			for (int c = 0; c < 15; c++) {
				squares[r][c] = new Square(LETTER_MULTIPLIER[r][c], WORD_MULTIPLIER[r][c]);
			}
		}
	}

	public String getCommand() {
		// Add your code here to input your commands
		// Your code must give the command NAME <botname> at the start of the game

		String command = "";
		switch (turnCount) {
		case 0:
			command = "NAME gooses";
			break;
		default:
			testGADDAG(me.getFrameAsString());
			System.out.println(me.getFrameAsString());
			System.exit(1);
			// TODO make move
		}
		turnCount++;
		return command;
	}

	public void getAnchors() {

		anchorColumns = new LinkedList<Integer>();
		anchorRows = new LinkedList<Integer>();

		//searching board for tiles and then searching surrounding spaces for empty spaces which are known as anchor points 
		for (int row = 0; row < squares.length; row++) {
			for (int column = 0; column < squares.length; column++) {
				if (squares[row][column] != null) {

					if (squares[row + 1][column] == null) {
						anchorColumns.add(column);
						anchorRows.add(row + 1);
					}
					if (squares[row - 1][column] == null) {
						anchorColumns.add(column);
						anchorRows.add(row - 1);
					}
					if (squares[row][column + 1] == null) {
						anchorColumns.add(column + 1);
						anchorRows.add(row);
					}
					if (squares[row][column - 1] == null) {
						anchorColumns.add(column - 1);
						anchorRows.add(row);
					}

				}
			}
		}
	}

}
