package kronick.sandboxsystem;

import processing.core.*;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.*;
//import java.awt.event.*;

public class SandboxSystem extends PApplet{
	// CONSTANTS
	int GRID_SIZE;

	// MEMBER OBJECTS
	Grid grid;
	ZoomEvent mWheel;
	ArrayList<XY[]> selections = new ArrayList<XY[]>();

	// VIEWPORT VARIABLES
	float zoom;
	float zoomTarget;
	XY center;
	XY centerTarget;
	boolean dragging;

	int speed;

	ArrayList<Block> blocks = new ArrayList<Block>();
	ArrayList<Arm> arms = new ArrayList<Arm>();
	//ArrayList<Truck> Trucks = new ArrayList<Truck>();
	//ArrayList<Worker> workers = new ArrayList<Worker>();
	//ArrayList<Elevator> elevators = new ArrayList<Elevator>();

	public void setup() { }

	public void draw() { }

	public void update() { }
	public void update(int n) { }

	protected void preDraw() {
		translate(width/2, height/2);
		zoom -= (zoom-zoomTarget) * .3;
		if(abs(zoom-zoomTarget) < .005) { zoom = zoomTarget;  }
		scale(zoom);

		center.x -= (center.x-centerTarget.x) * .3;
		center.y -= (center.y-centerTarget.y) * .3;
		translate((float)center.x, (float)center.y);

		background(0,0,255);

		grid.regenerateBackground(zoom, center);
		grid.draw(zoom, center);
		// Update each object n times
		for(int n=0; n<speed; n++) {
			update(n);
		}

		// Figure out which blocks are selected
		if(selections.size() > 0) {
			for(int i=0; i<blocks.size(); i++) {
				Block b = blocks.get(i);
				b.selected = false;
				for(int j=0; j<selections.size(); j++) {
					if(selections.get(j)[0] != null && selections.get(j)[1] != null) {
						if(inRect(b.position, selections.get(j))) {
							b.selected = true;
							break;
						}
					}
				}
			}
		}
	}

	protected void postDraw() {
		// Draw selection box if currently drawing one
		if(dragging && mouseButton == RIGHT) {
			pushStyle();
			rectMode(CORNERS);
			noFill();
			stroke(0,0,255);
			strokeWeight(2);
			XY[] sel = selections.get(selections.size()-1);
			XY end = grid.quantize(mouseToCoord(mouseX, mouseY), grid.EDGE);
			rect((float)sel[0].x, (float)sel[0].y, (float)end.x, (float)end.y);
			popStyle();
		}
		pushStyle();
		rectMode(CORNERS);
		fill(0,0,255,Math.abs(frameCount%50-25) * 4 + 0);
		noStroke();
		if(selections.size() > 0) {
			for(int i=0; i<selections.size(); i++) {
				XY[] sel = selections.get(i);
				if(sel[0] != null && sel[1] != null) {
					rect((float)sel[0].x, (float)sel[0].y, (float)sel[1].x, (float)sel[1].y);
				}
			}
		}
		popStyle();
	}

	public XY mouseToCoord(int x, int y) {
		XY clickloc = new XY(x, y);
		clickloc.subtract(width/2, height/2);
		clickloc.scale(1/zoom);
		clickloc.subtract(center);
		return clickloc;
	}

	public void mouseReleased() {
		if(mouseButton == LEFT) {
			if(!dragging) {
				XY clickloc = mouseToCoord(mouseX, mouseY);

				int[] centerBlock = grid.pixelToGrid(clickloc);
				println(clickloc.text() + ": " + centerBlock[0] + ", " + centerBlock[1]);
				int radius = (int)random(3,(10/(GRID_SIZE/10f)));
				for(int a=-radius-1; a<radius+1; a++) {
					for(int b=-radius-1; b<radius+1; b++) {
						if((sq(a) + sq(b)) < sq(radius)) {
							blocks.add(new Block(this, grid.quantizedOffset(centerBlock[0], centerBlock[1], a, b), GRID_SIZE));
						}
					}
				}
			}
		}
		else if(mouseButton == RIGHT) {
			if(dragging) {
				// Finish the selection
				selections.get(selections.size()-1)[1] = grid.quantize(mouseToCoord(mouseX, mouseY), grid.EDGE);;
			}
			else {	// If the mouse wasn't dragged, there is no selection to add
				selections.remove(selections.size()-1);
			}
		}
		dragging = false;
	}

	public void mousePressed() {
		if(mouseButton == RIGHT) {
			if(!(keyPressed && keyCode == CONTROL) || selections == null) { // Reset selection if CONTROL is not pressed, otherwise append to selection
				selections = new ArrayList<XY[]>();
			}
			XY[] newSel = new XY[2];
			newSel[0] = grid.quantize(mouseToCoord(mouseX, mouseY), grid.EDGE);;
			newSel[1] = null;
			selections.add(newSel);
		}
	}

	public void mouseDragged() {
		dragging = true;
		if(mouseButton == LEFT) {
			centerTarget.translate((mouseX-pmouseX) / zoom, (mouseY-pmouseY) / zoom);
		}
		else if(mouseButton == RIGHT) {

		}
	}

	public class ZoomEvent implements MouseWheelListener {
		public ZoomEvent() {
			addMouseWheelListener(this);
		}
		public void mouseWheelMoved(MouseWheelEvent e) {
			zoomTarget -= e.getWheelRotation() * zoom * .05;
			if(zoomTarget <= .1) zoomTarget = .1f;
		}
	}

	public boolean inRect(XY point, XY[] corners) {
		XY left, right, top, bottom;
		if(corners[0].x < corners[1].x) {
			left = corners[0];
			right = corners[1];
		}
		else {
			left = corners[1];
			right = corners[0];
		}
		if(corners[0].y < corners[1].y) {
			top = corners[1];
			bottom = corners[0];
		}
		else {
			top = corners[0];
			bottom = corners[1];
		}
		if(point.x <= right.x && point.x >= left.x && point.y <= top.y && point.y >= bottom.y) return true;
		else return false;
	}
}
