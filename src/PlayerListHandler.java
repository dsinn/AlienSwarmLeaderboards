import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PlayerListHandler extends DefaultHandler {
	String saxTemp = null;
	List<Profile> players = null;
	private String tag;

	public List<Profile> getFriends(Profile p) {
		tag = "friend";
		players = new ArrayList<Profile>();
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser sp = factory.newSAXParser();

			sp.parse(Main.site + p.folder + p.id + "/friends" + Main.appendix,
					this);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			System.out.println(se);
		}
		return players;
	}

	public List<Profile> getGroupMembers(String input) {
		tag = "steamID64";
		String folder = "groups";
		if (input.startsWith("1035827914") && input.length() == 18) {
			try {
				Long.parseLong(input.substring(10));
				folder = "gid";
			} catch (NumberFormatException nfe) {
			}
		}

		players = new ArrayList<Profile>();
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser sp = factory.newSAXParser();
			sp.parse(Main.site + folder + "/" + input + "/memberslistxml", this);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			System.out.println(se);
		}
		return players;
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		saxTemp = new String(ch, start, length);
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equalsIgnoreCase(tag)) {
			players.add(new Profile(saxTemp));
		} else if (qName.equalsIgnoreCase("error")) {
			players = null;
			throw new SAXException(
					"Profile is non-existent, private, or friends-only.");
		}
	}
}
