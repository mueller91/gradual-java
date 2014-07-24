package junitConstraints;

import static security.Definition.*;
import security.Definition.FieldSecurity;

public class FailArray {

	public static void main(String[] args) {}

	@FieldSecurity({ "low", "low" })
	public int[] low;

	@FieldSecurity({ "high", "high" })
	public int[] high;

	@FieldSecurity({ "low", "low", "low"})
	public int[][] lowLow;

	@FieldSecurity({ "low", "low", "high" })
	public int[][] lowHigh;

	@FieldSecurity({ "high", "high", "high" })
	public int[][] highHigh;

	@Constraints({ "low <= @return" })
	public int test1() {
		// @security("return is high")
		return high[0];
	}

	@Constraints({ "low <= @return" })
	public int test2() {
		int[] arr = arrayIntHigh(1);
		// @security("return is high")
		return arr[0];
	}

	public void test3() {
		int h = mkHigh(1);
		int[] arr = arrayIntLow(h);
	}
	

}