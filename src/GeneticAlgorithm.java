import java.util.ArrayList;
import java.util.Random;

/**
 * Trains and optimizes weights for features
 */
public class GeneticAlgorithm {
	public static <T extends Gene> GeneFitnessPair<T> run(
			ProblemDomain<T> problemDomain, GeneticAlgorithmConfig config) {

		//Initialize
		Random random = new Random();
		ArrayList<GeneFitnessPair<T>> fitnessResults = new ArrayList<GeneFitnessPair<T>>();
		int populationSize = config.getPopulationSize();
		float crossoverRate = config.getCrossoverRate();
		float mutationRate = config.getMutationRate();
		PlayerSkeleton.MapReduce mapReduce = new PlayerSkeleton.MapReduce(config.getForkJoinPool());
		PlayerSkeleton.MapFunc<T, GeneFitnessPair<T>> fitnessFunction = new FitnessFunction<T>(problemDomain);
		GeneFitnessPair<T> bestGene = null;
		float maxScore = -Float.MAX_VALUE;

		//Create a population
		ArrayList<T> population = new ArrayList<T>();
		for(int i = 0; i < populationSize; ++i) {
			population.add(problemDomain.newRandomGene());
		}

		do {
			//Evaluate fitness of population
			mapReduce.map(fitnessFunction, population, fitnessResults);

			//Create next generation
			ArrayList<T> nextGeneration = new ArrayList<T>();
			//Calculate sum of fitness for roulette-based selection
			float totalFitness = 0.0f;
			for(GeneFitnessPair<T> result: fitnessResults) {
				totalFitness += result.getFitness();
			}
			//Keep creating offspring until we have a full new population
			while(nextGeneration.size() < populationSize) {
				T parent1 = pickRandom(random, totalFitness, fitnessResults);
				T parent2 = pickRandom(random, totalFitness, fitnessResults);

				if(random.nextFloat() < crossoverRate) {//cross over happens
					int crossOverPoint = random.nextInt(parent1.getNumChromosomes());
					T[] children = problemDomain.crossover(parent1, parent2, crossOverPoint);
					for(T child: children) {
						nextGeneration.add(child);
					}
				}
				else {//clone
					nextGeneration.add(parent1);
					nextGeneration.add(parent2);
				}
			}

			//Mutation
			for(T gene: nextGeneration) {
				if(random.nextFloat() < mutationRate) {//mutation happens
					int mutatedChromosomeIndex = random.nextInt(gene.getNumChromosomes());
					problemDomain.mutate(gene, mutatedChromosomeIndex);
				}
			}

			//Find the best gene
			for(GeneFitnessPair<T> pair: fitnessResults) {
				float score = pair.getFitness();
				if(score > maxScore) {
					maxScore = score;
					bestGene = pair;
				}
			}
		} while(!problemDomain.canTerminate(fitnessResults));

		return bestGene;
	}

	private static <T extends Gene> T pickRandom(
			Random random, float totalFitness, ArrayList<GeneFitnessPair<T>> fitnessResults) {
		float decision = random.nextFloat() * totalFitness;
		for(GeneFitnessPair<T> result: fitnessResults) {
			float fitness = result.getFitness();

			if(decision < fitness) {
				return result.getGene();
			}
			else {
				decision -= fitness;
			}
		}

		return fitnessResults.get(0).getGene();
	}

	private static class FitnessFunction<T extends Gene> implements PlayerSkeleton.MapFunc<T, GeneFitnessPair<T>> {
		public FitnessFunction(ProblemDomain<T> problemDomain) {
			this.problemDomain = problemDomain;
		}

		@Override
		public GeneFitnessPair<T> map(T gene) {
			return new GeneFitnessPair<T>(gene, problemDomain.evaluateFitness(gene));
		}

		private ProblemDomain<T> problemDomain;
	}
}
