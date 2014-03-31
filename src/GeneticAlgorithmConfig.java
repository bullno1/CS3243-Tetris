import java.util.concurrent.ForkJoinPool;


public class GeneticAlgorithmConfig {
	public GeneticAlgorithmConfig(ForkJoinPool forkJoinPool) {
		this.forkJoinPool = forkJoinPool;
	}

	public ForkJoinPool getForkJoinPool() {
		return forkJoinPool;
	}

	public float getCrossoverRate() {
		return crossoverRate;
	}

	public GeneticAlgorithmConfig setCrossoverRate(float value) {
		crossoverRate = value;
		return this;
	}

	public float getMutationRate() {
		return mutationRate;
	}

	public GeneticAlgorithmConfig setMutationRate(float value) {
		mutationRate = value;
		return this;
	}

	public int getPopulationSize() {
		return populationSize;
	}

	public GeneticAlgorithmConfig setPopulationSize(int value) {
		populationSize = value;
		return this;
	}

	private ForkJoinPool forkJoinPool;
	private float crossoverRate = 0.6f;
	private float mutationRate = 0.01f;
	private int populationSize = 10;
}
