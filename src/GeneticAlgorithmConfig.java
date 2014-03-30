import java.util.concurrent.ExecutorService;


public class GeneticAlgorithmConfig {
	public GeneticAlgorithmConfig(ExecutorService executorService) {
		this.executorService = executorService;
	}

	public ExecutorService getExecutorService() {
		return executorService;
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

	private ExecutorService executorService;
	private float crossoverRate = 0.6f;
	private float mutationRate = 0.01f;
	private int populationSize = 10;
}
