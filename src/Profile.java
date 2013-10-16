import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Profile extends DefaultHandler {
	final public static String start64 = "7656119";

	private String saxTemp;

	final String id, folder;
	private String name, imgsrc;

	public Profile(String input) {
		this.id = input;
		folder = (is64ID(id) ? "profiles" : "id") + "/";
		name = null;
		imgsrc = "http://media.steampowered.com/steamcommunity/public/images/avatars/fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb.jpg";
	}

	public String getName() {
		if (name == null) {
			fetchData();
		}
		return name;
	}

	public String getImgsrc() {
		if (name == null) {
			fetchData();
		}
		return imgsrc;
	}

	private void fetchData() {
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser sp = factory.newSAXParser();
			sp.parse(Main.site + folder + id + Main.appendix, this);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
		}
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		saxTemp = new String(ch, start, length);
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equalsIgnoreCase("steamID") && saxTemp.trim().length() > 0) {
			name = saxTemp;
		} else if (qName.equalsIgnoreCase("avatarIcon")) {
			imgsrc = saxTemp;
			throw new SAXException("Collected data from " + name);
		}
	}

	public static boolean is64ID(String input) {
		if (input.startsWith(start64) && input.length() == 17) {
			try {
				Long.parseLong(input.substring(start64.length()));
				return true;
			} catch (NumberFormatException nfe) {
			}
		}
		return false;
	}
}
