package org.jglrxavpok.jlsl.glsl;

public class Vertex {

	private final Vec3 pos = new Vec3(1, 1, 1);

	private final Vec2 texCoords = new Vec2(0, 0);

	public double test(double v) {
		// return pos.x += 1; TODO: DUP2_X1
		this.pos.x += v;
		return this.pos.x;
	}
}
