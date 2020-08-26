package org.osm2world.core.world.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.*;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.util.ValueParseUtil;
import org.osm2world.core.world.attachment.AttachmentConnector;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;


/**
 * implementation of {@link AreaWorldObject} that offers some basic features:
 * < ul><li> providing the object outline based on the {@link MapArea}
 * </li><li> providing bounding geometry for intersection tests
 * </li><li> calculating a triangulation of the surface for rendering
 * </ul>
 */
public abstract class AbstractAreaWorldObject implements WorldObjectWithOutline, AreaWorldObject, BoundedObject {

	protected final MapArea area;

	private final PolygonWithHolesXZ outlinePolygonXZ;

	private final AttachmentConnector attachmentConnector;

	private EleConnectorGroup connectors;

	protected AbstractAreaWorldObject(MapArea area) {

		this.area = area;

		if (!area.getPolygon().getOuter().isClockwise()) {
			outlinePolygonXZ = area.getPolygon();
		} else {
			outlinePolygonXZ = new PolygonWithHolesXZ(
					area.getPolygon().getOuter().makeCounterclockwise(),
					area.getPolygon().getHoles());
		}

		List<String> types = new ArrayList<>();

		if (area.getTags().contains("location", "roof")) {
			if (area.getTags().containsKey("level")) {
				List<Integer> levels = ValueParseUtil.parseLevels(area.getTags().getValue("level"));
				if (levels != null) {
					types.add("roof" + levels.get(0));
				}
			}
			types.add("roof");
		} else if (area.getTags().containsKey("level")) {
			List<Integer> levels = ValueParseUtil.parseLevels(area.getTags().getValue("level"));
			if (levels != null) {
				types.add("floor" + levels.get(0));
			}
		}


		if (!types.isEmpty()) {
			VectorXYZ pos;
			if (area.getPolygon().contains(area.getOuterPolygon().getCentroid())) {
				pos = area.getOuterPolygon().getCentroid().xyz(0);
			} else {
				pos = area.getOuterPolygon().getClosestSegment(area.getOuterPolygon().getCentroid()).getCenter().xyz(0);
			}
			attachmentConnector = new AttachmentConnector(
					types,
					pos,
					this,
					0,
					false);
		} else {
			attachmentConnector = null;
		}

	}

	@Override
	public Iterable<AttachmentConnector> getAttachmentConnectors() {
		if (attachmentConnector == null) {
			return emptyList();
		} else {
			return singleton(attachmentConnector);
		}
	}

	public AttachmentConnector getAttachmentConnectorObjectIfAttached() {
		if (attachmentConnector != null) {
			if (attachmentConnector.isAttached()) {
				return attachmentConnector;
			}
		}
		return null;
	}

	@Override
	public GroundState getGroundState() {
		if (attachmentConnector != null) {
			return GroundState.ABOVE;
		} else {
			return GroundState.ON;
		}
	}

	@Override
	public EleConnectorGroup getEleConnectors() {

		if (connectors == null) {

			connectors = new EleConnectorGroup();

			connectors.addConnectorsForTriangulation(
					getTriangulationXZ(), null, getGroundState());

		}

		return connectors;

	}

	@Override
	public void defineEleConstraints(EleConstraintEnforcer enforcer) {}

	@Override
	public PolygonWithHolesXZ getOutlinePolygonXZ() {
		return outlinePolygonXZ;
	}

	@Override
	public PolygonXYZ getOutlinePolygon() {

		if (attachmentConnector != null) {
			if (attachmentConnector.isAttached()) {
				return outlinePolygonXZ.getOuter().xyz(attachmentConnector.getAttachedPos().getY());
			}
		}

		return connectors.getPosXYZ(outlinePolygonXZ.getOuter());
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return area.getOuterPolygon().boundingBox();
	}

	@Override
	public final MapArea getPrimaryMapElement() {
		return area;
	}

	/**
	 * decompose this area into counterclockwise triangles.
	 */
	protected Collection<TriangleXZ> getTriangulationXZ() {
		return TriangulationUtil.triangulate(area.getPolygon());
	}

	/**
	 * decompose this area into counterclockwise 3d triangles.
	 * Only available after elevation calculation.
	 */
	protected Collection<TriangleXYZ> getTriangulation() {

		if (attachmentConnector != null) {
			if (attachmentConnector.isAttached()) {
				return getTriangulationXZ().stream()
						.map(t -> t.makeCounterclockwise().xyz(attachmentConnector.getAttachedPos().getY() + 0.001))
						.collect(toList());
			}
		}

		return connectors.getTriangulationXYZ(getTriangulationXZ());
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + area + ")";
	}

}
