
public class WeightSet implements Chromosome {
	public WeightSet(float[] weights) {
		this.weights = weights;
	}

	@Override
	public int getNumGenes() {
		return weights.length;
	}

	public float[] getWeights() {
		return weights;
	}

	float[] weights;
}