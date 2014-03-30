import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

public class PlayerSkeleton {
	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		State s = new State();

		int numProcessors = Runtime.getRuntime().availableProcessors();
		LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		ExecutorService executorService = new ThreadPoolExecutor(numProcessors, numProcessors, 0, TimeUnit.MILLISECONDS, queue);
		PlayerSkeleton p = new PlayerSkeleton(executorService);

		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");

		executorService.shutdown();
	}

	public PlayerSkeleton(ExecutorService executorService) {
		this.executorService = executorService;
	}

	public int pickMove(State s, int[][] legalMoves) throws InterruptedException, ExecutionException {
		int piece = s.getNextPiece();
		ImmutableState currentState = new ImmutableState(s);
		Collection<Callable<EvaluationResult>> evaluationTasks = createEvaluationTasks(currentState, piece, legalMoves);
		List<Future<EvaluationResult>> evaluationResults = executorService.invokeAll(evaluationTasks);

		int maxScore = Integer.MIN_VALUE;
		int move = -1;

		for(Future<EvaluationResult> resultFuture: evaluationResults) {
			EvaluationResult result = resultFuture.get();
			int score = result.getScore();
			if(score > maxScore) {
				maxScore = score;
				move = result.getMove();
			}
		}

		return move;
	}

	public static void printState(int[][] field) {
		for(int y = State.ROWS - 1; y >= 0; --y) {
			for(int x = 0; x < State.COLS; ++x) {
				System.out.print(field[y][x] != 0 ? '*' : '_');
			}
			System.out.println();
		}
		System.out.println("---");
	}

	private Collection<Callable<EvaluationResult>> createEvaluationTasks(ImmutableState state, int piece, int[][] legalMoves) {
		evaluationTasks.clear();

		for(int moveIndex = 0; moveIndex < legalMoves.length; ++moveIndex) {
			int orientation = legalMoves[moveIndex][0];
			int position = legalMoves[moveIndex][1];
			evaluationTasks.add(new EvaluationTask(evaluator, state, moveIndex, piece, orientation, position));
		}

		return evaluationTasks;
	}

	private ExecutorService executorService;
	private MoveEvaluator evaluator = new DummyEvaluator();
	private ArrayList<Callable<EvaluationResult>> evaluationTasks = new ArrayList<Callable<EvaluationResult>>();

	//Nested classes because we are only allowed to use one file

	public static class DummyEvaluator implements MoveEvaluator {
		@Override
		public int evaluate(MoveResult moveResult) {
			return 0;
		}
	}

	public static class ImmutableState {
		public ImmutableState(State state) {
			field = copyField(state.getField());
			int[] srcTop = state.getTop();
			top = Arrays.copyOf(srcTop, srcTop.length);
		}

		public ImmutableState(int[][] field, int[] top) {
			this.field = field;
			this.top = top;
		}

		public int[][] getField() {
			return field;
		}

		public int[] getTop() {
			return top;
		}

		//returns false if you lose - true otherwise
		public MoveResult move(int piece, int orient, int slot) {
			int[][] field = copyField(this.field);
			int[] top = Arrays.copyOf(this.top, this.top.length);

			//height if the first column makes contact
			int height = top[slot] - pBottom[piece][orient][0];
			//for each column beyond the first in the piece
			for(int c = 1; c < pWidth[piece][orient];c++) {
				height = Math.max(height,top[slot+c]-pBottom[piece][orient][c]);
			}

			//check if game ended
			if(height + pHeight[piece][orient] >= ROWS) {
				return new MoveResult(field, top, true, 0);
			}

			//for each column in the piece - fill in the appropriate blocks
			for(int i = 0; i < pWidth[piece][orient]; i++) {
				//from bottom to top of brick
				for(int h = height+pBottom[piece][orient][i]; h < height+pTop[piece][orient][i]; h++) {
					field[h][i+slot] = 1;
				}
			}

			//adjust top
			for(int c = 0; c < pWidth[piece][orient]; c++) {
				top[slot+c]=height+pTop[piece][orient][c];
			}

			int rowsCleared = 0;
			//check for full rows - starting at the top
			for(int r = height+pHeight[piece][orient]-1; r >= height; r--) {
				//check all columns in the row
				boolean full = true;
				for(int c = 0; c < COLS; c++) {
					if(field[r][c] == 0) {
						full = false;
						break;
					}
				}
				//if the row was full - remove it and slide above stuff down
				if(full) {
					rowsCleared++;
					//for each column
					for(int c = 0; c < COLS; c++) {

						//slide down all bricks
						for(int i = r; i < top[c]; i++) {
							field[i][c] = field[i+1][c];
						}
						//lower the top
						top[c]--;
						while(top[c]>=1 && field[top[c]-1][c]==0)	top[c]--;
					}
				}
			}

			return new MoveResult(field, top, false, rowsCleared);
		}

		private static int[][] copyField(int[][] srcField) {
			int[][] copy = new int[ROWS][COLS];

			for(int i = 0; i < ROWS; ++i) {
				for(int j = 0; j < COLS; ++j) {
					copy[i][j] = srcField[i][j];
				}
			}

			return copy;
		}

		private final int[][] field;
		private final int[] top;

		//static
		public static final int COLS = 10;
		public static final int ROWS = 21;
		public static final int N_PIECES = 7;
		//all legal moves - first index is piece type - then a list of 2-length arrays
		private static int[][][] legalMoves = new int[N_PIECES][][];
		//indices for legalMoves
		private static final int ORIENT = 0;
		private static final int SLOT = 1;
		//possible orientations for a given piece type
		private static final int[] pOrients = {1,2,4,4,4,2,2};
		//the next several arrays define the piece vocabulary in detail
		//width of the pieces [piece ID][orientation]
		private static final int[][] pWidth = {
			{2},
			{1,4},
			{2,3,2,3},
			{2,3,2,3},
			{2,3,2,3},
			{3,2},
			{3,2}
		};
		//height of the pieces [piece ID][orientation]
		private static int[][] pHeight = {
			{2},
			{4,1},
			{3,2,3,2},
			{3,2,3,2},
			{3,2,3,2},
			{2,3},
			{2,3}
		};
		private static int[][][] pBottom = {
			{{0,0}},
			{{0},{0,0,0,0}},
			{{0,0},{0,1,1},{2,0},{0,0,0}},
			{{0,0},{0,0,0},{0,2},{1,1,0}},
			{{0,1},{1,0,1},{1,0},{0,0,0}},
			{{0,0,1},{1,0}},
			{{1,0,0},{0,1}}
		};
		private static int[][][] pTop = {
			{{2,2}},
			{{4},{1,1,1,1}},
			{{3,1},{2,2,2},{3,3},{1,1,2}},
			{{1,3},{2,1,1},{3,3},{2,2,2}},
			{{3,2},{2,2,2},{2,3},{1,2,1}},
			{{1,2,2},{3,2}},
			{{2,2,1},{2,3}}
		};

		//initialize legalMoves
		static {
			//for each piece type
			for(int i = 0; i < N_PIECES; i++) {
				//figure number of legal moves
				int n = 0;
				for(int j = 0; j < pOrients[i]; j++) {
					//number of locations in this orientation
					n += COLS+1-pWidth[i][j];
				}
				//allocate space
				legalMoves[i] = new int[n][2];
				//for each orientation
				n = 0;
				for(int j = 0; j < pOrients[i]; j++) {
					//for each slot
					for(int k = 0; k < COLS+1-pWidth[i][j];k++) {
						legalMoves[i][n][ORIENT] = j;
						legalMoves[i][n][SLOT] = k;
						n++;
					}
				}
			}
		}
	}

	public static class EvaluationTask implements Callable<EvaluationResult> {
		public EvaluationTask(MoveEvaluator evaluator, ImmutableState state, int moveIndex, int piece, int orientation, int position) {
			this.state = state;
			this.moveIndex = moveIndex;
			this.piece = piece;
			this.orientation = orientation;
			this.position = position;
			this.evaluator = evaluator;
		}

		@Override
		public EvaluationResult call() throws Exception {
			MoveResult moveResult = state.move(piece, orientation, position);
			int score = evaluator.evaluate(moveResult);
			return new EvaluationResult(moveIndex, score);
		}

		private final ImmutableState state;
		private final MoveEvaluator evaluator;
		private final int moveIndex;
		private final int piece;
		private final int orientation;
		private final int position;
	}

	public static interface MoveEvaluator {
		 public int evaluate(MoveResult moveResult);
	}

	public static class EvaluationResult {
		public EvaluationResult(int move, int score) {
			this.move = move;
			this.score = score;
		}

		public int getMove() {
			return move;
		}

		public int getScore() {
			return score;
		}

		private final int move;
		private final int score;
	}

	public static class MoveResult {
		public MoveResult(int field[][], int top[], boolean lost, int rowsCleared) {
			this.state = new ImmutableState(field, top);
			this.rowsCleared = rowsCleared;
			this.lost = lost;
		}

		public ImmutableState getState() {
			return state;
		}

		public int getRowsCleared() {
			return rowsCleared;
		}

		public boolean hasLost() {
			return lost;
		}

		private final int rowsCleared;
		private final boolean lost;
		private final ImmutableState state;
	}
}
