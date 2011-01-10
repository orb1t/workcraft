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

package org.workcraft.dom.visual;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.workcraft.NodeFactory;
import org.workcraft.annotations.MouseListeners;
import org.workcraft.dependencymanager.advanced.core.ExpressionBase;
import org.workcraft.dependencymanager.advanced.core.GlobalCache;
import org.workcraft.dependencymanager.advanced.user.CachedHashSet;
import org.workcraft.dom.AbstractModel;
import org.workcraft.dom.Container;
import org.workcraft.dom.DefaultMathNodeRemover;
import org.workcraft.dom.Node;
import org.workcraft.dom.math.MathConnection;
import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.math.MathNode;
import org.workcraft.dom.visual.connections.DefaultAnchorGenerator;
import org.workcraft.dom.visual.connections.Polyline;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.exceptions.NodeCreationException;
import org.workcraft.exceptions.PasteException;
import org.workcraft.gui.propertyeditor.Properties;
import org.workcraft.util.Hierarchy;
import org.workcraft.util.XmlUtil;

@MouseListeners ({ DefaultAnchorGenerator.class })
public abstract class AbstractVisualModel extends AbstractModel implements VisualModel {
	private MathModel mathModel;
	private Container currentLevel;
	private CachedHashSet<Node> selection = new CachedHashSet<Node>();
	private final ExpressionBase<HierarchicalGraphicalContent> graphicalContent;
	private SelectionEventPropagator selectionEventPropagator;

	public AbstractVisualModel(VisualGroup root) {
		this (null, root);
	}

	public AbstractVisualModel() {
		this ((MathModel)null);
	}

	public AbstractVisualModel(MathModel mathModel) {
		this(mathModel, null);
	}
	
	static class ConstructionInfo {
		public ConstructionInfo(MathModel mathModel, VisualGroup root) {
			this.mathModel = mathModel;
			this.root = root == null ? new VisualGroup() : root;
		}
		MathModel mathModel; 
		VisualGroup root;
	}

	public AbstractVisualModel(MathModel mathModel, VisualGroup root) {
		this(new ConstructionInfo(mathModel, root));
	}
	
	public AbstractVisualModel(ConstructionInfo param) {
		super(param.root);
		this.mathModel = param.mathModel;
		
		currentLevel =  param.root;
		selectionEventPropagator = new SelectionEventPropagator(param.root, this);
		new RemovedNodeDeselector(this);
		new DefaultMathNodeRemover(this, getRoot());
		graphicalContent = makeGraphicalContent();
	}

	protected final void createDefaultFlatStructure() throws NodeCreationException {
		HashMap <MathNode, VisualComponent> createdNodes = new HashMap <MathNode, VisualComponent>();
		HashMap <VisualConnection, MathConnection> createdConnections = new	HashMap <VisualConnection, MathConnection>();

		for (Node n : GlobalCache.eval(mathModel.getRoot().children())) {
			if (n instanceof MathConnection) {
				MathConnection connection = (MathConnection)n;

				// Will create incomplete instance, setConnection() needs to be called later to finalise.
				// This is to avoid cross-reference problems.
				VisualConnection visualConnection = NodeFactory.createVisualConnection(connection);
				createdConnections.put(visualConnection, connection);
			} else {
				MathNode node = (MathNode)n;
				VisualComponent visualComponent = (VisualComponent)NodeFactory.createVisualComponent(node);

				if (visualComponent != null) {
					getRoot().add(visualComponent);
					createdNodes.put(node, visualComponent);
				}
			}
		}

		for (VisualConnection vc : createdConnections.keySet()) {
			MathConnection mc = createdConnections.get(vc);
			vc.setVisualConnectionDependencies(createdNodes.get(mc.getFirst()), 
					createdNodes.get(mc.getSecond()), new Polyline(vc), mc);
			getRoot().add(vc);
		}
	}

	public ExpressionBase<HierarchicalGraphicalContent> graphicalContent() {
		return graphicalContent;
	}
	
	private ExpressionBase<HierarchicalGraphicalContent> makeGraphicalContent() {
		return new DrawMan(this).graphicalContent(getRoot());
	}

	public ExpressionBase<? extends Collection<? extends Node>> selection() {
		return selection;
	}

	/**
	 * Select all components, connections and groups from the <code>root</code> group.
	 */
	public void selectAll() {
		Collection<? extends Node> children = GlobalCache.eval(getRoot().children());
		if(GlobalCache.eval(selection).size()==children.size())
			return;
		
		selection.clear();
		selection.addAll(children);
	}

	/**
	 * Clear selection.
	 */
	public void selectNone() {
		selection.clear();
	}

	private void validateSelection (Node node) {
		if (!Hierarchy.isDescendant(node, getCurrentLevel()))
			throw new RuntimeException ("Cannot select a node that is not in the current editing level (" + node + "), parent (" + GlobalCache.eval(node.parent()) +")");
	}

	private void validateSelection (Collection<Node> nodes) {
		for (Node node : nodes)
			validateSelection(node);
	}

	public boolean isSelected(Node node) {
		return GlobalCache.eval(selection).contains(node);
	}

	public void select(Collection<Node> nodes) {
		if (nodes.isEmpty()) {
			selectNone();
			return;
		}
		
		validateSelection(nodes);

		selection.clear();
		selection.addAll(nodes);
	}

	public void select(Node node) {
		if (selection.getValue().size() == 1 && selection.getValue().contains(node))
			return;

		validateSelection(node);

		selection.clear();
		selection.add(node);
	}

	public void addToSelection(Node node) {
		if (selection.getValue().contains(node))
			return;

		validateSelection(node);
		
		selection.add(node);
	}

