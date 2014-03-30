
public class GeneFitnessPair<T extends Gene> {
	public GeneFitnessPair(T gene, float fitness) {
		this.gene = gene;
		this.fitness = fitness;
	}

	public float getFitness() {
		return fitness;
	}

	public T getGene() {
		return gene;
	}

	private final T gene;
	private final float fitness;
}