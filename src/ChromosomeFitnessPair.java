
public class ChromosomeFitnessPair<T extends Chromosome> {
	public ChromosomeFitnessPair(T chromosome, float fitness) {
		this.chromosome = chromosome;
		this.fitness = fitness;
	}

	public float getFitness() {
		return fitness;
	}

	public T getChromosome() {
		return chromosome;
	}

	private final T chromosome;
	private final float fitness;
}