	public void addToSelection(Collection<Node> nodes) {
		validateSelection(nodes);

		selection.addAll(nodes);
	}

	public void removeFromSelection(Node node) {
		if (selection.getValue().contains(node)) {
			selection.remove(node);
		}
	}

	public void removeFromSelection(Collection<Node> nodes) {
		selection.removeAll(nodes);
	}

	public MathModel getMathModel() {
		return mathModel;
	}

	public VisualModel getVisualModel() {
		return this;
	}

	/**
	 * @return Returns selection ordered the same way as the objects are ordered in the currently active group.
	 */
	public Collection<Node> getSelection() {
		return GlobalCache.eval(selection);
	}
	
	public Collection<Node> getOrderedCurrentLevelSelection() {
		List<Node> result = new ArrayList<Node>();
		for(Node node : GlobalCache.eval(currentLevel.children()))
		{
			if(GlobalCache.eval(selection).contains(node) && node instanceof VisualNode)
				result.add((VisualNode)node);
		}
		return result;
	}

	public Container getCurrentLevel() {
		return currentLevel;
	}

	public void setCurrentLevel(Container newCurrentLevel) {
		selection.clear();
		currentLevel = newCurrentLevel;
	}

	private Collection<Node> getGroupableSelection()
	{
		ArrayList<Node> result = new ArrayList<Node>();
		for(Node node : getOrderedCurrentLevelSelection())
			if(node instanceof VisualTransformableNode)
				result.add((VisualTransformableNode)node);
		return result;
	}

	
	/**
	 * Groups the selection, and selects the newly created group.
	 * @author Arseniy Alekseyev
	 */
	public void groupSelection() {
		Collection<Node> selected = getGroupableSelection();
		VisualGroup vg = groupCollection(selected);
		if (vg!=null) select(vg);
	}
	
	public VisualGroup groupCollection(Collection<Node> selected) {

		if(selected.size() <= 1)
			return null;
		
		VisualGroup group = new VisualGroup();

		currentLevel.add(group);

		currentLevel.reparent(selected, group);

		ArrayList<Node> connectionsToGroup = new ArrayList<Node>();

		for(VisualConnection connection : Hierarchy.getChildrenOfType(currentLevel, VisualConnection.class))
		{
			if(Hierarchy.isDescendant(connection.getFirst(), group) && 
					Hierarchy.isDescendant(connection.getSecond(), group)) {
				connectionsToGroup.add(connection);
			}
		}
		
		currentLevel.reparent(connectionsToGroup, group);
		
		return group;
		
	}

	/**
	 * Ungroups all groups in the current selection and adds the ungrouped components to the selection.
	 * @author Arseniy Alekseyev
	 */
	public void ungroupSelection() {
		ArrayList<Node> toSelect = new ArrayList<Node>();
		
		for(Node node : getOrderedCurrentLevelSelection())
		{
			if(node instanceof VisualGroup)
			{
				VisualGroup group = (VisualGroup)node;
				for(Node subNode : group.unGroup())
					toSelect.add(subNode);
				currentLevel.remove(group);
			}
			else
				toSelect.add(node);
		}
		
		select(toSelect);
	}

	public void deleteSelection() {
		remove(getSelection());
	}

	/**
	 * @param clipboard
	 * @param clipboardOwner
	 * @author Ivan Poliakov
	 * @throws ParserConfigurationException 
	 */
	public void copy(Clipboard clipboard, ClipboardOwner clipboardOwner) throws ParserConfigurationException {
		Document doc = XmlUtil.createDocument();

		Element root = doc.createElement("workcraft-clipboard-contents");

		doc.appendChild(root);
		root = doc.getDocumentElement();
		//selectionToXML(root);		
		clipboard.setContents(new TransferableDocument(doc), clipboardOwner);
	}

	public Collection<Node> paste(Collection<Node> what, Point2D where) throws PasteException {
		/*try {
			Document doc = (Document)clipboard.getData(TransferableDocument.DOCUMENT_FLAVOR);

			Element root = doc.getDocumentElement();
			if (!root.getTagName().equals("workcraft-clipboard-contents"))
				return null;

			Element mathModelElement = XmlUtil.getChildElement("model", root);
			Element visualModelElement = XmlUtil.getChildElement("visual-model", root);

			if (mathModelElement == null || visualModelElement == null)
				throw new PasteException("Structure of clipboard XML is invalid.");

			mathModel.pasteFromXML(mathModelElement);
			//return pasteFromXML(visualModelElement, where);			 
		} catch (UnsupportedFlavorException e) {
		} catch (IOException e) {
			throw new PasteException (e);
		} catch (LoadFromXMLException e) {
			throw new PasteException (e);
		}*/

		return null;
	}



	public void cut(Clipboard clipboard, ClipboardOwner clipboardOwner) throws ParserConfigurationException {
		copy(clipboard, clipboardOwner);
		deleteSelection();
	}

	private Point2D transformToCurrentSpace(Point2D pointInRootSpace)
	{
		Point2D newPoint = new Point2D.Double();
		TransformHelper.getTransform(getRoot(), currentLevel).transform(pointInRootSpace, newPoint);
		return newPoint;
	}

	public Collection<Node> boxHitTest(Point2D p1, Point2D p2) {
		p1 = transformToCurrentSpace(p1);
		p2 = transformToCurrentSpace(p2);
		return HitMan.boxHitTest(currentLevel, p1, p2);
	}

	@Override public Properties getProperties(Node node) {
		return null;
	}
	
	@Override
	public void ensureConsistency() {
		super.ensureConsistency();
		((AbstractModel)mathModel).ensureConsistency();
		selectionEventPropagator.refresh();
	}
}