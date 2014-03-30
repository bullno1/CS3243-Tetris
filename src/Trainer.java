import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * A program which finds the best weights for all features
 */
public class Trainer {
	public static void main(String[] args) {
		int numProcessors = Runtime.getRuntime().availableProcessors();
		LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		ExecutorService executorService = new ThreadPoolExecutor(numProcessors, numProcessors, 0, TimeUnit.MILLISECONDS, queue);

		GeneticAlgorithmConfig config =
			new GeneticAlgorithmConfig(executorService)
			    .setCrossoverRate(0.6f)
			    .setMutationRate(0.1f)
			    .setPopulationSize(20);

		try {
			SortedSet<GeneFitnessPair<WeightSet>> bestGenes =
					GeneticAlgorithm.run(new TetrisProblem(executorService), config);
			GeneFitnessPair<WeightSet> fittest = bestGenes.last();
			System.out.println("Best score: " + fittest.getFitness());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			executorService.shutdown();
		}
	}

	private static class TetrisProblem implements ProblemDomain<WeightSet> {
		public TetrisProblem(ExecutorService executorService) {
			this.executorService = executorService;
		}

		@Override
		public WeightSet newRandomGene() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean canTerminate(Iterable<WeightSet> population) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public float evaluateFitness(WeightSet gene) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void mutate(WeightSet gene, int mutatedChromosomeIndex) {
			// TODO Auto-generated method stub

		}

		@Override
		public WeightSet[] crossover(WeightSet parent1, WeightSet parent2,
				int crossOverPoint) {
			return null;
		}

		private ExecutorService executorService;
	}

	private static class WeightSet implements Gene {
		public WeightSet(float[] weights) {
			this.weights = weights;
		}

		@Override
		public int getNumChromosomes() {
			return weights.length;
		}

		public float[] getWeights() {
			return weights;
		}

		float[] weights;
	}
}
