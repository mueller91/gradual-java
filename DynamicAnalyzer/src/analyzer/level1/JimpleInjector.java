package analyzer.level1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import logging.L1Logger;
import logging.L2Logger;
import exceptions.IllegalFlowException;
import analyzer.level1.storage.UnitStore;
import analyzer.level1.storage.LocalStore;
import analyzer.level1.storage.UnitStore.Element;
import analyzer.level2.SecurityLevel;
import analyzer.level2.storage.ObjectMap;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootField;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.VoidType;
import soot.jimple.AddExpr;
import soot.jimple.Expr;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.util.Chain;

public class JimpleInjector {
	
	private final static String HANDLE_CLASS = "analyzer.level2.HandleStmt";


	
	static Body b = Jimple.v().newBody();
    static Chain<Unit> units = b.getUnits();
    static Chain<Local> locals = b.getLocals();
  
    static UnitStore unitStore = new UnitStore();
    static LocalStore localStore = new LocalStore();

	static Local hs = Jimple.v().newLocal("hs", RefType.v(HANDLE_CLASS));

	// Locals needed to add Locals to Map
	static Local local1 = Jimple.v().newLocal("local_name1", RefType.v("java.lang.String"));
	static Local local2 = Jimple.v().newLocal("local_name2", RefType.v("java.lang.String"));
	static Local local3 = Jimple.v().newLocal("local_name3", RefType.v("java.lang.String"));
	static Local level = Jimple.v().newLocal("local_level", RefType.v("java.lang.String"));
	
	static Logger LOGGER = L1Logger.getLogger();
	
	public static void setBody(Body body) {
		b = body;
		units = b.getUnits();
		locals = b.getLocals();
	}
	
	
	public static void invokeHS() {
		LOGGER.log(Level.INFO, "Invoke HandleStmt in method {0}", b.getMethod().getName());
		
		locals.add(hs);
		Unit in = Jimple.v().newAssignStmt(hs, Jimple.v().newNewExpr(RefType.v(HANDLE_CLASS)));
		ArrayList<Type> paramTypes = new ArrayList<Type>();
		Expr specialIn = Jimple.v().newSpecialInvokeExpr(
				hs, Scene.v().makeConstructorRef(Scene.v().getSootClass(HANDLE_CLASS), paramTypes));
		
		Iterator<Unit> it = units.iterator();
		Unit pos = null;
		
		int numOfArgs = getStartPos();
		for(int i = 0; i < numOfArgs; i++) {
			pos = it.next();
		}
		
		unitStore.insertElement(unitStore.new Element(in, pos)); 
		unitStore.lastPos = in;
		Unit inv = Jimple.v().newInvokeStmt(specialIn);
		unitStore.insertElement(unitStore.new Element(inv, unitStore.lastPos));
		unitStore.lastPos = inv;
	}
	
