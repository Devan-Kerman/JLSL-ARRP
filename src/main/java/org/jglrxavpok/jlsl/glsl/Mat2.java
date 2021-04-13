package org.jglrxavpok.jlsl.glsl;

public class Mat2 {
	private final double[] data;

	public Mat2(Vec2 column1, Vec2 column2) {
		this.data = new double[2 * 2];
		this.data[0] = column1.x;
		this.data[1] = column1.y;

		this.data[2] = column2.x;
		this.data[3] = column2.y;
	}
}
