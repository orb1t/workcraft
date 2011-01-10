/*
*
* Copyright 2008,2009 Newcastle University
*
* This file is part of Workcraft.
* 
* Workcraft is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Workcraft is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Workcraft.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package org.workcraft.dom.visual.connections;

import java.awt.Color;
import java.awt.Stroke;

import org.workcraft.dom.visual.Touchable;
import org.workcraft.dom.visual.connections.VisualConnection.ScaleMode;

public interface VisualConnectionProperties {
	public Color getDrawColor();
	public double getArrowWidth();
	public double getArrowLength();
	public boolean hasArrow();
	public Stroke getStroke();
	
	public Touchable getFirstShape();
	public Touchable getSecondShape();
	public ScaleMode getScaleMode();
	
	class Inheriting implements VisualConnectionProperties {
		private final VisualConnectionProperties target;

		public Inheriting(VisualConnectionProperties target) {
			this.target = target;
		}

		public Color getDrawColor() {
			return target.getDrawColor();
		}

		public double getArrowWidth() {
			return target.getArrowWidth();
		}

		public double getArrowLength() {
			return target.getArrowLength();
		}

		public boolean hasArrow() {
			return target.hasArrow();
		}

		public Stroke getStroke() {
			return target.getStroke();
		}

		public Touchable getFirstShape() {
			return target.getFirstShape();
		}

		public Touchable getSecondShape() {
			return target.getSecondShape();
		}

		public ScaleMode getScaleMode() {
			return target.getScaleMode();
		}

	}
}