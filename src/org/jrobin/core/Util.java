/* ============================================================
 * JRobin : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.jrobin.org
 * Project Lead:  Sasa Markovic (saxon@jrobin.org);
 *
 * (C) Copyright 2003, by Sasa Markovic.
 *
 * Developers:    Sasa Markovic (saxon@jrobin.org)
 *                Arne Vandamme (cobralord@jrobin.org)
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package org.jrobin.core;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.io.*;

/**
 * Class defines various utility functions used in JRobin. 
 *
 * @author <a href="mailto:saxon@jrobin.org">Sasa Markovic</a>
 */
public class Util {
	
	// pattern RRDTool uses to format doubles in XML files
	static final String PATTERN = "0.0000000000E00";
	// directory under $USER_HOME used for demo graphs storing
	static final String JROBIN_DIR = "jrobin-demo";

	static final DecimalFormat df;
	static {
		df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
		df.applyPattern(PATTERN);
		df.setPositivePrefix("+");
	}

	/**
	 * Returns current timestamp in seconds (without milliseconds). Returned timestamp
	 * is obtained with the following expression:
	 *
	 * <code>(System.currentTimeMillis() + 500L) / 1000L</code>
	 * @return Current timestamp
	 */
	public static long getTime() {
		return (System.currentTimeMillis() + 500L) / 1000L;
	}

	static long normalize(long timestamp, long step) {
		return timestamp - timestamp % step;
	}

	/**
	 * Returns the greater of two double values, but treats NaN as the smallest possible
	 * value. Note that <code>Math.max()</code> behaves differently for NaN arguments.
	 *
	 * @param x an argument
	 * @param y another argument
	 * @return the lager of arguments
	 */
	public static double max(double x, double y) {
		return Double.isNaN(x)? y: Double.isNaN(y)? x: Math.max(x, y);
	}

	/**
	 * Returns the smaller of two double values, but treats NaN as the greatest possible
	 * value. Note that <code>Math.min()</code> behaves differently for NaN arguments.
	 *
	 * @param x an argument
	 * @param y another argument
	 * @return the smaller of arguments
	 */
	public static double min(double x, double y) {
		return Double.isNaN(x)? y: Double.isNaN(y)? x: Math.min(x, y);
	}

	static double sum(double x, double y) {
		return Double.isNaN(x)? y: Double.isNaN(y)? x: x + y;
	}

	static String formatDouble(double x, String nanString, boolean forceExponents) {
		if(Double.isNaN(x)) {
			return nanString;
		}
		if(forceExponents) {
			return df.format(x);
		}
		return "" + x;
	}

	static String formatDouble(double x, boolean forceExponents) {
		return formatDouble(x, "" + Double.NaN, forceExponents);
	}

	static void debug(String message) {
		if(RrdDb.DEBUG) {
			System.out.println(message);
		}
	}

	/**
	 * Returns <code>Date</code> object for the given timestamp (in seconds, without
	 * milliseconds)
	 * @param timestamp Timestamp in seconds.
	 * @return Corresponding Date object.
	 */
	public static Date getDate(long timestamp) {
		return new Date(timestamp * 1000L);
	}

	/**
	 * Returns timestamp (unix epoch) for the given Date object
	 * @param date Date object
	 * @return Corresponding timestamp (without milliseconds)
	 */
	public static long getTimestamp(Date date) {
		return (date.getTime() + 500L) / 1000L;
	}

	/**
	 * Returns timestamp (unix epoch) for the given GregorianCalendar object
	 * @param gc GregorianCalendar object
	 * @return Corresponding timestamp (without milliseconds)
	 */
	public static long getTimestamp(GregorianCalendar gc) {
		return getTimestamp(gc.getTime());
	}

	/**
	 * Returns timestamp (unix epoch) for the given year, month, day, hour and minute.
	 * @param year Year
	 * @param month Month (zero-based)
	 * @param day Day in month
	 * @param hour Hour
	 * @param min Minute
	 * @return Corresponding timestamp
	 */
	public static long getTimestamp(int year, int month, int day, int hour, int min) {
		GregorianCalendar gc = new GregorianCalendar(year, month, day, hour, min);
		return Util.getTimestamp(gc);
	}

	/**
	 * Returns timestamp (unix epoch) for the given year, month and day.
	 * @param year Year
	 * @param month Month (zero-based)
	 * @param day Day in month
	 * @return Corresponding timestamp
	 */
	public static long getTimestamp(int year, int month, int day) {
		return Util.getTimestamp(year, month, day, 0, 0);
	}

