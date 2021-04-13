package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.glsl.GLSL.Substitute;

public class Mat4 {

	private final double[] data;

	public Mat4(Vec4 column1, Vec4 column2, Vec4 column3, Vec4 column4) {
		this.data = new double[4 * 4];
		this.data[0] = column1.x;
		this.data[1] = column1.y;
		this.data[2] = column1.z;
		this.data[3] = column1.w;

		this.data[4] = column2.x;
		this.data[5] = column2.y;
		this.data[6] = column2.z;
		this.data[7] = column2.w;

		this.data[8] = column3.x;
		this.data[9] = column3.y;
		this.data[10] = column3.z;
		this.data[11] = column3.w;

		this.data[12] = column4.x;
		this.data[13] = column4.y;
		this.data[14] = column4.z;
		this.data[15] = column4.w;
	}

	@Substitute (value = "*", ownerBefore = true, usesParenthesis = false)
	public Vec4 mul(Vec4 m) {
		return null;
	}

	@Substitute (value = "*", ownerBefore = true, usesParenthesis = false)
	public Mat4 mul(Mat4 m) {
		return null; // TODO: Implement
	}
}
