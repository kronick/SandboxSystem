package kronick.sandboxsystem;

import processing.core.*;
import processing.video.*;
import java.util.*;
import processing.opengl.*;

public class SystemTwo extends SandboxSystem {
	PFont monoFont10, monoFont12;

	PGraphics bg;

	int clickTimer = 5;
	boolean doubleClick = false;

	// CUSTOM PARAMETERS
	int ARM_COUNT = 4;

	Capture cam;

	public void setup() {
		size(800, 600, JAVA2D);
		println("System Two");
		smooth();
		frameRate(30);
		colorMode(HSB);

		//cam = new Capture(this, 43, 33, Capture.list()[3]);
		//cam = new Capture(this, 64, 48, Capture.list()[3]);
		cam = new Capture(this, 128, 96, Capture.list()[3]);

		monoFont10 = loadFont("data/CourierNewPS-BoldMT-10.vlw");
		monoFont12 = loadFont("data/CourierNewPS-BoldMT-12.vlw");
		hint(ENABLE_NATIVE_FONTS);
		textFont(monoFont10);
		textAlign(CENTER, CENTER);

		//BACKGROUND_COLOR = color(0,0,2,80);
		//FLOOR_COLOR = color(0,0,225);

		// OVERRIDE DEFAULT VALUES
		GRID_SIZE = 40;
		zoom = 3;
		zoomTarget = .5f;
		center = new XY(0,0);
		centerTarget = new XY(0,0);
		dragging = false;

		mWheel = new ZoomEvent();
		grid = new Grid(this);

		// Create some arms

		/*
		final int w = 4;
		float sc = GRID_SIZE/15f;
		for(int i=0; i<ARM_COUNT; i++) {
		 arms.add(new Arm(this, random(90*sc,200*sc), random(90*sc,200*sc), grid.quantize(new XY((i%w - w/2 + .5) * GRID_SIZE * 20,
																					(i/w - ARM_COUNT/w/2 + .5 - 1) * GRID_SIZE * 20))));
		}
		*/

		elevators.add(new Elevator(this, grid.quantize(new XY(-400,200)), 20));
		elevators.add(new Elevator(this, grid.quantize(new XY(400,200)), 20));

		for(int i=0; i<10; i++) {
			workers.add(new Worker(this, grid.quantize(new XY(0, random(-600,200)), Grid.FLOOR)));
		}


		imageMode(CENTER);
	}

	public void draw() {
		speed = (int)(sq((float)mouseY / height) * 49f + 1);
		//speed = 1;
		super.preDraw();

		//println(frameRate);
		/*
		if(cam.available()) {
			cam.read();
		}

		pushMatrix();
		scale(GRID_SIZE);
		image(cam,0,0);
		popMatrix();
		*/

		// Draw each object


		for(int i=0; i<blocks.size(); i++) {
			blocks.get(i).draw();
		}



		for(int i=0; i<arms.size(); i++) {
			arms.get(i).draw();
		}

		for(int i=0; i<elevators.size(); i++) {
			elevators.get(i).draw();
		}

		for(int i=0; i<workers.size(); i++) {
			workers.get(i).draw();
		}

		super.postDraw();

		pushStyle();
		fill(255);
		textFont(monoFont12);
		textAlign(LEFT, CENTER);
		text((int)frameRate, 0, height-10);
		popStyle();
	}

