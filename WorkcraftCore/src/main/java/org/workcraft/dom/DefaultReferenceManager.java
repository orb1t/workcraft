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

package org.workcraft.dom;

import java.util.List;

import org.workcraft.dependencymanager.advanced.core.GlobalCache;
import org.workcraft.dom.references.IDGenerator;
import org.workcraft.dom.references.ReferenceManager;
import org.workcraft.observation.HierarchySupervisor;
import org.workcraft.util.TwoWayMap;

public class DefaultReferenceManager extends HierarchySupervisor implements ReferenceManager {
	public DefaultReferenceManager(Node root) {
		super(root);
		start();
	}

	private IDGenerator idGenerator = new IDGenerator();

	private TwoWayMap<String, Node> nodes = new TwoWayMap<String, Node>();

	@Override
	public void handleEvent(List<Node> added, List<Node> removed) {
		for (Node n : added)
			nodeAdded(n);
		for (Node n: removed)
			nodeRemoved(n);
	}

	private void nodeRemoved(Node n) {
		nodes.removeValue(n);
		
		for (Node nn: GlobalCache.eval(n.children()))
			nodeRemoved(nn);
	}

	private void nodeAdded(Node n) {
		String id = Integer.toString(idGenerator.getNextID());
		nodes.put(id, n);
		
		for (Node nn : GlobalCache.eval(n.children()))
			nodeAdded(nn);
	}

	@Override
	public Node getNodeByReference(String reference) {
		return nodes.getValue(reference);
	}

	@Override
	public String getNodeReference(Node node) {
		return nodes.getKey(node);
	}
}