	public static void addLocal(Local l) {
		LOGGER.log(Level.INFO, "Add Local {0} in method {1}",new Object[] {
				getSignatureForLocal(l), b.getMethod().getName()});
		
		ArrayList<Type> paramTypes = new ArrayList<Type>();
		paramTypes.add(RefType.v("java.lang.String"));
		
		String signature = getSignatureForLocal(l);
	    Stmt sig = Jimple.v().newAssignStmt(local1, StringConstant.v(signature));
		
		Expr invokeAddLocal = Jimple.v().newVirtualInvokeExpr(
				hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS), 
						"addLocal", paramTypes, VoidType.v(),  false), local1);
		Unit ass = Jimple.v().newInvokeStmt(invokeAddLocal);
		

	    unitStore.insertElement(unitStore.new Element(sig, unitStore.lastPos));
	    unitStore.lastPos = sig;
		unitStore.insertElement(unitStore.new Element(ass, unitStore.lastPos));
		unitStore.lastPos = ass;
	}
  
	public static void initHS() {
		LOGGER.log(Level.INFO, "Initialize HandleStmt in method {0}", b.getMethod().getName());
		
		ArrayList<Type> paramTypes = new ArrayList<Type>();
		Expr invokeInit = Jimple.v().newStaticInvokeExpr(
				Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS), 
						"init", paramTypes, VoidType.v(), true));
		Unit init = Jimple.v().newInvokeStmt(invokeInit);
		unitStore.insertElement(unitStore.new Element(init, unitStore.lastPos));
		unitStore.lastPos = init;
	}

	public static void closeHS() {
		LOGGER.log(Level.INFO, "Close HandleStmt in method {0} {1}", 
				new Object[] {b.getMethod().getName(), System.getProperty("line.separator")});
		
		ArrayList<Type> paramTypes = new ArrayList<Type>();
		Expr invokeClose = Jimple.v().newVirtualInvokeExpr(
				hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS), 
						"close", paramTypes, VoidType.v(), false));
		units.insertBefore(Jimple.v().newInvokeStmt(invokeClose), units.getLast());
	}
	
	public static void getActualReturnLevel() {}
	
	public static void addObjectToObjectMap(Object o) {}
	
	public static void addFieldToObjectMap(Object o, String signature) {}
	
	public static void makeFieldHigh(Object o, String signature) {}
	
	public static void makeFieldLow(Object o, String signature) {}
	
	public static void addLocal(String signature, SecurityLevel level) {}
	
	public static void setLocalLevel(String signature, SecurityLevel level) {}
	
	public static void getLocalLevel(String signature) {}
	
	public static void makeLocalHigh(Local l) {
		LOGGER.log(Level.INFO, "Make Local {0} high in method {1}",
				new Object[] {getSignatureForLocal(l), b.getMethod().getName()});
		
		ArrayList<Type> paramTypes = new ArrayList<Type>();
		paramTypes.add(RefType.v("java.lang.String"));
		
		String signature = getSignatureForLocal(l);
	    Stmt sig = Jimple.v().newAssignStmt(local1, StringConstant.v(signature));
		
		Expr invokeAddLocal = Jimple.v().newVirtualInvokeExpr(
				hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS), 
						"makeLocalHigh", paramTypes, VoidType.v(),  false), local1);
		Unit ass = Jimple.v().newInvokeStmt(invokeAddLocal);
		

	    unitStore.insertElement(unitStore.new Element(sig, unitStore.lastPos));
	    unitStore.lastPos = sig;
		unitStore.insertElement(unitStore.new Element(ass, unitStore.lastPos));
		unitStore.lastPos = ass;
	}
	
	public static void makeLocalLow(Local l) {
		LOGGER.log(Level.INFO, "Make Local {0} low in method {1}",
				new Object[] {getSignatureForLocal(l), b.getMethod().getName()});
		
		ArrayList<Type> paramTypes = new ArrayList<Type>();
		paramTypes.add(RefType.v("java.lang.String"));
		
		String signature = getSignatureForLocal(l);
	    Stmt sig = Jimple.v().newAssignStmt(local1, StringConstant.v(signature));
		
		Expr invokeAddLocal = Jimple.v().newVirtualInvokeExpr(
				hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS), 
						"makeLocalLow", paramTypes, VoidType.v(),  false), local1);
		Unit ass = Jimple.v().newInvokeStmt(invokeAddLocal);
		

	    unitStore.insertElement(unitStore.new Element(sig, unitStore.lastPos));
	    unitStore.lastPos = sig;
		unitStore.insertElement(unitStore.new Element(ass, unitStore.lastPos));
		unitStore.lastPos = ass;
	}

	public static void assignLocalsToField(Object o, String field, String... locals) {}
	
	public static void assignLocalsToLocal(Local leftOp, Local rightOp) { // TODO 2 Locals?
		//assignLocalsToLocal(String leftOp, String... rightOp)
		LOGGER.log(Level.INFO, "Assign Local {0} to Local {1} in method {2}",
				new Object[] {getSignatureForLocal(leftOp), 
				getSignatureForLocal(rightOp), b.getMethod().getName()});
		
		ArrayList<Type> paramTypes = new ArrayList<Type>();
		paramTypes.add(RefType.v("java.lang.String"));
		paramTypes.add(RefType.v("java.lang.String"));
		
		String signatureLeft = getSignatureForLocal(leftOp);
		String signatureRight = getSignatureForLocal(rightOp);
		
	    Stmt sigLeft = Jimple.v().newAssignStmt(local1, StringConstant.v(signatureLeft));
	    Stmt sigRight = Jimple.v().newAssignStmt(local2, StringConstant.v(signatureRight));
		
		Expr invokeAddLocal = Jimple.v().newVirtualInvokeExpr(
				hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS), 
						"makeLocalLow", paramTypes, VoidType.v(),  false), local1, local2);
		Unit ass = Jimple.v().newInvokeStmt(invokeAddLocal);
		

	    unitStore.insertElement(unitStore.new Element(sigLeft, unitStore.lastPos));
	    unitStore.lastPos = sigLeft;
	    unitStore.insertElement(unitStore.new Element(sigRight, unitStore.lastPos));
	    unitStore.lastPos = sigRight;
		unitStore.insertElement(unitStore.new Element(ass, unitStore.lastPos));
		unitStore.lastPos = ass;
	}
	
	public static void assignConstantToLocal(Local leftOp) { // TODO mit der anderen Methode mergen?
		//assignLocalsToLocal(String leftOp, String... rightOp)
		LOGGER.log(Level.INFO, "Assign Constant to Local {0} in method {1}",
				new Object[] {getSignatureForLocal(leftOp), b.getMethod().getName()});
		
		ArrayList<Type> paramTypes = new ArrayList<Type>();
		paramTypes.add(RefType.v("java.lang.String"));
		paramTypes.add(ArrayType.v(RefType.v("java.lang.String"), 1));
		
		String signatureLeft = getSignatureForLocal(leftOp);

	    //  $r2 = newarray (java.lang.String)[0];
	    Expr strArr = Jimple.v().newNewArrayExpr(RefType.v("java.lang.String"), IntConstant.v(0));
		
	    Stmt sigLeft = Jimple.v().newAssignStmt(local1, StringConstant.v(signatureLeft));
	    Stmt right = Jimple.v().newAssignStmt(local2, strArr);
		
		Expr invokeAddLocal = Jimple.v().newVirtualInvokeExpr(
				hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS), 
						"assignLocalsToLocal", paramTypes, RefType.v("analyzer.level2.SecurityLevel"),  false), local1, local2);
		Unit ass = Jimple.v().newInvokeStmt(invokeAddLocal);
		

	    unitStore.insertElement(unitStore.new Element(sigLeft, unitStore.lastPos));
	    unitStore.lastPos = sigLeft;    
	    unitStore.insertElement(unitStore.new Element(right, unitStore.lastPos));
	    unitStore.lastPos = right;
		unitStore.insertElement(unitStore.new Element(ass, unitStore.lastPos));
		unitStore.lastPos = ass;
	}
	
	public static void assignFieldToLocal(Object o, String local, String field) {}
	
	public static void assignArgumentToLocal(int pos, String local) {}
	
	public static void returnConstant() {}

	public static void returnLocal(String signature) {}

	public static void storeArgumentLevels(String... arguments) {}
	
	public static void checkCondition(String... args) {}
	
	public static void exitInnerScope() {}
	
	/*
public static void Join(String resStr, String lO, String rO, Unit pos) {

    Local res = Jimple.v().newLocal("res", RefType.v("java.lang.String"));
    Local leftOp = Jimple.v().newLocal("leftOp", RefType.v("java.lang.String"));
    Local rightOp = Jimple.v().newLocal("rightOp", RefType.v("java.lang.String"));
    
    


    locals.add(res);
    locals.add(leftOp);
    locals.add(rightOp);
    
    Stmt l1 = Jimple.v().newAssignStmt(res, StringConstant.v(resStr));
    Stmt l2 = Jimple.v().newAssignStmt(leftOp, StringConstant.v(lO));
    Stmt l3 = Jimple.v().newAssignStmt(rightOp, StringConstant.v(rO));
    
    unitStore.insertElement(unitStore.new Element(l1, pos));
    unitStore.insertElement(unitStore.new Element(l2, pos));
    unitStore.insertElement(unitStore.new Element(l3, pos));
    
	    
    ArrayList<Type> paramTypes3 = new ArrayList<Type>();
   	paramTypes3.add(RefType.v("java.lang.String"));
   	paramTypes3.add(RefType.v("java.lang.String"));
   	paramTypes3.add(RefType.v("java.lang.String"));
   	
   	ArrayList<Local> params3 = new ArrayList<Local>();
   	params3.add(res);
   	params3.add(leftOp);
   	params3.add(rightOp);
   	
   	
   	SootMethodRef methodtI3 = Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS), "Join", paramTypes3, VoidType.v(), true);
    Expr methodInvoke3 = Jimple.v().newStaticInvokeExpr(methodtI3, params3);

    unitStore.insertElement(unitStore.new Element(Jimple.v().newInvokeStmt(methodInvoke3), pos));

    b.validate();
  }
  */

  /**
 * @param stmt
 * @param def
 * @param use
 * 
 */
