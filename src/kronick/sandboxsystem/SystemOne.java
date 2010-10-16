package kronick.sandboxsystem;

import processing.core.*;
import java.util.*;
import java.awt.event.*;

import kronick.sandboxsystem.SandboxSystem.ZoomEvent;

public class SystemOne extends SandboxSystem {
	PFont monoFont;


	PGraphics bg;

	int clickTimer = 5;
	boolean doubleClick = false;

	// CUSTOM PARAMETERS
	int ARM_COUNT = 24;

	public void setup() {
		println("System One");
		size(1280, 600, JAVA2D);
		smooth();
		frameRate(60);
		colorMode(HSB);

		//BACKGROUND_COLOR = color(0,0,2,80);
		//FLOOR_COLOR = color(0,0,225);

		// OVERRIDE DEFAULT VALUES
		GRID_SIZE = 20;
		zoom = 3;
		zoomTarget = .5f;
		center = new XY(0,0);
		centerTarget = new XY(0,0);
		dragging = false;

		mWheel = new ZoomEvent();
		grid = new Grid(this);

		// Create some arms
		final int w = 24;
		float sc = GRID_SIZE/15f;
		for(int i=0; i<ARM_COUNT; i++) {
		 arms.add(new Arm(this, random(90*sc,200*sc), random(90*sc,200*sc), grid.quantize(new XY((i%w - w/2 + .5) * GRID_SIZE * 20,
																					(i/w - ARM_COUNT/w/2 + .5) * GRID_SIZE * 10))));
		}
		imageMode(CENTER);
	}

	public void draw() {
		speed = (int)(sq((float)mouseY / height) * 49f + 1);
		super.preDraw();

		// Draw each object

		for(int i=0; i<blocks.size(); i++) {
			blocks.get(i).draw();
		}

		for(int i=0; i<arms.size(); i++) {
			arms.get(i).draw();
		}


		super.postDraw();
	}

	public void update(int n) {
		Arm _a;
		if((frameCount*speed + n) % 100 == 0) {
			for(int i=0; i<arms.size(); i++) {
				_a = arms.get(i);
				_a.dropAtTarget = true;

				if(_a.cargo != null) { // randomly drop it off somewhere
					float randomTheta = random(0,TWO_PI);
					float randomR     = random(_a.r1 - _a.r2, _a.r1 + _a.r2);
					XY goal = grid.quantize(new XY(randomR*sin(randomTheta) + _a.origin.x, randomR*cos(randomTheta) + _a.origin.y));
					goal.subtract(_a.origin);
					_a.setGoal(goal);
				}
				else { // Build a list of available blocks, randomly choose one that's within range
					ArrayList<Block> reachableBlocks = new ArrayList<Block>();
					for(int j=0; j<blocks.size(); j++) {
						double dist = blocks.get(j).position.distance(_a.origin);
						if(dist < _a.r1+_a.r2 && dist > _a.r1-_a.r2) {
							reachableBlocks.add(blocks.get(j));
						}
					}
					if(reachableBlocks.size() > 0) {
						int t = round(random(0,reachableBlocks.size()-1));
						_a.setGoal(reachableBlocks.get(t).position.get().subtract(_a.origin));
					}
				}
			}
		}

		for(int i=0; i<arms.size(); i++) {
			arms.get(i).update();
		}
	}


	public void keyPressed() {
		if(key == DELETE || key == BACKSPACE) {
			println("removing blocks");
			// remove all currently selected blocks
			ArrayList<Block> toRemove = new ArrayList<Block>();
			for(int i=0; i<blocks.size(); i++) {
				if(blocks.get(i).selected) {
					toRemove.add(blocks.get(i));
				}
			}
			for(int i=0; i<toRemove.size(); i++) {
				blocks.remove(toRemove.get(i));
				selections.clear();
			}
		}
	}


	public static void main(String args[]) {
		PApplet.main(new String[] { "kronick.sandboxsystem.SystemOne" });
	}
}
