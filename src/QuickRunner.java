import java.util.concurrent.ForkJoinPool;

public class QuickRunner {
	public static void main(String[] args) {
		State s = new State();

		ForkJoinPool forkJoinPool = new ForkJoinPool();
		PlayerSkeleton p = new PlayerSkeleton(forkJoinPool);

		try {
			while(!s.hasLost()) {
				s.makeMove(p.pickMove(s,s.legalMoves()));
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			forkJoinPool.shutdown();
		}

		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
}
