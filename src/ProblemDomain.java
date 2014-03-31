
public interface ProblemDomain<T extends Gene> {
	public T newRandomGene();
	public boolean canTerminate(Iterable<GeneFitnessPair<T>> population);
	public float evaluateFitness(T gene);
	public void mutate(T gene, int mutatedChromosomeIndex);
	public T[] crossover(T parent1, T parent2, int crossOverPoint);
}
