import java.util.Comparator;

public class TimeEntryComparator implements Comparator<TimeEntry> {
	public int compare(TimeEntry o1, TimeEntry o2) {
		return o1.seconds - o2.seconds;
	}
}
