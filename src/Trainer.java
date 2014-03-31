import java.util.SortedSet;
import java.util.concurrent.ForkJoinPool;

/**
 * A program which finds the best weights for all features
 */
public class Trainer {
	public static void main(String[] args) {
		ForkJoinPool forkJoinPool = new ForkJoinPool();

		GeneticAlgorithmConfig config =
			new GeneticAlgorithmConfig(forkJoinPool)
			    .setCrossoverRate(0.6f)
			    .setMutationRate(0.1f)
			    .setPopulationSize(40);

		try {
			SortedSet<GeneFitnessPair<WeightSet>> bestGenes =
					GeneticAlgorithm.run(new TetrisProblem(forkJoinPool), config);
			GeneFitnessPair<WeightSet> fittest = bestGenes.last();

			System.out.println("Best score: " + fittest.getFitness());
			System.out.println("Weights:");
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
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			forkJoinPool.shutdown();
		}
	}
}
