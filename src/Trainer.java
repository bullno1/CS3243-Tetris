import java.util.concurrent.ForkJoinPool;

/**
 * A program which finds the best weights for all features
 */
public class Trainer {
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
