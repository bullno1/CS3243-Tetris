
public interface ProblemDomain<T extends Chromosome> {
	/**
	 * Creates a random chromosome
	 * @return a random chromosome
	 */
	public T newRandomChromosome();

	/**
	 * Called at the beginning of all generations
	 */
	public void beginGeneration();

	/**
	 * Called at the end of a generation
	 * @param currentGeneration current generation
	 * @return whether genetic algorithm should continue running
	 */
	public boolean endGeneration(Iterable<ChromosomeFitnessPair<T>> currentGeneration);

	/**
	 * Evaluates the fitness of a chromosome
	 * @param chromosome the chromosome to be evaluated
	 * @return fitness
	 */
	public float evaluateFitness(T chromosome);

	/**
	 * Mutate a chromosome
	 * @param chromosome the chromosome to be mutated
	 * @param mutatedGeneIndex the gene at which mutation happens
	 */
	public void mutate(T gene, int mutatedGeneIndex);

	/**
	 * Crossover two parents to create offsprings
	 * @param parent1 first parent
	 * @param parent2 second parent
	 * @param crossoverPoint the point at which crossover happens
	 * @return offsprings
	 */
	public T[] crossover(T parent1, T parent2, int crossoverPoint);
}
