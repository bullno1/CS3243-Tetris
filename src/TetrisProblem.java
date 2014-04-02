import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

class TetrisProblem implements ProblemDomain<WeightSet> {
	public static final int SEQUENCE_LENGTH = 10000000;
	public static final int NUM_SEQUENCES = 5;
	public static final int MAX_LOST_GENERATIONS = 20;
	public static final float CROSSOVER_RATE = 0.6f;
	public static final float MUTATION_RATE = 0.01f;
	public static final int POPULATION_SIZE = 100;

	public static void main(String[] args) {
		System.out.println("Number of features: " + PlayerSkeleton.EVALUATORS.length);

		ForkJoinPool forkJoinPool = new ForkJoinPool();

		GeneticAlgorithmConfig config =
			new GeneticAlgorithmConfig(forkJoinPool)
			    .setCrossoverRate(CROSSOVER_RATE)
			    .setMutationRate(MUTATION_RATE)
			    .setPopulationSize(POPULATION_SIZE);
		try {
			ChromosomeFitnessPair<WeightSet> fittest =
					GeneticAlgorithm.run(new TetrisProblem(forkJoinPool), config);

			System.out.println();
			System.out.println("Best score: " + fittest.getFitness());
			System.out.println("Weights:");
			printChromosome(fittest);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			forkJoinPool.shutdown();
		}
	}

	public TetrisProblem(ForkJoinPool forkJoinPool) {
		this.forkJoinPool = forkJoinPool;
		this.mapReduce = new PlayerSkeleton.MapReduce(forkJoinPool);
	}

	@Override
	public WeightSet newRandomChromosome() {
		WeightSet chromosome = newChromosome();
		float[] weights = chromosome.getWeights();
		for(int weightIndex = 0; weightIndex < weights.length; ++weightIndex) {
			weights[weightIndex] = randomGene();
		}

		return new WeightSet(weights);
	}

	@Override
	public void beginGeneration() {
		//Create new sequences to use for this generation's evaluation
		for(int seqIndex = 0; seqIndex < NUM_SEQUENCES; ++seqIndex) {
			for(int pieceIndex = 0; pieceIndex < SEQUENCE_LENGTH; ++pieceIndex) {
				sequences[seqIndex][pieceIndex] = random.nextInt(State.N_PIECES);
			}
		}
	}

	@Override
	public boolean endGeneration(Iterable<ChromosomeFitnessPair<WeightSet>> generation) {
		float maxScore = -Float.MAX_VALUE;
		ChromosomeFitnessPair<WeightSet> bestChromosome = null;
		for(ChromosomeFitnessPair<WeightSet> pair: generation) {
			float score = pair.getFitness();
			if(score > maxScore) {
				maxScore = score;
				bestChromosome = pair;
			}
		}

		System.out.println("Score: " + maxScore);
		printChromosome(bestChromosome);

		if(maxScore > bestScore) {
			bestScore = maxScore;
			numLostGenerations = 0;
			for(int i = 0; i < MAX_LOST_GENERATIONS; ++i) {
				System.out.print("+");
			}
			System.out.println();
			return true;
		}
		else {
			++numLostGenerations;
			for(int i = 0; i < MAX_LOST_GENERATIONS - numLostGenerations; ++i) {
				System.out.print("-");
			}
			System.out.println();
			return numLostGenerations < MAX_LOST_GENERATIONS;
		}
	}

	@Override
	public float evaluateFitness(WeightSet chromosome) {
		ArrayList<TestConfig> testConfigs = new ArrayList<TestConfig>(NUM_SEQUENCES);
		for(int i = 0; i < NUM_SEQUENCES; ++i) {
			testConfigs.add(new TestConfig(chromosome.getWeights(), sequences[i], forkJoinPool));
		}

		return mapReduce.mapReduce(FITNESS_FUNC, AVG_SCORE, testConfigs);
	}

	@Override
	public void mutate(WeightSet chromosome, int mutatedChromosomeIndex) {
		chromosome.getWeights()[mutatedChromosomeIndex] = randomGene();
	}

