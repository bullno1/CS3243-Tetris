import java.util.ArrayList;
import java.util.Random;

/**
 * Trains and optimizes weights for features
 */
public class GeneticAlgorithm {
	public static <T extends Chromosome> ChromosomeFitnessPair<T> run(
			ProblemDomain<T> problemDomain, GeneticAlgorithmConfig config) {

		//Initialize
		Random random = new Random();
		ArrayList<ChromosomeFitnessPair<T>> fitnessResults = new ArrayList<ChromosomeFitnessPair<T>>();
		int populationSize = config.getPopulationSize();
		float crossoverRate = config.getCrossoverRate();
		float mutationRate = config.getMutationRate();
		PlayerSkeleton.MapReduce mapReduce = new PlayerSkeleton.MapReduce(config.getForkJoinPool());
		PlayerSkeleton.MapFunc<T, ChromosomeFitnessPair<T>> fitnessFunction = new FitnessFunction<T>(problemDomain);

		//Create a population
		ArrayList<T> population = new ArrayList<T>();
		for(int i = 0; i < populationSize; ++i) {
			population.add(problemDomain.newRandomChromosome());
		}

		do {
			//Evaluate fitness of population
			problemDomain.beginGeneration();
			mapReduce.map(fitnessFunction, population, fitnessResults);

			//Create next generation
			ArrayList<T> nextGeneration = new ArrayList<T>();
			//Calculate sum of fitness for roulette-based selection
			float totalFitness = 0.0f;
			for(ChromosomeFitnessPair<T> result: fitnessResults) {
				totalFitness += result.getFitness();
			}

			//Keep creating offspring until we have a full new population
			while(nextGeneration.size() < populationSize) {
				T parent1 = pickRandom(random, totalFitness, fitnessResults);
				T parent2 = pickRandom(random, totalFitness, fitnessResults);

				if(random.nextFloat() < crossoverRate) {//cross over happens
					int crossoverPoint = random.nextInt(parent1.getNumGenes());
					T[] children = problemDomain.crossover(parent1, parent2, crossoverPoint);
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
			for(T chromosome: nextGeneration) {
				for(int geneIndex = 0; geneIndex < chromosome.getNumGenes(); ++geneIndex) {
					if(random.nextFloat() < mutationRate) {//mutation happens
						problemDomain.mutate(chromosome, geneIndex);
					}
				}
			}
		} while(problemDomain.endGeneration(fitnessResults));

		//Find the best chromosome
		ChromosomeFitnessPair<T> bestChromosome = null;
		float maxScore = -Float.MAX_VALUE;
		for(ChromosomeFitnessPair<T> pair: fitnessResults) {
			float score = pair.getFitness();
			if(score > maxScore) {
				maxScore = score;
				bestChromosome = pair;
			}
		}
		return bestChromosome;
	}

	private static <T extends Chromosome> T pickRandom(
			Random random, float totalFitness, ArrayList<ChromosomeFitnessPair<T>> fitnessResults) {
		float decision = random.nextFloat() * totalFitness;
		for(ChromosomeFitnessPair<T> result: fitnessResults) {
			float fitness = result.getFitness();

			if(decision < fitness) {
				return result.getChromosome();
			}
			else {
				decision -= fitness;
			}
		}

		return fitnessResults.get(0).getChromosome();
	}

	private static class FitnessFunction<T extends Chromosome> implements PlayerSkeleton.MapFunc<T, ChromosomeFitnessPair<T>> {
		public FitnessFunction(ProblemDomain<T> problemDomain) {
			this.problemDomain = problemDomain;
		}

		@Override
		public ChromosomeFitnessPair<T> map(T chromosome) {
			return new ChromosomeFitnessPair<T>(chromosome, problemDomain.evaluateFitness(chromosome));
		}

		private ProblemDomain<T> problemDomain;
	}
}
