public class TimeEntry {
	final Profile p;
	final int seconds;
	final String difficulty, time;

	public TimeEntry(Profile p, int seconds, String difficulty) {
		this.p = p;
		this.seconds = seconds;
		this.difficulty = difficulty;
		
		int minutes = seconds / 60;
		time = String.format("%d:%02d", minutes, seconds % 60);
	}
}