/*
public static void invokeHandleStmtUnit( Unit stmt, List<ValueBox> def, List<ValueBox> use) {
	  System.out.println("invokeHandleStmt");
	  System.out.print("Definition Box: " + def);
	  System.out.println(" Use Box: " + use);
	  
	  Iterator<ValueBox> ubIt = use.iterator();
	  while (ubIt.hasNext()) {
	  ValueBox vb = (ValueBox) ubIt.next();
	  Value v = vb.getValue();
	  if (v instanceof AddExpr ) {
		  Local lO = (Local) ((AddExpr) v).getOp1();
		  Local rO = (Local) ((AddExpr) v).getOp2();
		  Local res = (Local) def.get(0).getValue(); // TODO : das geschickter machen
		  
		  String lOStr = getSignatureForLocal(lO);
		  String rOStr = getSignatureForLocal(rO);
		  String resStr = getSignatureForLocal(res);
		  // TODO ich habe ein Problem damit, dass handleStmt in die Unit chain schreibt und dadurch eine Exception auslöst
		   Join(resStr, lOStr, rOStr, stmt);
		  
	  }
	  }
	  
	  b.validate();
  }
  
  /*
	public static void addFieldToMap(SootField item, Level level) {
	    Local field = Jimple.v().newLocal("field", RefType.v("java.lang.String"));
	    Local levelStr = Jimple.v().newLocal("level", RefType.v("java.lang.String"));
	    locals.add(field);
	    locals.add(levelStr); // TODO hier das enum einsetzen
	    Stmt l1 = Jimple.v().newAssignStmt(field, StringConstant.v(getSignatureForField(item))); 
	    Stmt l2 = Jimple.v().newAssignStmt(field, StringConstant.v(getSignatureForField(item)));    
	    unitStore.insertElement(unitStore.new Element(l1, item));        
	    unitStore.insertElement(unitStore.new Element(l2, units.getFirst())); 
		    
	    ArrayList paramTypes3 = new ArrayList();
	   	paramTypes3.add(RefType.v("java.lang.String"));
	   	paramTypes3.add(RefType.v("java.lang.String"));
	   	
	   	ArrayList<Local> params3 = new ArrayList();
	   	params3.add(field);
	   	params3.add(levelStr);
	   	
	   	
	   	SootMethodRef methodtI3 = Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS), "addField", paramTypes3, VoidType.v(), true);
	    Expr methodInvoke3 = Jimple.v().newStaticInvokeExpr(methodtI3, params3);

	    unitStore.insertElement(unitStore.new Element(Jimple.v().newInvokeStmt(methodInvoke3), units.getFirst()));

	    b.validate();
		
	}
  */
  

