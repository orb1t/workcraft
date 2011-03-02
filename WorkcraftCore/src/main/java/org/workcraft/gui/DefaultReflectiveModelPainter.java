package org.workcraft.gui;

import java.awt.Graphics2D;

import org.workcraft.dependencymanager.advanced.core.Expression;
import org.workcraft.dependencymanager.advanced.core.Expressions;
import org.workcraft.dom.Node;
import org.workcraft.dom.visual.ColorisableGraphicalContent;
import org.workcraft.dom.visual.DrawMan;
import org.workcraft.dom.visual.DrawRequest;
import org.workcraft.dom.visual.DrawableNew;
import org.workcraft.dom.visual.GraphicalContent;
import org.workcraft.dom.visual.TouchableProvider;
import org.workcraft.dom.visual.VisualGroup;
import org.workcraft.gui.graph.tools.Colorisation;
import org.workcraft.gui.graph.tools.Colorisator;
import org.workcraft.gui.graph.tools.NodePainter;
import org.workcraft.util.Func;
import org.workcraft.util.Function2;

public class DefaultReflectiveModelPainter {
	
	public static final class ReflectiveNodePainter implements NodePainter {
		private final Colorisator colorisator;
		private final TouchableProvider<Node> tp;

		public ReflectiveNodePainter(final TouchableProvider<Node> tp, Colorisator colorisator) {
			this.tp = tp;
			this.colorisator = colorisator;
		}

		public static Expression<? extends GraphicalContent> colorise(Expression<? extends ColorisableGraphicalContent> gc, Expression<? extends Colorisation> colorisation) {
			return Expressions.bindFunc(colorisation, gc, new Function2<Colorisation, ColorisableGraphicalContent, GraphicalContent>() {
				@Override
				public GraphicalContent apply(final Colorisation colorisation, final ColorisableGraphicalContent content) {
					return new GraphicalContent() {
						@Override
						public void draw(final Graphics2D graphics) {
							DrawRequest request = new DrawRequest(){
								@Override
								public Colorisation getColorisation() {
									return colorisation;
								}
								@Override
								public Graphics2D getGraphics() {
									return graphics;
								}
							};									
							content.draw(request);
						}
					};
				}
			});
		}

		@Override
		public Expression<? extends GraphicalContent> getGraphicalContent(Node node) {
			final Expression<? extends Colorisation> colorisation = colorisator.getColorisation(node);
			if(node instanceof DrawableNew) {
				final DrawableNew drawable = (DrawableNew) node; 
				final Expression<? extends ColorisableGraphicalContent> gc = drawable.graphicalContent();
				return colorise(gc, colorisation);
			}
			else
				if(node instanceof VisualGroup) {
					return colorise(VisualGroup.graphicalContent(tp, ((VisualGroup)node)), colorisation);
				}
			else
				return Expressions.constant(GraphicalContent.EMPTY);
		}
	}

	public static Func<Colorisator, Expression<? extends GraphicalContent>> reflectivePainterProvider(final TouchableProvider<Node> tp, final Node root) {
		return new Func<Colorisator, Expression<? extends GraphicalContent>>(){
			@Override
			public Expression<? extends GraphicalContent> eval(Colorisator colorisator) {
				return createReflectivePainter(tp, root, colorisator);
			}
		};
	}
	
	public static Expression<? extends GraphicalContent> createReflectivePainter(TouchableProvider<Node>tp, Node root, final Colorisator colorisator) {
		return DrawMan.graphicalContent(root, new ReflectiveNodePainter(tp, colorisator));
	}
}