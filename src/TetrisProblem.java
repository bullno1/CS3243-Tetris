import java.util.Random;
import java.util.concurrent.ForkJoinPool;

class TetrisProblem implements ProblemDomain<WeightSet> {
	public TetrisProblem(ForkJoinPool forkJoinPool) {
		this.forkJoinPool = forkJoinPool;
	}

	@Override
	public WeightSet newRandomGene() {
		WeightSet gene = newGene();
		float[] weights = gene.getWeights();
		for(int weightIndex = 0; weightIndex < weights.length; ++weightIndex) {
			weights[weightIndex] = random.nextFloat() * 4;
		}

		return new WeightSet(weights);
	}

	@Override
	public boolean canTerminate(Iterable<GeneFitnessPair<WeightSet>> population) {
		float maxScore = -Float.MAX_VALUE;
		for(GeneFitnessPair<WeightSet> pair: population) {
			float score = pair.getFitness();
			if(score > maxScore) {
				maxScore = score;
			}
		}
		
		return maxScore > 5.f;
	}

	@Override
	public float evaluateFitness(WeightSet gene) {
		State state = new State();
		PlayerSkeleton player = new PlayerSkeleton(forkJoinPool, gene.getWeights());
		while(!state.hasLost()) {
			state.makeMove(player.pickMove(state, state.legalMoves()));
		}
		return (float)state.getRowsCleared();
	}

	@Override
	public void mutate(WeightSet gene, int mutatedChromosomeIndex) {
		gene.getWeights()[mutatedChromosomeIndex] = random.nextFloat() * 4;
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
	
	private static WeightSet newGene() {
		float[] weights = new float[PlayerSkeleton.EVALUATORS.length];
		return new WeightSet(weights);
	}

	private ForkJoinPool forkJoinPool;
	private Random random = new Random();
}