package org.workcraft.gui.modeleditor
import javax.swing.JPanel
import org.workcraft.dependencymanager.advanced.core.ExpressionBase
import org.workcraft.dependencymanager.advanced.core.GlobalCache
import javax.swing.Timer
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import org.workcraft.dependencymanager.advanced.core.EvaluationContext
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import org.workcraft.dependencymanager.advanced.user.Variable
import java.awt.event.FocusListener
import java.awt.event.FocusEvent
import org.workcraft.scala.Expressions.Expression
import java.awt.BasicStroke

class ModelEditorPanel extends JPanel {
	class ImageModel 
	
	class Repainter extends ExpressionBase[ImageModel] {
override def evaluate(context : EvaluationContext) : ImageModel = {
			context.resolve(graphicalContent)
			repaint

			new ImageModel()
		}
	  
	}
	
	object Repainter {
	  def start = {
	    val repainter = new Repainter	    
	    
			new Timer(20, new ActionListener {
				override def actionPerformed(e: ActionEvent) = GlobalCache.eval(repainter)
			}).start
	  } 
	}

	object Resizer extends ComponentAdapter {
		override def componentResized(e: ComponentEvent) = reshape
	}

	object FocusListener extends FocusListener {
		val value = Variable.create(false)
		
		override def focusGained(e: FocusEvent) = value.setValue(true)
		override def focusLost(e: FocusEvent) =	value.setValue(false)
	}

	def hasFocus : Expression[Boolean] = FocusListener.value
	
	val view  = new Viewport(0, 0, getWidth(), getHeight())
	val grid  = new Grid(view)
	val ruler = new Ruler(grid)

	val borderStroke = new BasicStroke(2)

	//private Overlay overlay = new Overlay();
	
	var firstPaint = true
	
	def mouseListener: Option[GraphEditorTool] => GraphEditorMouseListener = {
	  case Some(tool) => tool.mouseListener
	  case None => DummyMouseListener
	}
	
	def reshape = {
		view.setShape(15, 15, getWidth()-15, getHeight()-15)
		ruler.setShape(0, 0, getWidth(), getHeight())
	}

	val graphicalContent = new ExpressionBase[GraphicalContent] {

		@Override
		protected GraphicalContent evaluate(final EvaluationContext context) {
			
			final GraphEditorTool tool = context.resolve(
					toolboxPanel.selectedTool());
			
			return new GraphicalContent() {
				
				@Override
				public void draw(Graphics2D g2d) {
					AffineTransform screenTransform = new AffineTransform(g2d.getTransform());

					if (firstPaint) {
						reshape();
						firstPaint = false;
					}

					g2d.setBackground(context.resolve(CommonVisualSettings.backgroundColor));
					g2d.clearRect(0, 0, getWidth(), getHeight());
					context.resolve(grid.graphicalContent()).draw(g2d);
					g2d.setTransform(screenTransform);

					
					g2d.transform(context.resolve(view.transform()));
					
					g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
					g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
					g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
					g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//					g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				
					g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

					context.resolve(tool.userSpaceContent(view, hasFocus)).draw(org.workcraft.util.Graphics.cloneGraphics(g2d));

					g2d.setTransform(screenTransform);

					context.resolve(ruler.graphicalContent()).draw(g2d);

					if (context.resolve(hasFocus)) {
						context.resolve(tool.screenSpaceContent(view, hasFocus)).draw(g2d);
						g2d.setTransform(screenTransform);

						g2d.setStroke(borderStroke);
						g2d.setColor(context.resolve(CommonVisualSettings.foregroundColor));
						g2d.drawRect(0, 0, getWidth()-1, getHeight()-1);
					}
					
				}
			};
		}
		
	};
	
}

object ModelEditorPanel {
	  
}
	
	public GraphEditorPanel(MainWindow mainWindow, WorkspaceEntry workspaceEntry) throws ServiceNotAvailableException {
		super (new BorderLayout());
		this.mainWindow = mainWindow;
		this.workspaceEntry = workspaceEntry;
		
		GraphEditable graphEditable = workspaceEntry.getModelEntry().getImplementation(GraphEditable.SERVICE_HANDLE);
		
		Repainter.start

		toolboxPanel = new ToolboxPanel(graphEditable.createTools(this));

		GraphEditorPanelMouseListener mouseListener = new GraphEditorPanelMouseListener(this, fmap(mouseListenerGetter, toolboxPanel.selectedTool()));
		GraphEditorPanelKeyListener keyListener = new GraphEditorPanelKeyListener(this, toolboxPanel);

		addMouseMotionListener(mouseListener);
		addMouseListener(mouseListener);
		addMouseWheelListener(mouseListener);
		
		GraphEditorFocusListener focusListener = new GraphEditorFocusListener();
		addFocusListener(focusListener);
		hasFocus = focusListener;
		
		addComponentListener(new Resizer());

		addKeyListener(keyListener);
		
		add(overlay, BorderLayout.CENTER);
		
		updatePropertyView(graphEditable.properties());
	}

	

	@Override
	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		
		GlobalCache.eval(graphicalContent).draw(g2d);
		
		paintChildren(g2d);
	}

	public Viewport getViewport() {
		return view;
	}

	public Point2D snap(Point2D point) {
		return new Point2D.Double(grid.snapCoordinate(point.getX()), grid.snapCoordinate(point.getY()));
	}
	
	public MainWindow getMainWindow() {
		return mainWindow;
	}
	
	private void updatePropertyView(Expression<? extends PVector<EditableProperty>> properties) {
		final PropertyEditorWindow propertyWindow = mainWindow.getPropertyView();
		
		propertyWindow.propertyObject.setValue(properties);
	}

	@Override
	public EditorOverlay getOverlay() {
		return overlay;
	}

	public ToolboxPanel getToolBox() {
		return toolboxPanel;
	}

	public WorkspaceEntry getWorkspaceEntry() {
		return workspaceEntry;
	}

	@Override
	public Function<Point2D, Point2D> snapFunction() {
		return new Function<Point2D, Point2D>() {
			@Override
			public Point2D apply(Point2D argument) {
				return snap(argument);
			}
		};	
	}
}
