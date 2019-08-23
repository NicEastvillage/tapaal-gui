package pipe.gui.graphicElements;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.*;

import javax.swing.JComponent;

import net.tapaal.gui.DrawingSurfaceManager.AbstractDrawingSurfaceManager;
import net.tapaal.helpers.Reference.Reference;
import pipe.dataLayer.DataLayer;
import pipe.gui.canvas.DrawingSurfaceImpl;
import pipe.gui.Grid;
import pipe.gui.Pipe;
import pipe.gui.Translatable;
import pipe.gui.Zoomable;
import pipe.gui.Zoomer;
import pipe.gui.handler.PetriNetObjectHandler;

/**
 * Petri-Net Object Class 
 * Implements things in common between all types of objects
 */
public abstract class PetriNetObject extends JComponent implements Zoomable, Translatable {

	private static final long serialVersionUID = 2693171860021066729L;

	protected static final int COMPONENT_DRAW_OFFSET= 5;
	/** x/y position position on screen (zoomed) */
	protected double positionX;
	protected double positionY;

	/** X/Y-axis Position on screen */
	protected double nameOffsetX;
	protected double nameOffsetY;

	// The x/y coordinate of object at 100% zoom.
	//XXX: pushed down from PlaceTransitionObject, need further refactoring and rename, //kyrke 2019-08-23
	//XXX: should by int not double (location are tracked as ints) //kyrke 2019-08-23
	protected double locationX;
	protected double locationY;

	protected String id = null;

	/* Name Label for displaying name */
	protected NameLabel pnName;
	protected boolean selected = false; // True if part of the current selection.
	protected boolean selectable = true; // True if object can be selected.
	protected boolean draggable = true; // True if object can be dragged.

	protected Rectangle bounds = new Rectangle();

	protected boolean deleted = false;

	// Integer value which represents a zoom percentage
	protected int zoom = Pipe.ZOOM_DEFAULT;

	private DataLayer guiModel;
	private Reference<AbstractDrawingSurfaceManager> managerRef = null;

	public PetriNetObjectHandler getMouseHandler() {
		return mouseHandler;
	}

	protected PetriNetObjectHandler mouseHandler;

	PetriNetObject() {

		addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (managerRef!=null && managerRef.get()!=null) {
					managerRef.get().triggerEvent(new AbstractDrawingSurfaceManager.DrawingSurfaceEvent(
							PetriNetObject.this, e, AbstractDrawingSurfaceManager.MouseAction.clicked
					));
				}
				if (mouseHandler != null) {
					mouseHandler.mouseClicked(e);
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (managerRef!=null && managerRef.get()!=null) {
					managerRef.get().triggerEvent(new AbstractDrawingSurfaceManager.DrawingSurfaceEvent(
							PetriNetObject.this, e, AbstractDrawingSurfaceManager.MouseAction.pressed
					));
				}
				if (mouseHandler != null) {
					mouseHandler.mousePressed(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (managerRef!=null && managerRef.get()!=null) {
					managerRef.get().triggerEvent(new AbstractDrawingSurfaceManager.DrawingSurfaceEvent(
							PetriNetObject.this, e, AbstractDrawingSurfaceManager.MouseAction.released
					));
				}
				if (mouseHandler != null) {
					mouseHandler.mouseReleased(e);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				if (managerRef!=null && managerRef.get()!=null) {
					managerRef.get().triggerEvent(new AbstractDrawingSurfaceManager.DrawingSurfaceEvent(
							PetriNetObject.this, e, AbstractDrawingSurfaceManager.MouseAction.entered
					));
				}
				if (mouseHandler != null) {
					mouseHandler.mouseEntered(e);
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (managerRef!=null && managerRef.get()!=null) {
					managerRef.get().triggerEvent(new AbstractDrawingSurfaceManager.DrawingSurfaceEvent(
							PetriNetObject.this, e, AbstractDrawingSurfaceManager.MouseAction.exited
					));
				}
				if (mouseHandler != null) {
					mouseHandler.mouseExited(e);
				}
			}
		});

		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (managerRef!=null && managerRef.get()!=null) {
					managerRef.get().triggerEvent(new AbstractDrawingSurfaceManager.DrawingSurfaceEvent(
							PetriNetObject.this, e, AbstractDrawingSurfaceManager.MouseAction.wheel
					));
				}
				if (mouseHandler != null) {
					mouseHandler.mouseWheelMoved(e);
				}
			}
		});

		addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (managerRef!=null && managerRef.get()!=null) {
					managerRef.get().triggerEvent(new AbstractDrawingSurfaceManager.DrawingSurfaceEvent(
							PetriNetObject.this, e, AbstractDrawingSurfaceManager.MouseAction.dragged
					));
				}
				if (mouseHandler != null) {
					mouseHandler.mouseDragged(e);
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				/*if (managerRef!=null && managerRef.get()!=null) {
					managerRef.get().triggerEvent(new AbstractDrawingSurfaceManager.DrawingSurfaceEvent(
							PetriNetObject.this, e, AbstractDrawingSurfaceManager.MouseAction.moved
					));
				}*/
				if (mouseHandler != null) {
					mouseHandler.mouseMoved(e);
				}
			}
		});

	}

	public void setGuiModel(DataLayer guiModel) {
		this.guiModel = guiModel;
	}
	//XXX: not sure if datalayer should be accessable, but needed for refactorings away from "public" view.
	public DataLayer getGuiModel() { return this.guiModel;}

	public void setId(String idInput) {
		id = idInput;
		setName(idInput);
	}
	
	protected void updateLabelLocation() {
		this.getNameLabel().setPosition(
				Grid.getModifiedX((int) (positionX + Zoomer.getZoomedValue(nameOffsetX, zoom))), 
				Grid.getModifiedY((int) (positionY + Zoomer.getZoomedValue(nameOffsetY, zoom)))
		);
	}
	
	public void updateOnMoveOrZoom() {
		updateLabelLocation();
	}
	/**
	 * Set X-axis offset for name position
	 * 
	 * @param nameOffsetXInput
	 *            Double value for name X-axis offset
	 */
	public void setNameOffsetX(double nameOffsetXInput) {
		nameOffsetX += Zoomer.getUnzoomedValue(nameOffsetXInput, zoom);
	}

	/**
	 * Set Y-axis offset for name position
	 * 
	 * @param nameOffsetYInput
	 *            Double value for name Y-axis offset
	 */
	public void setNameOffsetY(double nameOffsetYInput) {
		nameOffsetY += Zoomer.getUnzoomedValue(nameOffsetYInput, zoom);
	}
	/**
	 * Get X-axis offset for ...
	 * 
	 * @return Double value for X-axis offset of ...
	 */
	public Double getNameOffsetXObject() {
		return nameOffsetX;
	}

	/**
	 * Moved to PetriNetObject Get Y-axis offset for ...
	 * 
	 * @return Double value for Y-axis offset of ...
	 */
	public Double getNameOffsetYObject() {
		return nameOffsetY;
	}

	/**
	 * Get id returns null if value not yet entered
	 * 
	 * @return String value for id;
	 */
	public String getId() {
		return getName();
	}

	/**
	 * Returns Name Label - is used by GuiView
	 * 
	 * @return PetriNetObject's Name Label (Model View Controller Design
	 *         Pattern)
	 */
	public NameLabel getNameLabel() {
		return pnName;
	}

	public void addLabelToContainer() {
		if (getParent() != null && pnName.getParent() == null) {
			getParent().add(pnName);
		}
	}
	public void removeLabelFromContainer() {
		if (getParent() != null && pnName != null) {
			getParent().remove(pnName);
		}
	}

	public boolean isSelected() {
		return selected;
	}

	public void select() {
		select(true);
	}

	public void select(boolean shouldRepaint) {
		if (selectable && !selected) {
			selected = true;

			if (pnName != null) {
				pnName.setForeground(Pipe.SELECTION_LINE_COLOUR);
			}

			if (shouldRepaint) {
				repaint();
			}
		}

	}

	public void deselect() {
		if (selected) {
			selected = false;

			if (pnName != null) {
				pnName.setForeground(Pipe.ELEMENT_LINE_COLOUR);
			}

			repaint();
		}
	}

	public boolean isSelectable() {
		return selectable;
	}

	public void setSelectable(boolean allow) {
		selectable = allow;
	}

	public boolean isDraggable() {
		return draggable;
	}

	public void setDraggable(boolean allow) {
		draggable = allow;
	}

	public abstract void addedToGui();
	public abstract void removedFromGui();

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	public boolean isDeleted() {
		return deleted;
	}

	public void select(Rectangle selectionRectangle) {
		if (selectionRectangle.intersects(this.getBounds())) {
			select();
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}

	public abstract int getLayerOffset();

	public int getZoom() {
		return zoom;
	}

	public DrawingSurfaceImpl getParent() {
		return (DrawingSurfaceImpl) super.getParent();
	}


	public Reference<AbstractDrawingSurfaceManager> getManagerRef() {
		return managerRef;
	}

	public void setManagerRef(Reference<AbstractDrawingSurfaceManager> manager) {
		this.managerRef = manager;
	}

    /**
     * Get X-axis position, returns null if value not yet entered
     *
     * @return Double value for X-axis position
     */
    public Double getPositionXObject() {
        return locationX;
        // return new Double(positionX);
    }

    /**
     * Get Y-axis position, returns null if value not yet entered
     *
     * @return Double value for Y-axis position
     */
    public Double getPositionYObject() {
        return locationY;
        // return new Double(positionY);
    }
}
