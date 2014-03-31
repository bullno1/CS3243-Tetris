import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;

public class QuickRunner {
	public static void main(String[] args) {

		ForkJoinPool forkJoinPool = new ForkJoinPool();

		try {
			int sum = 0;
			ArrayList<Integer> scores = new ArrayList<Integer>();

			for(int i = 0; i < 100; ++i) {
				State s = new State();
				PlayerSkeleton p = new PlayerSkeleton(forkJoinPool);
				while(!s.hasLost()) {
					s.makeMove(p.pickMove(s,s.legalMoves()));
				}
				System.out.println(s.getRowsCleared());
				scores.add(s.getRowsCleared());
				sum += s.getRowsCleared();
			}

			float average = (float)sum / 100.0f;
			System.out.println("Average: " + average);
			
			float sumOfSquaredDiff = 0.0f;
			for(int score: scores) {
				float diff = (float)score - average;
				sumOfSquaredDiff += diff * diff;
			}
			float stddev = (float)Math.sqrt(sumOfSquaredDiff / 100.0f);
			System.out.println("Standard deviation: " + stddev);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			forkJoinPool.shutdown();
		}
	}
}
