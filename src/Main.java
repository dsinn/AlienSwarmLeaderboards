import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Main extends DefaultHandler {
	private String saxTemp;

	final public static String site = "http://steamcommunity.com/";
	final public static String appendix = "?xml=1";
	final public static String[] maps = { "asi-jac1-landingbay_01",
			"asi-jac1-landingbay_02", "asi-jac2-deima", "asi-jac3-rydberg",
			"asi-jac4-residential", "asi-jac6-sewerjunction",
			"asi-jac7-timorstation" };
	final public static Properties mapNames = getMapNames();
	final public static String fieldEnd = ".time.best.";

	private SAXParser sp = null;
	private int iMap = -1, bestTime = -1;
	private String curField = null, curDiff = null, fastestDiff = null;
	private Profile curProfile = null;
	private boolean useNextValue = false, atExperience = false;
	private List<TimeEntry>[] times;

	public static void main(String[] args) {
		final Scanner sn = new Scanner(System.in);
		System.out
				.println("To select a friends list, enter \"1\" (without quotes).");
		System.out
				.println("To select a Steam group members list, enter anything else.");
		System.out.print("Input: ");
		final String mode = sn.nextLine().trim();

		System.out.println();
		System.out.print("Enter Steam ID: ");
		final String input = sn.nextLine().trim();
		final List<TimeEntry>[] times = new Main().getLeaders(input,
				mode.equals("1"));

		if (times == null) {
			return;
		}
		outputAscii(times, input);
		outputHtml("AswLeaders.htm", times, input);
		System.exit(0);
	}

	/**
	 * 
	 * @param id
	 * @param friendsMode
	 *            True if
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<TimeEntry>[] getLeaders(String id, boolean friendsMode) {
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			sp = factory.newSAXParser();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			se.printStackTrace();
		}
		final Profile myProfile = new Profile(id);
		final List<Profile> players;
		if (friendsMode) {
			players = new PlayerListHandler().getFriends(myProfile);
		} else {
			players = new PlayerListHandler().getGroupMembers(id);
		}

		if (players != null) {
			times = new ArrayList[maps.length];
			for (int i = 0; i < times.length; i++) {
				times[i] = new ArrayList<TimeEntry>();
			}

			if (friendsMode) {
				try {
					parse(myProfile);
				} catch (IOException ioe) {
					ioe.printStackTrace();
				} catch (SAXException se) {
					System.out.println(se);
				}
			}

			for (final Profile p : players) {
				try {
					parse(p);
				} catch (IOException ioe) {
					ioe.printStackTrace();
				} catch (SAXException se) {
					System.out.println(se);
				}
			}
		}
		return times;
	}

	private void parse(Profile p) throws IOException, SAXException {
		changeMap(true);
		atExperience = false;
		curProfile = p;
		sp.parse(site + p.folder + p.id + "/statsfeed/630", this);
	}

	private void changeMap(boolean reset) throws SAXException {
		if (reset) {
			iMap = 0;
		} else {
			iMap++;
		}
		if (iMap < maps.length) {
			bestTime = Integer.MAX_VALUE;
			fastestDiff = null;
			curField = maps[iMap] + fieldEnd;
			useNextValue = false;
		} else {
			throw new SAXException("Collected times from "
					+ curProfile.getName());
		}
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		saxTemp = new String(ch, start, length);
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equalsIgnoreCase("APIName")) {
			if (saxTemp.startsWith(curField)) {
				final String difficulty = saxTemp.substring(curField.length());
				if (difficulty.equalsIgnoreCase("normal")
						|| difficulty.equalsIgnoreCase("hard")
						|| difficulty.equalsIgnoreCase("insane")) {
					// "difficulty" and "easy" do not count towards
					// achievements
					curDiff = difficulty;
					useNextValue = true;
				}
			} else if (saxTemp.equalsIgnoreCase("experience")) {
				atExperience = true;
			}
		} else if (qName.equalsIgnoreCase("value")) {
			if (useNextValue) {
				useNextValue = false;
				int seconds = Integer.parseInt(saxTemp);
				if (seconds > 0) {
					// Player has completed the map at least once
					if (seconds < bestTime) {
						bestTime = seconds;
						fastestDiff = curDiff;
					}
				}
				if (curDiff.equalsIgnoreCase("insane")) {
					// Insane is the last difficulty
					if (bestTime != Integer.MAX_VALUE) {
						times[iMap].add(new TimeEntry(curProfile, bestTime,
								fastestDiff));
					}
					changeMap(false);
				}
			} else if (atExperience) {
				atExperience = false;
				if (saxTemp.equals("0")) {
					throw new SAXException(curProfile.id
							+ " has never completed a level");
				}
			}
		}
	}

	public static void outputAscii(List<TimeEntry>[] times, String myId) {
		for (int i = 0; i < maps.length; i++) {
			String mapName = mapNames.getProperty(maps[i]);
			if (mapName == null) {
				mapName = maps[i];
			}
			System.out.printf("%n== %s ==%n", mapName);
			Collections.sort(times[i], new TimeEntryComparator());
			for (final TimeEntry te : times[i]) {
				System.out.printf("%-32s - %5s (%s)", te.p.getName(), te.time,
						te.difficulty);
				if (myId.equals(te.p.id)) {
					System.out.print(" *");
				}
				System.out.println();
			}
		}
		System.out.printf("%nData collected on %tc.",
				System.currentTimeMillis());
	}

	public static void outputHtml(String path, List<TimeEntry>[] times,
			String myId) {
		try {
			final File file = new File(path);
			final FileOutputStream fso = new FileOutputStream(file);
			final OutputStreamWriter osw = new OutputStreamWriter(fso, "UTF8");
			final BufferedWriter bw = new BufferedWriter(osw);

			bw.write(copypaste("start.txt"));
			for (int i = 0; i < maps.length; i++) {
				String mapName = mapNames.getProperty(maps[i]);
				if (mapName == null) {
					mapName = maps[i];
				}
				bw.write("<h2>" + mapName + "</h2>");
				bw.newLine();
				bw.write(copypaste("tableStart.txt"));
				Collections.sort(times[i], new TimeEntryComparator());
				for (final TimeEntry te : times[i]) {
					bw.write("<tr>");
					bw.newLine();
					bw.write(String.format(
							"\t<td class=\"avatar\"><img src=\"%s\" /></td>",
							te.p.getImgsrc()));
					bw.write("\t<td class=\"name\">");
					final String profileUrl = Main.site + te.p.folder + te.p.id;
					String profileClass;
					if (myId.equals(te.p.id)) {
						profileClass = " class=\"you\"";
					} else {
						profileClass = "";
					}
					bw.write(String.format("<a%s href=\"%s\">%s</a>",
							profileClass, profileUrl, te.p.getName()));

					bw.write(String
							.format(" (<a href=\"%s\">stats</a>)",
									profileUrl
											+ "/stats/AlienSwarm?tab=stats&subtab=missions"));
					bw.write("</td>");
					bw.newLine();
					bw.write("<td class=\"time\">" + te.time + "</td>");
					bw.newLine();
					bw.write("<td class=\"difficulty\">"
							+ Character.toUpperCase(te.difficulty.charAt(0))
							+ te.difficulty.substring(1) + "</td>");
					bw.newLine();
					bw.write("</tr>");
					bw.newLine();
				}
				bw.write("</table>");
				bw.newLine();
			}
			bw.write(String.format("<br />Tables generated on %tc.",
					System.currentTimeMillis()));
			bw.write(copypaste("end.txt"));
			bw.close();
			java.awt.Desktop.getDesktop().browse(file.toURI());
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Returns all of the text in a file.
	 * 
	 * @param path
	 *            the path of the file
	 * @return all of the text in a file
	 */
	protected static String copypaste(String path) {
		String output = "";
		final Scanner sn = new Scanner(ClassLoader.getSystemClassLoader()
				.getResourceAsStream(path));
		while (sn.hasNextLine()) {
			output += String.format("%s%n", sn.nextLine());
		}
		return output;
	}

	/**
	 * Loads the map key-value pairs from a file inside the JAR.
	 */
	private static Properties getMapNames() {
		final Properties ppt = new Properties();
		try {
			ppt.load(ClassLoader.getSystemClassLoader().getResourceAsStream(
					"mapNames.txt"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ppt;
	}
}
