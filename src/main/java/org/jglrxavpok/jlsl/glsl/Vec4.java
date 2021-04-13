package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.glsl.GLSL.Substitute;

public class Vec4 {
	public double x;
	public double y;
	public double z;
	public double w;

	public Vec4(double x, double y, double z, double w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public Vec4(double x, double y, Vec2 zw) {
		this.z = zw.x;
		this.w = zw.y;
		this.x = x;
		this.y = y;
	}

	public Vec4(double x, Vec2 yz, double w) {
		this.y = yz.x;
		this.z = yz.y;
		this.x = x;
		this.w = w;
	}

	public Vec4(Vec2 xy, double z, double w) {
		this.x = xy.x;
		this.y = xy.y;
		this.z = z;
		this.w = w;
	}

	public Vec4(Vec3 xyz, double w) {
		this.x = xyz.x;
		this.y = xyz.y;
		this.z = xyz.z;
		this.w = w;
	}

	public Vec4 normalize() {
		double l = this.length();
		double x1 = this.x / l;
		double y1 = this.y / l;
		double z1 = this.z / l;
		double w1 = this.w / l;
		return new Vec4(x1, y1, z1, w1);
	}

	public double length() {
		double dx = this.x;
		double dy = this.y;
		double dz = this.z;
		double dw = this.w;
		return Math.sqrt(dx * dx + dy * dy + dz * dz + dw * dw);
	}

	@Substitute (value = "+", usesParenthesis = false, ownerBefore = true)
	public Vec4 add(Vec4 v) {
		return new Vec4(this.x + v.x, this.y + v.y, this.z + v.z, this.w + v.w);
	}

	@Substitute (value = "-", usesParenthesis = false, ownerBefore = true)
	public Vec4 sub(Vec4 v) {
		return new Vec4(this.x - v.x, this.y - v.y, this.z - v.z, this.w - v.w);
	}

	@Substitute (value = "*", usesParenthesis = false, ownerBefore = true)
	public Vec4 mul(double d) {
		return new Vec4(this.x * d, this.y * d, this.z * d, this.w * d);
	}
}
