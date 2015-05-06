package tests.handleStmtTests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import analyzer.level2.HandleStmtForTests;
import analyzer.level2.SecurityLevel;

public class IfStmtSuccess {
	
	@Before
	public void init() {
		HandleStmtForTests.init();
	}

	@Test
	public void ifStmtSimpleTest() {
		
		System.out.println("SIMPLE IF STMT TEST STARTED");
		
		HandleStmtForTests hs = new HandleStmtForTests();
		hs.addObjectToObjectMap(this);
		hs.addLocal("int_x");
		
		hs.assignLocalsToLocal("int_x");
		int x = 1;
		assertEquals(SecurityLevel.LOW, hs.getLocalLevel("int_x"));

		assertEquals(SecurityLevel.LOW, hs.getLocalPC());
		assertEquals(SecurityLevel.LOW, hs.getGlobalPC());	
		
		hs.checkCondition("int_x");
		if (x == 1) {
			assertEquals(SecurityLevel.LOW, hs.getLocalPC());
			assertEquals(SecurityLevel.LOW, hs.getGlobalPC());	
			
			hs.makeLocalHigh("int_x");
			
			hs.checkCondition("int_x");
			if (x == 1) {

				assertEquals(SecurityLevel.HIGH, hs.getLocalPC());
				assertEquals(SecurityLevel.HIGH, hs.getGlobalPC());	
				
				hs.exitInnerScope();
			}
			
			assertEquals(SecurityLevel.LOW, hs.getLocalPC());
			assertEquals(SecurityLevel.LOW, hs.getGlobalPC());	
			
			hs.exitInnerScope();
		}
		

		assertEquals(SecurityLevel.LOW, hs.getLocalPC());
		assertEquals(SecurityLevel.LOW, hs.getGlobalPC());	
		
		hs.makeLocalHigh("int_x");
		
		hs.checkCondition("int_x");
		if (x == 1) {

			assertEquals(SecurityLevel.HIGH, hs.getLocalPC());
			assertEquals(SecurityLevel.HIGH, hs.getGlobalPC());	
			
			hs.exitInnerScope();
		}
		

		assertEquals(SecurityLevel.LOW, hs.getLocalPC());
		assertEquals(SecurityLevel.LOW, hs.getGlobalPC());	
		
		hs.makeLocalLow("int_x");
		hs.setLocalPC(SecurityLevel.HIGH);
		hs.pushGlobalPC(SecurityLevel.HIGH);
		
		hs.checkCondition("int_x");
		if (x == 1) {

			assertEquals(SecurityLevel.HIGH, hs.getLocalPC());
			assertEquals(SecurityLevel.HIGH, hs.getGlobalPC());	
			
			hs.exitInnerScope();
		}
		
		assertEquals(SecurityLevel.HIGH, hs.getLocalPC());
		assertEquals(SecurityLevel.HIGH, hs.getGlobalPC());
		
		hs.close();
		
		System.out.println("SIMPLE IF STMT TEST FINISHED");
		
	}
	
	@Test
	public void ifStmt2Test() {
		
		int x = 1;
		
		if ( x == 1) {
			x = 2;
		}
		
	}
	
	@Test
	public void ifStmt3Test() {
		
		int x = 1;
		int y = 2;
		
		if ( x == y) {
			x = 2;
		}
		
	}

}