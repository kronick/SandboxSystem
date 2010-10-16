package kronick.sandboxsystem;

public class XY
{
  //NumberFormat f = new DecimalFormat("+000.00;-000.00");

  public double x;
  public double y;

  XY(double ix, double iy) {
    this.x = ix;
    this.y = iy;
  }
  XY(XY p) {
    this.x = p.x;
    this.y = p.y;
  }

  XY set(double ix, double iy) {
    this.x = ix;
    this.y = iy;
    return this;
  }

  XY translate(double dx, double dy) {
    this.x += dx;
    this.y += dy;
    return this;
  }

  XY translate(XY d) {
    this.x += d.x;
    this.y += d.y;
    return this;
  }

  XY subtract(XY d) {
    this.x -= d.x;
    this.y -= d.y;
    return this;
  }
  XY subtract(double dx, double dy) {
    this.x -= dx;
    this.y -= dy;
    return this;
  }

  double distance(XY a) {
    return this.distance(this, a);
  }
  double distance(XY a, XY b) {
    a = new XY(a);
    a.subtract(b);
    return a.length();
  }

  double distance2(XY a) {
    return (Math.pow((this.x - a.x), 2) + Math.pow((this.y - a.y), 2));
  }

  XY scale(double k) {
    this.x *= k;
    this.y *= k;
    return this;
  }

  String text() {
    return "(" + this.x + ", " + this.y + ")";
  }

  double length() {
    return Math.sqrt(this.x*this.x + this.y*this.y);
  }

  XY normalize() {
    if(this.length() > 0) {
      this.scale(1/this.length());
    }
    return this;
  }

  XY get() {
	  return new XY(this);
  }

  boolean equals(XY b) {
	  if(b.x == this.x && b.y == this.y) return true; else return false;
  }
}