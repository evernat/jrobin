/* ============================================================
 * JRobin : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.sourceforge.net/projects/jrobin
 * Project Lead:  Sasa Markovic (saxon@eunet.yu);
 *
 * (C) Copyright 2003, by Sasa Markovic.
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

package jrobin.core;

import java.io.IOException;

/**
 * Class to represent fetch request. For the complete explanation of all
 * fetch parameters consult RRDTool's
 * <a href="../../../man/rrdfetch.html" target="man">rrdfetch man page</a>.
 *
 * You cannot create <code>FetchRequest</code> directly (no public constructor
 * is provided). Use {@link jrobin.core.RrdDb#createFetchRequest(java.lang.String, long, long, long)
 * createFetchRequest()} method of your {@link jrobin.core.RrdDb RrdDb} object.
 *
 * @author <a href="mailto:saxon@eunet.yu">Sasa Markovic</a>
 */
public class FetchRequest {
	private RrdDb parentDb;
	private String consolFun;
	private long fetchStart;
	private long fetchEnd;
	private long resolution;

	FetchRequest(RrdDb parentDb, String consolFun, long fetchStart, long fetchEnd,
		long resolution) throws RrdException {
		this.parentDb = parentDb;
		this.consolFun = consolFun;
		this.fetchStart = fetchStart;
		this.fetchEnd = fetchEnd;
		this.resolution = resolution;
		validate();
	}

	/**
	 * Returns consolitation function to be used during the fetch process.
	 * @return Consolidation function.
	 */
	public String getConsolFun() {
		return consolFun;
	}

	/**
	 * Returns starting timestamp to be used for the fetch request.
	 * @return Starting timstamp in seconds.
	 */
	public long getFetchStart() {
		return fetchStart;
	}

	/**
	 * Returns ending timestamp to be used for the fetch request.
	 * @return Ending timestamp in seconds.
	 */
	public long getFetchEnd() {
		return fetchEnd;
	}

	/**
	 * Returns fetch resolution to be used for the fetch request.
	 * @return Fetch resolution in seconds.
	 */
	public long getResolution() {
		return resolution;
	}

	private void validate() throws RrdException {
		if(!ArcDef.isValidConsolFun(consolFun)) {
			throw new RrdException("Invalid consolidation function in fetch request: " + consolFun);
		}
		if(fetchStart < 0) {
			throw new RrdException("Invalid start time in fetch request: " + fetchStart);
		}
		if(fetchEnd < 0) {
			throw new RrdException("Invalid end time in fetch request: " + fetchEnd);
		}
		if(fetchStart >= fetchEnd) {
			throw new RrdException("Invalid start/end time in fetch request: " + fetchStart +
				"/" + fetchEnd);
		}
		if(resolution <= 0) {
			throw new RrdException("Invalid resolution in fetch request: " + resolution);
		}
	}

	/**
	 * Dumps the content of fetch request using the syntax of RRDTool's fetch command.
	 * @return Fetch request dump.
	 */
	public String dump() {
		return RrdDb.RRDTOOL + " fetch " + parentDb.getRrdFile().getFilePath() +
			" " + consolFun + " --start " + fetchStart + " --end " + fetchEnd +
			(resolution > 1? " --resolution " + resolution: "");
	}

	String getRrdToolCommand() {
		return dump();
	}

	/**
	 * Returns data from RRD file. Returned data consists of many fetch points (see
	 * {@link jrobin.core.FetchPoint FetchPoint}), and each fetch point represents
	 * RRD datasource values for the specific timestamp. Timestamp difference between
	 * consecutive fecth points is guaranteed to be constant.
	 * @return Array of fetch points.
	 * @throws RrdException Thrown in case of JRobin specific error.
	 * @throws IOException Thrown in case of I/O error.
	 */
	public FetchPoint[] fetch() throws RrdException, IOException {
		return parentDb.fetch(this);
	}

}