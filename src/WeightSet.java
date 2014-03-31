
public class WeightSet implements Gene {
	public WeightSet(float[] weights) {
		this.weights = weights;
	}

	@Override
	public int getNumChromosomes() {
		return weights.length;
	}

	public float[] getWeights() {
		return weights;
	}

	float[] weights;
}