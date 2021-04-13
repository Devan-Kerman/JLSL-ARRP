package classes;

public class TestStruct {
	public final int fieldA;

	public TestStruct(int a) {
		this.fieldA = a;
	}

	public int getFieldASquared() {
		return this.fieldA * this.fieldA;
	}
}
