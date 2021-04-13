package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.glsl.GLSL.Substitute;

public class Vec2 {

	public double x;
	public double y;

	public Vec2(double x, double y) {
		super();
		this.x = x;
		this.y = y;
	}

	public Vec2 normalize() {
		double l = this.length();
		double x1 = this.x / l;
		double y1 = this.y / l;
		return new Vec2(x1, y1);
	}

	public double length() {
		double dx = this.x;
		double dy = this.y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	@Substitute (value = "/", usesParenthesis = false, ownerBefore = true)
	public Vec2 div(double i) {
		return new Vec2(this.x / i, this.y / i);
	}

}