	@Override
	public WeightSet[] crossover(WeightSet parent1, WeightSet parent2,
			int crossoverPoint) {

		float[] parent1Weights = parent1.getWeights();
		float[] parent2Weights = parent2.getWeights();
		int numChromosomes = parent1Weights.length;

		WeightSet children1 = newChromosome();
		WeightSet children2 = newChromosome();
		float[] weights1 = children1.getWeights();
		float[] weights2 = children2.getWeights();

		System.arraycopy(parent1Weights, 0, weights1, 0, crossoverPoint);
		System.arraycopy(parent2Weights, 0, weights2, 0, crossoverPoint);
		System.arraycopy(parent1Weights, crossoverPoint, weights2, crossoverPoint, numChromosomes - crossoverPoint);
		System.arraycopy(parent2Weights, crossoverPoint, weights1, crossoverPoint, numChromosomes - crossoverPoint);

		return new WeightSet[] { children1, children2 };
	}

	private float randomGene() {
		return random.nextFloat() * 1000.0f;
	}

	private static WeightSet newChromosome() {
		float[] weights = new float[PlayerSkeleton.EVALUATORS.length];
		return new WeightSet(weights);
	}

	private static void printChromosome(ChromosomeFitnessPair<WeightSet> fittest) {
		System.out.print("{ ");
		boolean first = true;
		for(float weight: fittest.getChromosome().getWeights()) {
			if(first) {
				first = false;
			}
			else {
				System.out.print("f, ");
			}

			System.out.print(new BigDecimal(weight));
		}
		System.out.println("f }");
	}

	private ForkJoinPool forkJoinPool;
	private Random random = new Random();
	private float bestScore = -Float.MAX_VALUE;
	private int numLostGenerations = 0;
	private PlayerSkeleton.MapReduce mapReduce;
	private int[][] sequences = new int[NUM_SEQUENCES][SEQUENCE_LENGTH];

	private static final PlayerSkeleton.MapFunc<TestConfig, Float> FITNESS_FUNC =
	new PlayerSkeleton.MapFunc<TestConfig, Float>() {
		@Override
		public Float map(TestConfig config) {

			PlayerSkeleton.ImmutableState state = new PlayerSkeleton.ImmutableState();
			PlayerSkeleton player = new PlayerSkeleton(config.getForkJoinPool(), config.getWeights());

			int turn = 0;
			boolean hasLost = false;
			int rowsCleared = 0;
			int[] sequence = config.getSequence();

			while(!hasLost) {
				int piece = sequence[turn];
				int[][] legalMoves = State.legalMoves[piece];
				int move = player.pickMove(state, piece, State.legalMoves[piece]);
				PlayerSkeleton.MoveResult result = state.move(piece, legalMoves[move][0], legalMoves[move][1]);
				state = result.getState();
				hasLost = result.hasLost();
				rowsCleared += result.getRowsCleared();

				++turn;
			}
			return (float)rowsCleared;
		}
	};

	private static final PlayerSkeleton.ReduceFunc<Float, Float> AVG_SCORE =
	new PlayerSkeleton.ReduceFunc<Float, Float>() {
		@Override
		public Float reduce(Iterable<Float> inputs) {
			int count = 0;
			float sum = 0.0f;

			for(float num: inputs) {
				sum += num;
				++count;
			}

			return sum / (float)count;
		}
	};

	private static class TestConfig {
		public TestConfig(float[] weights, int[] sequence, ForkJoinPool forkJoinPool) {
			this.weights = weights;
			this.sequence = sequence;
			this.forkJoinPool = forkJoinPool;
		}

		public float[] getWeights() {
			return weights;
		}

		public int[] getSequence() {
			return sequence;
		}

		public ForkJoinPool getForkJoinPool() {
			return forkJoinPool;
		}

		private final ForkJoinPool forkJoinPool;
		private final float[] weights;
		private final int[] sequence;
	}
}