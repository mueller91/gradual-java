package analyzer.level2;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import utils.exceptions.IFCError;
import utils.logging.L2Logger;


import java.util.logging.Level;
import java.util.logging.Logger;

public class SwitchStmtFail {
	
	Logger LOGGER = L2Logger.getLogger();
	
	@Before
	public void init() {
		HandleStmt.init();
	}

	@Test(expected = IFCError.class)
	public void switchStmtLowTest() {
		
		LOGGER.log(Level.INFO, "SWITCH STMT FAIL TEST STARTED");
		
		HandleStmt hs = new HandleStmt();
		hs.initHandleStmtUtils(false, 0);
		
		hs.addLocal("int_x", SecurityLevel.top());
		int x = 0;
		hs.addLocal("int_y", SecurityLevel.bottom());
		@SuppressWarnings("unused")
		int y = 0;
		
		assertEquals(SecurityLevel.bottom(), hs.getLocalPC());
		
		hs.checkCondition("123", "int_x");
		switch (x) {
		
		  case 0: 
			  assertEquals(SecurityLevel.top(), hs.getLocalPC()); 
			  hs.checkLocalPC("int_y");
			  hs.setLevelOfLocal("int_y");
			  x += 2;
			  hs.exitInnerScope("123");
			  break;
		  case 1:  
			  assertEquals(SecurityLevel.top(), hs.getLocalPC()); 
			  hs.exitInnerScope("123");
			  break;
		  default:  
			  assertEquals(SecurityLevel.top(), hs.getLocalPC()); 
			  hs.exitInnerScope("123");
			  break;
		} 

		assertEquals(SecurityLevel.bottom(), hs.getLocalPC());

		LOGGER.log(Level.INFO, "SWITCH STMT FAIL TEST FINISHED");
	}


}