	public void update(int n) {
		updateView();
		Arm _a;
		if((frameCount*speed + n) % 100 == 0) {
			// ARM LOGIC
			// ===============================================================================
			for(int i=0; i<arms.size(); i++) {
				_a = arms.get(i);
				_a.dropAtTarget = true;

				if(_a.cargo != null) { // randomly drop it off somewhere
					float randomTheta = random(0,TWO_PI);
					float randomR     = random(_a.r1 - _a.r2, _a.r1 + _a.r2);
					//randomTheta = PI;
					//randomR = _a.r1+_a.r2  - 10;
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

			/*
			Elevator _e;
			for(int j=0; j<1; j++) {
				for(int i=0; i<elevators.size(); i++) {
					_e = elevators.get(i);
					int randomFloor = (int)random(0, _e.height);
					int randomDir = (random(-1,1) < 0 ? -1 : 1);
					_e.call(randomFloor, randomDir);
				}
			}
			*/


			// WORKER LOGIC
			// ===============================================================================
			Worker _w;
			for(int i=0; i<workers.size(); i++) {
				_w = workers.get(i);
				if(_w.state == Worker.IDLE) {
					// Try to find a block on the current floor, then move towards it.
					// If no block is on current floor, move to nearest elevator and call it.
					int workerFloor = grid.positionToFloor(_w.position);
					ArrayList<Block> blocksOnThisFloor = new ArrayList<Block>();
					HashMap<Integer, Integer> floorPopulationMap = new HashMap<Integer, Integer>();	// Keep track of how many blocks are on each floor
					Block _b;
					for(int b=0; b<blocks.size(); b++) {
						_b = blocks.get(b);
						int blockFloor = grid.positionToFloor(_b.position);

						if(_b.workable()) {	// If it's not already been worked on, add it to the total for its floor
							int weight = (_b.type == Block.STALE ? _b.age/1000 : 100);
							if(floorPopulationMap.get(blockFloor) == null) floorPopulationMap.put(blockFloor, weight);
							else floorPopulationMap.put(blockFloor, floorPopulationMap.get(blockFloor) + weight);
						}

						if(blockFloor == workerFloor && _b.workable()) {
							blocksOnThisFloor.add(_b);
						}
					}

					if(blocksOnThisFloor.size() > 0) {
						Block randomBlock = blocksOnThisFloor.get((int)random(0, blocksOnThisFloor.size()-1));
						XY t = randomBlock.position.get();
						t.y = _w.position.y;
						_w.setTarget(t);
					}
					else if(blocks.size() > 0) {
						// Take an elevator to the floor with the most blocks.
						// First find the floor with most blocks (based on the map created earlier)
						int[] floorsWithBlocks = new int[floorPopulationMap.size()];
						int most = -1;
						int j = 0;
						Iterator it = floorPopulationMap.keySet().iterator();
						while(it.hasNext()) {
							Integer k = (Integer)it.next();
							if(floorPopulationMap.get(k) > most && floorsConnected(_w.currFloor, k))  {
								_w.destFloor = k;
								most = floorPopulationMap.get(k);
							}
							floorsWithBlocks[j++] = k;
						}

						//if(floorsWithBlocks.length > 0)
							//_w.destFloor = floorsWithBlocks[(int)random(0,floorsWithBlocks.length)];

						// Second, try to find an elevator that goes to that floor.
						// If one doesn't exist, find the one that gets closest
						Elevator _e;
						Elevator targetElev = null;
						for(int e=0; e<elevators.size(); e++) {
							_e = elevators.get(e);
							if(_e.servesFloor(_w.currFloor) && _e.servesFloor(_w.destFloor)) {
								targetElev = _e;
							}
						}
						if(targetElev != null)
							_w.goToElevator(targetElev);
					}

				}
				else {
					println("Worker #" + i + " is in state " + _w.state + " velocity " + _w.dPosition.length2() );
				}
			}
		}

		for(int i=0; i<arms.size(); i++) {
			arms.get(i).update();
		}

		for(int i=0; i<elevators.size(); i++) {
			elevators.get(i).update();
		}

		for(int i=0; i<workers.size(); i++) {
			workers.get(i).update();
		}

		for(int i=0; i<blocks.size(); i++) {
			blocks.get(i).update();
		}
	}

	public void xformBlock(Block b) {
		int n = grid.pixelToGrid(b.position)[0]*4-cam.width/2-(grid.pixelToGrid(b.position)[1]*4-3*cam.height/4)*cam.width;
		if(n>=0 && n+b.xformedColors.length+b.xformedColors[0].length*cam.width<cam.width*cam.height) {
			if(cam.available()) cam.read();
			cam.loadPixels();
			b.blockColor = cam.pixels[n];
			for(int i=0; i<b.xformedColors.length; i++) {
				for(int j=0; j<b.xformedColors[i].length; j++) {
					b.xformedColors[i][j] = cam.pixels[n+i+j*cam.width];
				}
			}
		}
		else {
			b.blockColor = color(0);
		}
		b.setXFormed();
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
		if(key == 'a') {

			/*
			if(cam.available()) cam.read();
			cam.loadPixels();
			Block nb;
			for(int i=0; i<cam.width*cam.height; i++) {
				nb = new Block(this, grid.gridToPixel(-i%cam.width + (int)cam.width/2, -(int)(i/cam.width) + (int)cam.height/2));
				nb.blockColor = cam.pixels[i];
				blocks.add(nb);
			}
			*/


			for(int i=-cam.width/8; i<cam.width/8; i++) {
				for(int j=-cam.height/8; j<cam.height/8; j++) {
					blocks.add(new Block(this, grid.gridToPixel(i,j + 6)));
				}
			}


		}
		if(key == '1') {
			elevators.get(0).call(1, 1);
		}
		if(key == '5') {
			elevators.get(0).call(5, 1);
		}
		if(key == '4') {
			elevators.get(0).call(4, -1);
		}
		if(key == '6')
			elevators.get(0).call(6,1);
		if(key == '0')
			elevators.get(0).call(10,1);
		if(key == '9')
			elevators.get(0).call(10,-1);
	}


	public static void main(String args[]) {
		PApplet.main(new String[] {"kronick.sandboxsystem.SystemTwo" });
	}
}
