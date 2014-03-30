import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class QuickRunner {
	public static void main(String[] args) {
		State s = new State();

		int numProcessors = Runtime.getRuntime().availableProcessors();
		LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		ExecutorService executorService = new ThreadPoolExecutor(numProcessors, numProcessors, 0, TimeUnit.MILLISECONDS, queue);
		PlayerSkeleton p = new PlayerSkeleton(executorService);

		try {
			while(!s.hasLost()) {
				s.makeMove(p.pickMove(s,s.legalMoves()));
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			executorService.shutdown();
		}

		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
}
