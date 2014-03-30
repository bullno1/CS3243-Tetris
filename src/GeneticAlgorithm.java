import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


/**
 * Trains and optimizes weights for features
 */
public class GeneticAlgorithm {
	public static <T extends Gene> SortedSet<GeneFitnessPair<T>> run(
			ProblemDomain<T> problemDomain, GeneticAlgorithmConfig config
			) throws InterruptedException, ExecutionException {

		//Initialize
        Random random = new Random();
        ArrayList<GeneFitnessPair<T>> fitnessResults = new ArrayList<GeneFitnessPair<T>>();
        ArrayList<Callable<GeneFitnessPair<T>>> fitnessFunctions = new ArrayList<Callable<GeneFitnessPair<T>>>();
		int populationSize = config.getPopulationSize();
		float crossoverRate = config.getCrossoverRate();
		float mutationRate = config.getMutationRate();
		ExecutorService executorService = config.getExecutorService();

		//Create a population
		ArrayList<T> population = new ArrayList<T>();
		for(int i = 0; i < populationSize; ++i) {
			population.add(problemDomain.newRandomGene());
		}

		while(!problemDomain.canTerminate(population)) {
			//Evaluate fitness of population
			fitnessFunctions.clear();
			for(T gene: population) {
				fitnessFunctions.add(new FitnessFunction<T>(gene, problemDomain));
			}
			Collection<Future<GeneFitnessPair<T>>> fitnessResultFutures = executorService.invokeAll(fitnessFunctions);
			fitnessResults.clear();
			for(Future<GeneFitnessPair<T>> future: fitnessResultFutures) {
				fitnessResults.add(future.get());
			}

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
		}

		TreeSet<GeneFitnessPair<T>> returnValue = new TreeSet<GeneFitnessPair<T>>(FITNESS_COMPARATOR);
		returnValue.addAll(fitnessResults);
		return returnValue;
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

	private static final Comparator<GeneFitnessPair<?>> FITNESS_COMPARATOR = new Comparator<GeneFitnessPair<?>>() {
		@Override
		public int compare(GeneFitnessPair<?> lhs, GeneFitnessPair<?> rhs) {
			return Float.compare(lhs.getFitness(), rhs.getFitness());
		}
	};

	private static class FitnessFunction<T extends Gene> implements Callable<GeneFitnessPair<T>> {
		public FitnessFunction(T gene, ProblemDomain<T> problemDomain) {
			this.gene = gene;
			this.problemDomain = problemDomain;
		}

		@Override
		public GeneFitnessPair<T> call() throws Exception {
			return new GeneFitnessPair<T>(gene, problemDomain.evaluateFitness(gene));
		}

		private T gene;
		private ProblemDomain<T> problemDomain;
	}
}
