
public interface ProblemDomain<T extends Chromosome> {
	public T newRandomChromosome();
	public boolean canTerminate(Iterable<ChromosomeFitnessPair<T>> population);
	public float evaluateFitness(T chromosome);
	public void mutate(T gene, int mutatedGeneIndex);
	public T[] crossover(T parent1, T parent2, int crossoverPoint);
}
