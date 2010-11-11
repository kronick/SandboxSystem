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

	XY topLeft, bottomRight;

	int speed;

	private int startDrawTime;

	ArrayList<Block> blocks = new ArrayList<Block>();
	ArrayList<Arm> arms = new ArrayList<Arm>();
	//ArrayList<Truck> Trucks = new ArrayList<Truck>();
	ArrayList<Worker> workers = new ArrayList<Worker>();
	ArrayList<Elevator> elevators = new ArrayList<Elevator>();

	public void setup() { }

	public void draw() { }

	public void updateView() {
		topLeft = mouseToCoord(0,0);
		bottomRight = mouseToCoord(width, height);
	}
	public void update(int n) {
	}

	protected void preDraw() {
		pushMatrix();
		translate(width/2, height/2);
		zoom -= (zoom-zoomTarget) * .3;
		if(abs(zoom-zoomTarget) < .05) { zoom = zoomTarget;  }
		scale(zoom);

		center.x -= (center.x-centerTarget.x) * .3;
		center.y -= (center.y-centerTarget.y) * .3;
		if(abs((float)(center.x - centerTarget.x)) < .01) center.x = centerTarget.x;
		if(abs((float)(center.y - centerTarget.y)) < .01) center.y = centerTarget.y;
		translate((float)center.x, (float)center.y);

		background(0,0,0);

		grid.regenerateBackground(zoom, center);
		grid.draw(zoom, center);
		// Update each object n times
		int start = this.millis();
		for(int n=0; n<speed; n++) {
			update(n);
		}

		//if((millis()-start) > 10)
		//	println("Update took: " + (millis()-start) + " milliseconds");


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

		startDrawTime = millis();
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
		popMatrix();

		//if((millis()-startDrawTime) > 10)
		//	println("Draw took: " + (millis()-startDrawTime) + " milliseconds");
	}

	public void xformBlock(Block b) {	}

	public XY mouseToCoord() {
		return mouseToCoord(mouseX, mouseY);
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
				int radius = (int)random(3,(20/(GRID_SIZE/10f)));
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
				selections.get(selections.size()-1)[1] = grid.quantize(mouseToCoord(), grid.EDGE);;
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
			newSel[0] = grid.quantize(mouseToCoord(), grid.EDGE);;
			newSel[1] = null;
			selections.add(newSel);
		}
	}

	public void mouseDragged() {
		if(abs(mouseX-pmouseX) > 1 || abs(mouseY-pmouseY) > 1) {
			dragging = true;
			if(mouseButton == LEFT) {
				centerTarget.translate((mouseX-pmouseX) / zoom, (mouseY-pmouseY) / zoom);
			}
			else if(mouseButton == RIGHT) {

			}
		}
	}

	public class ZoomEvent implements MouseWheelListener {
		public ZoomEvent() {
			addMouseWheelListener(this);
		}
		public void mouseWheelMoved(MouseWheelEvent e) {
			zoomTarget -= e.getWheelRotation() * zoom * .05;
			if(zoomTarget <= .01) zoomTarget = .01f;
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

	public boolean inView(XY point) {
		return inView((float)point.x, (float)point.y);
	}
	public boolean inView(float x, float y) {
		try {
			return (x >= topLeft.x && x <= bottomRight.x && y >= topLeft.y && y <= bottomRight.y);
		}
		catch (NullPointerException e) { return true; }
	}

	public int XYtoPixel(XY point) {
		XY p = point.get().subtract(center).scale(1/zoom).subtract(width/2, height/2);
		p.y *= -1;
		//println("good pixel: " + p.x + ", " + p.y);
		if(p.x <= width && p.y <= height && p.x >= 0 && p.y >= 0) {
			return (int)(p.x+p.y*width);
		}
		else return 0;
	}

	public boolean floorsConnected(int a, int b) {
		// Iterate through all the elevators. Make a list of elevators that serve floor a, and ones that serve floor b.
		// If there are no common...
		// TODO: Bah, this needs to be recursive and generalized to work for a n-elevator route.
		// Good practice for routing blocks across arms, too

		ArrayList<Elevator> serveA = new ArrayList<Elevator>();
		ArrayList<Elevator> serveB = new ArrayList<Elevator>();
		Elevator _e;
		for(int i=0; i<elevators.size(); i++) {
			_e = elevators.get(i);
			if(_e.servesFloor(a)) serveA.add(_e);
			if(_e.servesFloor(b)) serveB.add(_e);
		}
		for(int i=0; i<serveA.size(); i++) {
			if(serveB.contains(serveA.get(i))) return true;
		}
		return false;
	}

	public boolean gridConnected(int[] a, int[] b) {
		return true;
	}

	ArrayList<Block> blocksAt(int x, int y) {
		ArrayList<Block> blocksout = new ArrayList<Block>();
		int[] pos = new int[2];
		for(int i=0; i<blocks.size(); i++) {
			pos = grid.pixelToGrid(blocks.get(i).position);
			if(pos[0] == x && pos[1] == y) {
				blocksout.add(blocks.get(i));
			}
		}
		return blocksout;
	}
}
