import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class PlayerSkeleton {
	public static void main(String[] args) {
		State s = new State();

		ForkJoinPool executorService = new ForkJoinPool();
		PlayerSkeleton p = new PlayerSkeleton(executorService);

		new TFrame(s);
		try {
			while(!s.hasLost()) {
				s.makeMove(p.pickMove(s,s.legalMoves()));
				s.draw();
				s.drawNext(0,0);
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			executorService.shutdown();
		}

		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}

	public static final MoveEvaluator[] EVALUATORS;
	static {
		ArrayList<MoveEvaluator> evaluators = new ArrayList<MoveEvaluator>();

		//Column heights
		for(int columnIndex = 0; columnIndex < State.COLS; ++columnIndex) {
			evaluators.add(new ColumnHeight(columnIndex));
		}

		evaluators.add(new MaxColumnHeight());

		EVALUATORS = evaluators.toArray(new MoveEvaluator[evaluators.size()]);
	}

	public PlayerSkeleton(ForkJoinPool forkJoinPool) {
		this.mapReduce = new MapReduce(forkJoinPool);
		float[] weights = new float[]
		{ 33.519493f, 26.983864f, 27.579832f, 39.10369f, 37.06415f, 45.665203f, 40.899426f, 41.192616f, 92.94577f, 35.54414f, 67.767006f }
		;
		this.evaluator = new WeightedSumEvaluator(EVALUATORS, weights);
	}

	public PlayerSkeleton(ForkJoinPool forkJoinPool, float[] weights) {
		this.mapReduce = new MapReduce(forkJoinPool);
		this.evaluator = new WeightedSumEvaluator(EVALUATORS, weights);
	}

	public int pickMove(State s, int[][] legalMoves) {
		int piece = s.getNextPiece();
		ImmutableState currentState = new ImmutableState(s);
		possibleMoves.clear();
		for(int moveIndex = 0; moveIndex < legalMoves.length; ++moveIndex) {
			int orientation = legalMoves[moveIndex][0];
			int position = legalMoves[moveIndex][1];
			possibleMoves.add(new Move(currentState, moveIndex, piece, orientation, position));
		}

		return mapReduce.mapReduce(EVAL_MOVE_FUNC, PICK_MOVE_FUNC, possibleMoves);
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

	private MoveEvaluator evaluator;
	private MapReduce mapReduce;
	private ArrayList<Move> possibleMoves = new ArrayList<Move>();

	private final MapFunc<Move, EvaluationResult> EVAL_MOVE_FUNC = new MapFunc<Move, EvaluationResult>() {
		@Override
		public EvaluationResult map(Move move) {
			ImmutableState state = move.getState();
			MoveResult moveResult = state.move(move.getPiece(), move.getOrientation(), move.getPosition());
			float score = evaluator.map(moveResult);
			return new EvaluationResult(move.getIndex(), score);
		}
	};

	private static final ReduceFunc<EvaluationResult, Integer> PICK_MOVE_FUNC = new ReduceFunc<EvaluationResult, Integer>() {
		public Integer reduce(Iterable<EvaluationResult> results) {
			float maxScore = -Float.MAX_VALUE;
			int move = -1;

			for(EvaluationResult result: results) {
				float score = result.getScore();
				if(score > maxScore) {
					maxScore = score;
					move = result.getMove();
				}
			}

			return move;
		}
	};

	//Nested classes because we are only allowed to use one file
	/**
	 * An evaluator which uses a weighted sum of features as score
	 */
	public static class WeightedSumEvaluator implements MoveEvaluator {
		public WeightedSumEvaluator(MoveEvaluator[] evaluators, float[] weights) {
			this.evaluators = evaluators;
			this.weights = weights;
		}

		@Override
		public Float map(MoveResult moveResult) {
			float sum = 0.0f;

			for(int i = 0; i < evaluators.length; ++i) {
				float score = evaluators[i].map(moveResult);
				sum += score * weights[i];
			}

			return sum;
		}

		private final MoveEvaluator[] evaluators;
		private final float[] weights;
	}

	/**
	 * Doesn't do anything, just return 0 for testing purposes
	 */
	public static class DummyEvaluator implements MoveEvaluator {
		@Override
		public Float map(MoveResult moveResult) {
			return 0.0f;
		}
	}

	public static class ColumnHeight implements MoveEvaluator {
		public ColumnHeight(int columnIndex) {
			this.columnIndex = columnIndex;
		}

		@Override
		public Float map(MoveResult moveResult) {
			return -(float)moveResult.getState().getTop()[columnIndex];//column height is a negative trait
		}

		private int columnIndex;
	}

	public static class MaxColumnHeight implements MoveEvaluator {
		@Override
		public Float map(MoveResult result) {
			int[] top = result.getState().getTop();

			int maxHeight = Integer.MIN_VALUE;
			for(int column = 0; column < top.length; ++column) {
				int height = top[column];
				if(height > maxHeight) {
					maxHeight = height;
				}
			}

			return -(float)maxHeight;
		}
	}

	/**
	 * A state that is more useful then the provided one.
	 * It is immutable and suitable for parallel processing
	 */
	public static class ImmutableState {
		/**
		 * Construct a state which is identical to the built-in state
		 */
		public ImmutableState(State state) {
			field = copyField(state.getField());
			int[] srcTop = state.getTop();
			top = Arrays.copyOf(srcTop, srcTop.length);
		}

		/**
		 * Construct a state with the given field and top
		 * @param field
		 * @param top
		 */
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

		/**
		 * Make a move
		 * @param piece
		 * @param orient
		 * @param slot
		 * @return result of the move
		 */
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

	public static class MapReduce {
		public MapReduce(ForkJoinPool forkJoinPool) {
			this.forkJoinPool = forkJoinPool;
		}

		public <Src, Dst> void map(MapFunc<Src, Dst> mapFunc, Iterable<Src> inputs, Collection<Dst> outputs) {
			forkJoinPool.invoke(new MapTask<Src, Dst>(mapFunc, inputs, outputs));
		}

		public <SrcT, IntT, DstT> DstT mapReduce(
				MapFunc<SrcT, IntT> mapFunc, ReduceFunc<IntT, DstT> reduceFunc, Iterable<SrcT> inputs) {

			return forkJoinPool.invoke(new MapReduceTask<SrcT, IntT, DstT>(mapFunc, reduceFunc, inputs));
		}

		private final ForkJoinPool forkJoinPool;
	}

	public static interface MapFunc<SrcT, DstT> {
		public DstT map(SrcT input);
	}

	public static interface ReduceFunc<SrcT, DstT> {
		public DstT reduce(Iterable<SrcT> inputs);
	}

	public static class MapTask<SrcT, DstT> extends ForkJoinTask<Void> {
		public MapTask(MapFunc<SrcT, DstT> mapFunc, Iterable<SrcT> inputs, Collection<DstT> outputs) {
			this.mapFunc = mapFunc;
			this.inputs = inputs;
			this.outputs = outputs;
		}

		@Override
		protected boolean exec() {
			ArrayList<ForkJoinTask<DstT>> applyTasks = new ArrayList<ForkJoinTask<DstT>>();
			for(SrcT input: inputs) {
				applyTasks.add(new ApplyTask(input));
			}
			invokeAll(applyTasks);

			for(ForkJoinTask<DstT> applyTask: applyTasks) {
				outputs.add(applyTask.join());
			}

			return true;
		}

		@Override
		public Void getRawResult() {
			return null;
		}

		@Override
		protected void setRawResult(Void value) {
		}

		private final MapFunc<SrcT, DstT> mapFunc;
		private final Iterable<SrcT> inputs;
		private final Collection<DstT> outputs;
		private static final long serialVersionUID = 1L;

		private class ApplyTask extends ForkJoinTask<DstT> {
			public ApplyTask(SrcT input) {
				this.input = input;
			}

			@Override
			protected boolean exec() {
				setRawResult(mapFunc.map(input));
				return true;
			}

			@Override
			public DstT getRawResult() {
				return output;
			}

			@Override
			protected void setRawResult(DstT value) {
				output = value;
			}

			private final SrcT input;
			private DstT output;

			private static final long serialVersionUID = 1L;
		}
	}

	public static class MapReduceTask<SrcT, IntT, DstT> extends ForkJoinTask<DstT> {
		public MapReduceTask(
				MapFunc<SrcT, IntT> mapFunc, ReduceFunc<IntT, DstT> reduceFunc, Iterable<SrcT> inputs) {
			this.inputs = inputs;
			this.mapFunc = mapFunc;
			this.reduceFunc = reduceFunc;
		}

		@Override
		protected boolean exec() {
			//Map
			ArrayList<IntT> mapResults = new ArrayList<IntT>();
			MapTask<SrcT, IntT> mapTask = new MapTask<SrcT, IntT>(mapFunc, inputs, mapResults);
			mapTask.invoke();
			//Reduce
			setRawResult(reduceFunc.reduce(mapResults));

			return true;
		}

		@Override
		public DstT getRawResult() {
			return output;
		}

		@Override
		protected void setRawResult(DstT value) {
			output = value;
		}

		private final Iterable<SrcT> inputs;
		private DstT output = null;
		private MapFunc<SrcT, IntT> mapFunc;
		private ReduceFunc<IntT, DstT> reduceFunc;
		private static final long serialVersionUID = 1L;
	}

	/**
	 * A common interface for different kind of evaluator
	 */
	public interface MoveEvaluator extends MapFunc<MoveResult, Float> {}

	private static class Move {
		public Move(ImmutableState state, int index, int piece, int orientation, int position) {
			this.state = state;
			this.index = index;
			this.piece = piece;
			this.orientation = orientation;
			this.position = position;
		}

		public ImmutableState getState() { return state; }
		public int getIndex() { return index; }
		public int getPiece() { return piece; }
		public int getOrientation() { return orientation; }
		public int getPosition() { return position; }

		private final ImmutableState state;
		private final int index;
		private final int piece;
		private final int orientation;
		private final int position;
	}

	/**
	 * A simple class to hold the evaluation result of a move
	 */
	public static class EvaluationResult {
		public EvaluationResult(int move, float score) {
			this.move = move;
			this.score = score;
		}

		public int getMove() {
			return move;
		}

		public float getScore() {
			return score;
		}

		private final int move;
		private final float score;
	}

	/**
	 * Result of a move, returned by ImmutableState.move
	 */
	public static class MoveResult {
		public MoveResult(int field[][], int top[], boolean lost, int rowsCleared) {
			this.state = new ImmutableState(field, top);
			this.rowsCleared = rowsCleared;
			this.lost = lost;
		}

		public ImmutableState getState() { return state; }
		public int getRowsCleared() { return rowsCleared; }
		public boolean hasLost() { return lost; }

		private final int rowsCleared;
		private final boolean lost;
		private final ImmutableState state;
	}
}
