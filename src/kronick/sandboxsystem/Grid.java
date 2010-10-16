package kronick.sandboxsystem;

import processing.core.*;

public class Grid {
	SandboxSystem parent;
	int divisionX, divisionY;

	final int EDGE = 0;
	final int CENTER = 1;

	PGraphics bg;
	PGraphics gradient;

	private float lastzoom = 0;
	private XY lastcenter = new XY(0,0);

	Grid(SandboxSystem parent) {
		this.parent = parent;
		this.divisionX = parent.GRID_SIZE;
		this.divisionY = parent.GRID_SIZE;
		bg = parent.createGraphics(parent.width,parent.height, parent.P2D);
		gradient = parent.createGraphics(parent.width,parent.height, parent.P2D);

		gradient.beginDraw();
		gradient.colorMode(bg.HSB);
		// Pre-render gradient
		gradient.pushMatrix();
			gradient.translate(bg.width/2, bg.height/2);
			for(int x=-bg.width/2; x<=bg.width/2; x++) {
				for(int y=-bg.height/2; y<=bg.height/2; y++) {
					gradient.stroke(150, 150, (float)(150 - .3*Math.sqrt(Math.pow(x,2)/6 + Math.pow(y,2)/3)));
					gradient.point(x,y);
				}
			}
		gradient.popMatrix();
		gradient.endDraw();

		regenerateBackground(parent.zoom, new XY(0,0));
	}

	public void regenerateBackground(float zoomlevel, XY center) {
		// Read the image from the "cache" if the view hasn't changed
		if(zoomlevel != lastzoom || !center.equals(lastcenter)) {
			bg.beginDraw();
			bg.colorMode(bg.HSB);
			bg.smooth();
			bg.translate(bg.width/2, bg.height/2);

			bg.imageMode(bg.CENTER);
			bg.image(gradient, 0,0);

			// Draw grid lines
			bg.stroke(0,0,255,40);
			bg.strokeWeight(1);
			bg.pushMatrix();
			for(int i=(int)(-bg.width/divisionX/2/zoomlevel); i<bg.width/divisionX/2/zoomlevel; i++) {
				bg.line((float)(i*divisionX*zoomlevel + (center.x*zoomlevel)%(divisionX*zoomlevel)), -bg.height/2,
						(float)(i*divisionX*zoomlevel + (center.x*zoomlevel)%(divisionX*zoomlevel)), bg.height/2);
			}
			for(int i=(int)(-bg.height/divisionY/2/zoomlevel); i<=bg.height/divisionY/2/zoomlevel; i++) {
				bg.line(-bg.width/2, (float)(i*divisionY*zoomlevel + (center.y*zoomlevel)%(divisionY*zoomlevel)),
						 bg.width/2, (float)(i*divisionY*zoomlevel + (center.y*zoomlevel)%(divisionY*zoomlevel)));
			}
			bg.popMatrix();
			bg.endDraw();
		}
		lastzoom = zoomlevel;
		lastcenter = new XY(center);
	}

	public void draw(float zoom, XY center) {
		parent.pushStyle();

		// Copy prerendered bg and grid
		parent.imageMode(parent.CENTER);
		parent.pushMatrix();
			parent.scale(1/zoom);
			parent.translate(-(float)center.x * zoom, -(float)center.y* zoom);
			parent.image(bg, 0,0);
		parent.popMatrix();

		parent.popStyle();
	}

	XY gridToPixel(int[] g) {
		if(g.length == 2) return gridToPixel(g[0], g[1]);
		else return null;
	}
	XY gridToPixel(int x, int y) {
		return new XY(divisionX * x + divisionX/2., -(divisionY * y + divisionY/2.));
	}

	XY quantize(XY pixel) { return this.quantize(pixel, CENTER); }
	XY quantize(XY pixel, int snap) {
		if(snap == CENTER) {
			int[] grid = pixelToGrid(pixel);
			return gridToPixel(grid);
		}
		else if(snap == EDGE) {
			XY out = new XY((int)Math.round(pixel.x / divisionX) * divisionX,
							(int)Math.round(pixel.y / divisionY) * divisionY);
			return out;
		}
		else return null;
	}

	XY quantizedOffset(int x, int y, int dx, int dy) {
		XY start = gridToPixel(x, y);
		start.translate(dx * divisionX, dy * divisionY);
		return start;
	}

	int[] pixelToGrid(XY pixel) {
		int[] out = new int[2];
		out[0] = (int)Math.floor(pixel.x / divisionX);
		out[1] = -(int)Math.ceil((pixel.y) / divisionY);
		return out;
	}
}