	static double parseDouble(String valueStr) {
		double value;
		try {
			value = Double.parseDouble(valueStr);
		}
		catch(NumberFormatException nfe) {
			value = Double.NaN;
		}
		return value;
	}

	private static final File homeDirFile;
	private static final String homeDirPath;

	static {
		String delim = System.getProperty("file.separator");
		homeDirPath = System.getProperty("user.home") + delim + JROBIN_DIR + delim;
		homeDirFile = new File(homeDirPath);
	}

	/**
	 * Returns path to directory used for placement of JRobin demo graphs and creates it
	 * if necessary.
	 * @return Path to demo directory (defaults to $HOME/jrobin/) if directory exists or
	 * was successfully created. Null if such directory could not be created.
	 */
	public static String getJRobinDemoDirectory() {
		return (homeDirFile.exists() || homeDirFile.mkdirs())? homeDirPath: null;
	}

	/**
	 * Returns full path to the file stored in the demo directory of JRobin
	 * @param filename Partial path to the file stored in the demo directory of JRobin
	 * (just name and extension, without parent directories)
	 * @return Full path to the file
	 */
	public static String getJRobinDemoPath(String filename) {
		String demoDir = getJRobinDemoDirectory();
		if(demoDir != null) {
			return demoDir + filename;
		}
		else {
			return null;
		}
	}

	static boolean sameFilePath(String path1, String path2) throws IOException {
		File file1 = new File(path1);
		File file2 = new File(path2);
		return file1.getCanonicalPath().equals(file2.getCanonicalPath());
	}

	static int getMatchingDatasourceIndex(RrdDb rrd1, int dsIndex, RrdDb rrd2) {
		String dsName = rrd1.getDatasource(dsIndex).getDsName();
		try {
			return rrd2.getDsIndex(dsName);
		} catch (RrdException e) {
			return -1;
		}
	}

	static int getMatchingArchiveIndex(RrdDb rrd1, int arcIndex, RrdDb rrd2)
		throws IOException {
		Archive archive = rrd1.getArchive(arcIndex);
		String consolFun = archive.getConsolFun();
		int steps = archive.getSteps();
		try {
			return rrd2.getArcIndex(consolFun, steps);
		} catch (RrdException e) {
			return -1;
		}
	}

	static String getTmpFilename() throws IOException {
		return File.createTempFile("JROBIN_", ".tmp").getCanonicalPath();
	}

	static class Xml {
		static Node[] getChildNodes(Node parentNode, String childName) {
			ArrayList nodes = new ArrayList();
			NodeList nodeList = parentNode.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (node.getNodeName().equals(childName)) {
					nodes.add(node);
				}
			}
			return (Node[]) nodes.toArray(new Node[0]);
		}

		static Node getFirstChildNode(Node parentNode, String childName) throws RrdException {
			Node[] childs = getChildNodes(parentNode, childName);
			if (childs.length > 0) {
				return childs[0];
			}
			throw new RrdException("XML Error, no such child: " + childName);
		}

		static String getChildValue(Node parentNode, String childName) throws RrdException {
			NodeList children = parentNode.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeName().equals(childName)) {
					return child.getFirstChild().getNodeValue().trim();
				}
			}
			throw new RrdException("XML Error, no such child: " + childName);
		}

		static int getChildValueAsInt(Node parentNode, String childName) throws RrdException {
			String valueStr = getChildValue(parentNode, childName);
			return Integer.parseInt(valueStr);
		}

		static long getChildValueAsLong(Node parentNode, String childName) throws RrdException {
			String valueStr = getChildValue(parentNode, childName);
			return Long.parseLong(valueStr);
		}

		static double getChildValueAsDouble(Node parentNode, String childName) throws RrdException {
			String valueStr = getChildValue(parentNode, childName);
			return Util.parseDouble(valueStr);
		}

		static Element getRootElement(InputSource inputSource) throws RrdException, IOException	{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(false);
			try {
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.parse(inputSource);
				return doc.getDocumentElement();
			} catch (ParserConfigurationException e) {
				throw new RrdException(e);
			} catch (SAXException e) {
				throw new RrdException(e);
			}
		}

		static Element getRootElement(String xmlString)	throws RrdException, IOException {
			return getRootElement(new InputSource(new StringReader(xmlString)));
		}

		static Element getRootElement(File xmlFile)	throws RrdException, IOException {
			Reader reader = null;
			try {
				reader = new FileReader(xmlFile);
				return getRootElement(new InputSource(reader));
			}
			finally {
				if(reader != null) {
					reader.close();
				}
			}
		}
	}
}


