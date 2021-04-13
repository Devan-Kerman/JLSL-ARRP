package org.jglrxavpok.jlsl.glsl;

public class Vec3 {

	public double x;
	public double y;
	public double z;

	public Vec3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vec3 normalize() {
		double l = this.length();
		double x1 = this.x / l;
		double y1 = this.y / l;
		double z1 = this.z / l;
		return new Vec3(x1, y1, z1);
	}

	public double length() {
		double dx = this.x;
		double dy = this.y;
		double dz = this.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

}
