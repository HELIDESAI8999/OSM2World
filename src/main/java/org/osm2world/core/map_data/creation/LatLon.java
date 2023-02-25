package org.osm2world.core.map_data.creation;

import org.osm2world.console.LatLonEle;

import java.util.Objects;

/**
 * an immutable coordinate pair with latitude and longitude
 */
public class LatLon {

	public final double lat;
	public final double lon;

	// typographical minus '−' works around the CLI parser's special handling of '-'
	public static final String DOUBLE_PATTERN = "[+-−]?\\d+(?:\\.\\d+)?";

	/** pattern for parseable arguments */
	public static final String PATTERN = "("+DOUBLE_PATTERN+"),("+DOUBLE_PATTERN+")";

	public LatLon(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	/** parsing constructor for strings matching {@link #PATTERN} */
	public LatLon(String string) {
		LatLonEle lle = new LatLonEle(string);
		this.lat = lle.lat;
		this.lon = lle.lon;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LatLon latLon = (LatLon) o;
		return Double.compare(latLon.lat, lat) == 0 && Double.compare(latLon.lon, lon) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lat, lon);
	}

	@Override
	public String toString() {
		return "(" + lat + ", " + lon + ")";
	}

}
