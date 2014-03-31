import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

class TetrisProblem implements ProblemDomain<WeightSet> {
	public static void main(String[] args) {
		System.out.println("Number of features: " + PlayerSkeleton.EVALUATORS.length);

		ForkJoinPool forkJoinPool = new ForkJoinPool();

		GeneticAlgorithmConfig config =
			new GeneticAlgorithmConfig(forkJoinPool)
			    .setCrossoverRate(0.7f)
			    .setMutationRate(0.01f)
			    .setPopulationSize(100);
		try {
			GeneFitnessPair<WeightSet> fittest =
					GeneticAlgorithm.run(new TetrisProblem(forkJoinPool), config);

			System.out.println();
			System.out.println("Best score: " + fittest.getFitness());
			System.out.println("Weights:");
			printGene(fittest);
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
	public WeightSet newRandomGene() {
		WeightSet gene = newGene();
		float[] weights = gene.getWeights();
		for(int weightIndex = 0; weightIndex < weights.length; ++weightIndex) {
			weights[weightIndex] = randomChromosome();
		}

		return new WeightSet(weights);
	}

	@Override
	public boolean canTerminate(Iterable<GeneFitnessPair<WeightSet>> population) {
		float maxScore = -Float.MAX_VALUE;
		GeneFitnessPair<WeightSet> bestGene = null;
		for(GeneFitnessPair<WeightSet> pair: population) {
			float score = pair.getFitness();
			if(score > maxScore) {
				maxScore = score;
				bestGene = pair;
			}
		}

		if(maxScore > bestScore) {
			bestScore = maxScore;
			numLostGenerations = 0;
			System.out.println();
			System.out.println("Score: " + bestScore);
			printGene(bestGene);
			return false;
		}
		else {
			++numLostGenerations;
			System.out.print(".");
			return numLostGenerations > 20;
		}
	}

	@Override
	public float evaluateFitness(WeightSet gene) {
		final int NUM_GAMES = 50;
		ArrayList<WeightSet> inputs = new ArrayList<WeightSet>(NUM_GAMES);
		for(int i = 0; i < NUM_GAMES; ++i) { inputs.add(gene); }
		return mapReduce.mapReduce(EVAL_FUNC, AVG_SCORE, inputs);
	}

	@Override
	public void mutate(WeightSet gene, int mutatedChromosomeIndex) {
		gene.getWeights()[mutatedChromosomeIndex] = randomChromosome();
	}

	@Override
	public WeightSet[] crossover(WeightSet parent1, WeightSet parent2,
			int crossOverPoint) {

		float[] parent1Weights = parent1.getWeights();
		float[] parent2Weights = parent2.getWeights();
		int numChromosomes = parent1Weights.length;

		WeightSet children1 = newGene();
		WeightSet children2 = newGene();
		float[] weights1 = children1.getWeights();
		float[] weights2 = children2.getWeights();

		System.arraycopy(parent1Weights, 0, weights1, 0, crossOverPoint);
		System.arraycopy(parent2Weights, 0, weights2, 0, crossOverPoint);
		System.arraycopy(parent1Weights, crossOverPoint, weights2, crossOverPoint, numChromosomes - crossOverPoint);
		System.arraycopy(parent2Weights, crossOverPoint, weights1, crossOverPoint, numChromosomes - crossOverPoint);

		return new WeightSet[] { children1, children2 };
	}

	private float randomChromosome() {
		return random.nextFloat() * 1000.0f;
	}

	private static WeightSet newGene() {
		float[] weights = new float[PlayerSkeleton.EVALUATORS.length];
		return new WeightSet(weights);
	}

	private static void printGene(GeneFitnessPair<WeightSet> fittest) {
		System.out.print("{ ");
		boolean first = true;
		for(float weight: fittest.getGene().getWeights()) {
			if(first) {
				first = false;
			}
			else {
				System.out.print("f, ");
			}

			System.out.print(weight);
		}
		System.out.println("f }");
	}


	private ForkJoinPool forkJoinPool;
	private PlayerSkeleton.MapReduce mapReduce;
	private Random random = new Random();
	private float bestScore = -Float.MAX_VALUE;
	private int numLostGenerations = 0;

	private final PlayerSkeleton.MapFunc<WeightSet, Float> EVAL_FUNC = new PlayerSkeleton.MapFunc<WeightSet, Float>() {
		@Override
		public Float map(WeightSet gene) {
			State state = new State();
			PlayerSkeleton player = new PlayerSkeleton(forkJoinPool, gene.getWeights());
			while(!state.hasLost()) {
				state.makeMove(player.pickMove(state, state.legalMoves()));
			}
			return (float)state.getRowsCleared();
		}
	};
	
	private final PlayerSkeleton.ReduceFunc<Float, Float> PERCENTILE_SCORE = new PlayerSkeleton.ReduceFunc<Float, Float>() {
		@Override
		public Float reduce(Iterable<Float> inputs) {
			ArrayList<Float> scores = new ArrayList<Float>();
			for(Float score: inputs) {
				scores.add(score);
			}

			float PERCENTILE = 0.5f;
			int index = Math.round(scores.size() * PERCENTILE) - 1;

			return scores.get(index);
		}
	};

	private final PlayerSkeleton.ReduceFunc<Float, Float> AVG_SCORE = new PlayerSkeleton.ReduceFunc<Float, Float>() {
		@Override
		public Float reduce(Iterable<Float> inputs) {
			int numGames = 0;
			float sum = 0.0f;

			for(Float score: inputs) {
				sum += score;
				++numGames;
			}

			return sum / numGames;
		}
	};

	private final PlayerSkeleton.ReduceFunc<Float, Float> MIN_SCORE = new PlayerSkeleton.ReduceFunc<Float, Float>() {
		@Override
		public Float reduce(Iterable<Float> inputs) {
			Float minScore = Float.MAX_VALUE;

			for(Float score: inputs) {
				if(score < minScore) {
					minScore = score;
				}
			}

			return minScore;
		}
	};
}