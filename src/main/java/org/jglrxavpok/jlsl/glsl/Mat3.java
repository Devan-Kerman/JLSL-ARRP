package org.jglrxavpok.jlsl.glsl;

public class Mat3 {

	private final double[] data;

	public Mat3(Vec3 column1, Vec3 column2, Vec3 column3) {
		this.data = new double[3 * 3];
		this.data[0] = column1.x;
		this.data[1] = column1.y;
		this.data[2] = column1.z;

		this.data[3] = column2.x;
		this.data[4] = column2.y;
		this.data[5] = column2.z;

		this.data[6] = column3.x;
		this.data[7] = column3.y;
		this.data[8] = column3.z;
	}
}