/*
	public static void addLocalToMap(Local item) {
	    Stmt l1 = Jimple.v().newAssignStmt(local, StringConstant.v(getSignatureForLocal(item))); 
	    Stmt l2 = Jimple.v().newAssignStmt(level, StringConstant.v("High"));    
	    unitStore.insertElement(unitStore.new Element(l1, units.getFirst()));        
	    unitStore.insertElement(unitStore.new Element(l2, units.getFirst())); 
		    
	    ArrayList<Type> paramTypes3 = new ArrayList<Type>();
	   	paramTypes3.add(RefType.v("java.lang.String"));
	   	paramTypes3.add(RefType.v("java.lang.String"));
	   	
	   	ArrayList<Local> params3 = new ArrayList<Local>();
	   	params3.add(item);
	   	params3.add(level);
	   	
	   	
	   	SootMethodRef methodtI3 = Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS), "addLocal", paramTypes3, VoidType.v(), true);
	    Expr methodInvoke3 = Jimple.v().newStaticInvokeExpr(methodtI3, params3);

	    unitStore.insertElement(unitStore.new Element(Jimple.v().newInvokeStmt(methodInvoke3), units.getFirst()));

	    b.validate();	
	}


*/
	
public static void addUnitsToChain() {	
	Iterator<Element> UIt = unitStore.getElements().iterator();
	while(UIt.hasNext()) {
		Element item = (Element) UIt.next();
		if (item.getPosition() == null) {
			units.addFirst(item.getUnit());
		} else {
		    units.insertAfter(item.getUnit(), item.getPosition()); 
		}
	}
	
	unitStore.flush();
	b.validate();
}

public static void addNeededLocals() {
	locals.add(local1);
	locals.add(local2);
	locals.add(local3);
	locals.add(level);	
}

private static String getSignatureForLocal(Local l) {
	return l.getType() + "_" + l.getName();
}

private static String getSignatureForField(SootField f) {
	return f.getType() + "_" + f.getName();
}

private static int getStartPos() {
	if (b.getMethod().isConstructor()) {
		return 1;
	} else {
		return b.getMethod().getParameterCount();
	}
}

}
