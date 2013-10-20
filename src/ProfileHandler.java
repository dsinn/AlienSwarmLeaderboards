import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ProfileHandler extends DefaultHandler {
	String saxTemp;

	private String name, imgsrc;

	/**
	 * Returns the name, privacy state, and 32px image URL of a Steam profile.
	 * 
	 * @param id
	 *            a player's 64-bit Steam ID
	 * @return a String array of size 3 with the name, privacy state, and img
	 *         src in order
	 */
	protected String[] getInfo(String id) {
		imgsrc = "http://media.steampowered.com/steamcommunity/public/images/avatars/fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb.jpg";
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser sp = factory.newSAXParser();
			sp.parse(Main.site + "profiles/" + id + Main.appendix, this);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			// System.out.println(se);
		}
		return new String[] { name, imgsrc };
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		saxTemp = new String(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("steamID") && saxTemp.trim().length() > 0) {
			name = saxTemp;
		} else if (qName.equalsIgnoreCase("avatarIcon")) {
			imgsrc = saxTemp;
			throw new SAXException("Collected profile data from " + name);
		}
	}
}
