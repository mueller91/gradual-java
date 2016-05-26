package classfiletests;

import classfiletests.utils.ClassRunner;
import org.junit.Test;
import utils.logging.L1Logger;

import java.util.logging.Logger;


public class InvokeLibMethodTest {

	Logger logger = L1Logger.getLogger();

	@Test
	public void test() {
		logger.info("Start of executing main.testclasses.InvokeLibMethod");

		ClassRunner.testClass("InvokeLibMethod", true);
		logger.info("Finished executing main.testclasses.InvokeLibMethod");
	}
}
