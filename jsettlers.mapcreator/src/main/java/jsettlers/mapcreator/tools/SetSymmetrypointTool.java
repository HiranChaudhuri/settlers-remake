/*******************************************************************************
 * Copyright (c) 2015
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.mapcreator.tools;

import jsettlers.common.position.ShortPoint2D;
import jsettlers.mapcreator.data.MapData;
import jsettlers.mapcreator.data.symmetry.SymmetryConfig;
import jsettlers.mapcreator.main.window.sidebar.ToolSettingsPanel;
import jsettlers.mapcreator.tools.shapes.EShapeType;
import jsettlers.mapcreator.tools.shapes.ShapeType;

/**
 * Set the startup position of a player
 *
 * @author Andreas Butti
 *
 */
public class SetSymmetrypointTool extends AbstractTool {

	/**
	 * Interface to get the currently selected player
	 */
	private final ToolSettingsPanel settings;

	/**
	 * Constructor
	 *
	 * @param settings The toolbar that manages the symmetry settings
	 */
	public SetSymmetrypointTool(ToolSettingsPanel settings) {
		super("sympoint");
		this.settings = settings;
		shapeTypes.add(EShapeType.POINT);
	}

	@Override
	public void start(MapData data, ShapeType shape, ShortPoint2D pos) {
		settings.setSymmetryPoint(pos);
	}

	@Override
	public void apply(MapData map, ShapeType shapeType, ShortPoint2D start, ShortPoint2D end, double uidx) {
	}

	@Override
	public void apply(MapData map, SymmetryConfig symmetry, ShapeType shapeType, ShortPoint2D start, ShortPoint2D end, double uidx) {
	}
}
