import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;

public class QuickRunner {
	public static void main(String[] args) {
		final int NUM_GAMES = args.length >= 1 ? Integer.parseInt(args[0]) : 100;

		ForkJoinPool forkJoinPool = new ForkJoinPool();
		PlayerSkeleton.MapReduce mapReduce = new PlayerSkeleton.MapReduce(forkJoinPool);
		ArrayList<ForkJoinPool> inputs = new ArrayList<ForkJoinPool>(NUM_GAMES);
		for(int i = 0; i < NUM_GAMES; ++i) {
			inputs.add(forkJoinPool);
		}

		try {
			System.out.println("Running " + NUM_GAMES + " games. Please be patient.");

			GameStats stats = mapReduce.mapReduce(PLAY_GAME, AGGREGATE_STATS, inputs);

			System.out.println("Rows cleared stats: ");
			System.out.println("Min: " + stats.getScoreMin());
			System.out.println("Max: " + stats.getScoreMax());
			System.out.println("Avg: " + stats.getScoreAvg());
			System.out.println("Std: " + stats.getScoreStd());

			System.out.println();

			System.out.println("Game length stats: ");
			System.out.println("Min: " + stats.getLengthMin());
			System.out.println("Max: " + stats.getLengthMax());
			System.out.println("Avg: " + stats.getLengthAvg());
			System.out.println("Std: " + stats.getLengthStd());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			forkJoinPool.shutdown();
		}
	}

	private static final PlayerSkeleton.MapFunc<ForkJoinPool, State> PLAY_GAME =
			new PlayerSkeleton.MapFunc<ForkJoinPool, State>() {

				@Override
				public State map(ForkJoinPool forkJoinPool) {
					State s = new State();

					PlayerSkeleton p = new PlayerSkeleton(forkJoinPool);
					while(!s.hasLost()) {
						s.makeMove(p.pickMove(s,s.legalMoves()));
					}

					return s;
				}
	};

	private static final PlayerSkeleton.ReduceFunc<State, GameStats> AGGREGATE_STATS =
			new PlayerSkeleton.ReduceFunc<State, GameStats>() {
				@Override
				public GameStats reduce(Iterable<State> games) {
					return new GameStats(games);
				}
	};

	private static class GameStats {
		public GameStats(Iterable<State> games) {
			ArrayList<Float> scores = new ArrayList<Float>();
			ArrayList<Float> lengths = new ArrayList<Float>();

			for(State game: games) {
				scores.add((float)game.getRowsCleared());
				lengths.add((float)game.getTurnNumber());
			}

			scoreMin = min(scores);
			scoreMax = max(scores);
			scoreAvg = mean(scores);
			scoreStd = std(scores, scoreAvg);

			lengthMin = min(lengths);
			lengthMax = max(lengths);
			lengthAvg = mean(lengths);
			lengthStd = std(lengths, lengthAvg);
		}

		private float min(Iterable<Float> numbers) {
			float min = Float.MAX_VALUE;

			for(float num: numbers) {
				min = Math.min(min, num);
			}

			return min;
		}

		private float max(Iterable<Float> numbers) {
			float max = -Float.MAX_VALUE;

			for(float num: numbers) {
				max = Math.max(max, num);
			}

			return max;
		}

		private float mean(Iterable<Float> numbers) {
			float sum = 0.0f;
			int count = 0;

			for(float num: numbers) {
				sum += num;
				++count;
			}

			return sum / (float)count;
		}

		private float std(Iterable<Float> numbers, float mean) {
			float sumOfSquaredDiff = 0.0f;
			int count = 0;

			for(float num: numbers) {
				++count;
				float diff = num - mean;
				sumOfSquaredDiff += diff * diff;
			}

			return (float)Math.sqrt(sumOfSquaredDiff / count);
		}

		public float getScoreMin() {
			return scoreMin;
		}

		public float getScoreMax() {
			return scoreMax;
		}

		public float getScoreAvg() {
			return scoreAvg;
		}

		public float getScoreStd() {
			return scoreStd;
		}

		public float getLengthMin() {
			return lengthMin;
		}

		public float getLengthMax() {
			return lengthMax;
		}

		public float getLengthAvg() {
			return lengthAvg;
		}

		public float getLengthStd() {
			return lengthStd;
		}

		private final float scoreMin;
		private final float scoreMax;
		private final float scoreAvg;
		private final float scoreStd;

		private final float lengthMin;
		private final float lengthMax;
		private final float lengthAvg;
		private final float lengthStd;
	}
}
