package analyzer.level1;

import analyzer.level1.storage.UnitStore;
import analyzer.level1.storage.UnitStore.Element;
import analyzer.level2.SecurityLevel;
import de.unifreiburg.cs.proglang.jgs.instrumentation.Casts;
import de.unifreiburg.cs.proglang.jgs.instrumentation.CxTyping;
import de.unifreiburg.cs.proglang.jgs.instrumentation.Instantiation;
import de.unifreiburg.cs.proglang.jgs.instrumentation.VarTyping;
import scala.Option;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.util.Chain;
import utils.dominator.DominatorFinder;
import utils.exceptions.InternalAnalyzerException;
import utils.logging.L1Logger;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JimpleInjector is called by the AnnotatorStmtSwitch and AnnotatorValueSwitch.
 * Inserts additional statements into a methods body.
 *
 * @author Regina Koenig (2015)
 */
public class JimpleInjector {

    /**
     * String for the HandleStmt class.
     */
    private static final String HANDLE_CLASS = "analyzer.level2.HandleStmt";


    /**
     * The body of the actually analyzed method.
     */
    private static Body b = Jimple.v().newBody();

    /**
     * Chain with all units in the actual method-body.
     */
    private static Chain<Unit> units = b.getUnits();

    /**
     * Chain with all locals in the actual method-body.
     */
    private static Chain<Local> locals = b.getLocals();

    /**
     * Chain containing all new units which have to be set
     * after a given position.
     */
    private static UnitStore unitStore_After = new UnitStore("UnitStore_After");

    /**
     * Chain containing all new units which have to be set
     * before a given position.
     */
    private static UnitStore unitStore_Before = new UnitStore("UnitStore_Before");

    /**
     * Local which holds the object of HandleStmt.
     */
    private static Local hs = Jimple.v().newLocal("hs", RefType.v(HANDLE_CLASS));

    /**
     * Boolean to check whether the extra locals had already been added.
     */
    private static boolean extralocals = false;

    /**
     * Local wto store String values.
     */
    private static Local local_for_Strings = Jimple.v().newLocal(
            "local_for_Strings", RefType.v("java.lang.String"));

    /**
     * This local is needed for methods
     * with more than two arguments.
     */
    private static Local local_for_Strings2 = Jimple.v().newLocal(
            "local_for_Strings2", RefType.v("java.lang.String"));
    /**
     * This locals is needed for methods
     * with more than two arguments.
     */
    private static Local local_for_Strings3 = Jimple.v().newLocal(
            "local_for_Strings3", RefType.v("java.lang.String"));

    /**
     * Local where String arrays can be stored. Needed to store arguments for injected methods.
     */
    private static Local local_for_String_Arrays = Jimple.v().newLocal(
            "local_for_String_Arrays", ArrayType.v(RefType.v("java.lang.String"), 1));

    /**
     * Local where Objects can be stored as arguments for injected methods.
     */
    private static Local local_for_Objects = Jimple.v().newLocal(
            "local_for_Objects", RefType.v("java.lang.Object"));

    /**
     * Logger.
     */
    private static Logger logger = L1Logger.getLogger();

    /**
     * Stores the position of the last unit which was analyzed in the unit chain or the last
     * inserted unit. Is needed for further units,
     * which have to be inserted after the last position.
     */
    private static Unit lastPos;

    private static Casts casts;

    /**
     * Stores the results of the static analysis. Use Lvel instead of Level because of conflicts with the LEVEL of the Logger.
     */
    private static VarTyping varTyping;
    private static CxTyping cxTyping;
    private static Instantiation instantiation;

    
    /**
     * See method with same name in HandleStatement.
     *
     * @param pos   Statement / Unit where to insert setReturnLevelAfterInvokeStmt
     */
    public static void setReturnLevelAfterInvokeStmt(Unit pos) {
        ArrayList<Type> paramTypes = new ArrayList<>();
        paramTypes.add(RefType.v("java.lang.String"));

        Expr setReturnLevel = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "setReturnLevelAfterInvokeStmt", paramTypes,
                        VoidType.v(),
                        false), local_for_Strings);
        Unit invoke = Jimple.v().newInvokeStmt(setReturnLevel);

        // only add setReturnLevelAfterInvokeStmt if the left side is dynamic
        if ( varTyping.getAfter(instantiation, (Stmt) pos, (Local) ((JAssignStmt) pos).leftBox.getValue() ).isDynamic() ) {
            unitStore_After.insertElement(unitStore_After.new Element(invoke, pos));
        }
    }

    /**
     * Initialization of JimpleInjector. Set all needed variables
     * and compute the start position for inserting new units.
     *
     * @param body The body of the analyzed method.
     */
    public static void setBody(Body body) {
        b = body;
        units = b.getUnits();
        locals = b.getLocals();

        extralocals = false;

        // Insert after the setting of all arguments and the @this-reference,
        // since Jimple would otherwise complain.
        int startPos = getStartPos(body);
        Iterator<Unit> uIt = units.iterator();
        for (int i = 0; i <= startPos; i++) {
            lastPos = uIt.next();
        }

        logger.info("Start position is " + lastPos.toString());

    }

    /**
     * Add "hs = new HandleStmt()" expression to Jimplecode.
     */
    static void invokeHS() {
        logger.log(Level.INFO, "Invoke HandleStmt in method {0}", b.getMethod().getName());

        locals.add(hs);
        Unit in = Jimple.v().newAssignStmt(hs, Jimple.v().newNewExpr(
                RefType.v(HANDLE_CLASS)));
        ArrayList<Type> paramTypes = new ArrayList<>();
        Expr specialIn = Jimple.v().newSpecialInvokeExpr(
                hs, Scene.v().makeConstructorRef(
                        Scene.v().getSootClass(HANDLE_CLASS), paramTypes));

        Unit inv = Jimple.v().newInvokeStmt(specialIn);

        unitStore_Before.insertElement(unitStore_Before.new Element(inv, lastPos));
        unitStore_Before.insertElement(unitStore_Before.new Element(in, inv));
        lastPos = inv;
    }

    static void initHandleStmtUtils(boolean controllerIsActive, int expectedException) {
        logger.log(Level.INFO, "Set Handle Stmt Utils and aktive/passive Mode of superfluous instrumentation checker");


        ArrayList<Type> paramTypes = new ArrayList<>();
        paramTypes.add(BooleanType.v());
        paramTypes.add(IntType.v());

        Expr invHS = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "initHandleStmtUtils",
                        paramTypes, VoidType.v(), false),
                IntConstant.v(controllerIsActive ? 1 : 0),
                IntConstant.v(expectedException)
        );
        Unit inv = Jimple.v().newInvokeStmt(invHS);

        unitStore_After.insertElement(unitStore_After.new Element(inv, lastPos));
        lastPos = inv;
    }


    /**
     * Injects the constructor call of HandleStmt into the analyzed method.
     */
    static void initHS() {
        logger.log(Level.INFO, "Initialize HandleStmt in method {0}",
                b.getMethod().getName());

        ArrayList<Type> paramTypes = new ArrayList<>();
        Expr invokeInit = Jimple.v().newStaticInvokeExpr(
                Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "init", paramTypes, VoidType.v(), true));
        Unit init = Jimple.v().newInvokeStmt(invokeInit);
        unitStore_After.insertElement(unitStore_After.new Element(init, lastPos));
        lastPos = init;
    }

    /**
     * Injects the HandleStmt.close() method. This method should be injected at the
     * end of every analyzed method.
     */
    static void closeHS() {
        logger.log(Level.INFO, "Close HandleStmt in method {0} {1}",
                new Object[]{b.getMethod().getName(),
                        System.getProperty("line.separator")});

        ArrayList<Type> paramTypes = new ArrayList<>();
        Expr invokeClose = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "close", paramTypes, VoidType.v(), false));
        units.insertBefore(Jimple.v().newInvokeStmt(invokeClose), units.getLast());
    }

	/*
     * Adding Elements to map
	 */


    /**
     * Add a new local.
     *
     * @param local The Local
     */
    public static void addLocal(Local local) {

        logger.log(Level.INFO, "Add Local {0} in method {1}", new Object[]{
                getSignatureForLocal(local), b.getMethod().getName()});

        ArrayList<Type> paramTypes = new ArrayList<>();
        paramTypes.add(RefType.v("java.lang.String"));

        Expr invokeAddLocal = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "addLocal", paramTypes, VoidType.v(), false), StringConstant.v(getSignatureForLocal(local)));
        Unit ass = Jimple.v().newInvokeStmt(invokeAddLocal);

        unitStore_After.insertElement(unitStore_After.new Element(ass, lastPos));
        lastPos = ass;
    }

    /**
     * Add the instance of the actual class-object to the object map.
     * This is only done in "init".
     */
    static void addInstanceObjectToObjectMap() {

        // Check if the first unit is a reference to the actual object
        if (!(units.getFirst() instanceof IdentityStmt)
                || !(units.getFirst().getUseBoxes().get(0).getValue()
                instanceof ThisRef)) {
            throw new InternalAnalyzerException("Expected @this reference");
        }

        String thisObj = units.getFirst().getUseBoxes().get(0).getValue().toString();

        logger.log(Level.INFO, "Add object {0} to ObjectMap in method {1}",
                new Object[]{thisObj, b.getMethod().getName()});

        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(RefType.v("java.lang.Object"));

        Expr addObj = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(
                        Scene.v().getSootClass(HANDLE_CLASS), "addObjectToObjectMap",
                        parameterTypes, VoidType.v(), false),
                units.getFirst().getDefBoxes().get(0).getValue());
        Unit assignExpr = Jimple.v().newInvokeStmt(addObj);

        unitStore_After.insertElement(unitStore_After.new Element(assignExpr, lastPos));
        lastPos = assignExpr;

    }

    /**
     * Add a class object. Needed for static fields.
     *
     * @param sc SootClass.
     */
    static void addClassObjectToObjectMap(SootClass sc) {

        logger.log(Level.INFO, "Add object {0} to ObjectMap in method {1}",
                new Object[]{sc.getName(), b.getMethod().getName()});

        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(RefType.v("java.lang.Object"));

        logger.info("Adding class Object:" + sc.getName().replace(".", "/"));
        ClassConstant cc = ClassConstant.v(sc.getName().replace(".", "/"));
        logger.info("Value: " + cc.value);

        Expr addObj = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(
                        Scene.v().getSootClass(HANDLE_CLASS), "addObjectToObjectMap",
                        parameterTypes, VoidType.v(), false),
                ClassConstant.v(sc.getName().replace(".", "/")));
        Unit assignExpr = Jimple.v().newInvokeStmt(addObj);

        unitStore_After.insertElement(unitStore_After.new Element(assignExpr, lastPos));
        lastPos = assignExpr;
    }

    /**
     * Add a field to the object map.
     *
     * @param field the SootField.
     */
    static void addInstanceFieldToObjectMap(SootField field) {
        logger.log(Level.INFO, "Adding field {0} to ObjectMap in method {1}",
                new Object[]{field.getSignature(), b.getMethod().getName()});

        if (!(units.getFirst() instanceof IdentityStmt)
                || !(units.getFirst().getUseBoxes().get(0).getValue()
                instanceof ThisRef)) {
            throw new InternalAnalyzerException("Expected @this reference");
        }

        String fieldSignature = getSignatureForField(field);


        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(RefType.v("java.lang.Object"));
        parameterTypes.add(RefType.v("java.lang.String"));

        Local tmpLocal = (Local) units.getFirst().getDefBoxes().get(0).getValue();

        Unit assignSignature = Jimple.v().newAssignStmt(local_for_Strings,
                StringConstant.v(fieldSignature));


        Expr addObj = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(
                        Scene.v().getSootClass(HANDLE_CLASS), "addFieldToObjectMap",
                        parameterTypes, Scene.v().getObjectType(), false),
                tmpLocal, local_for_Strings);
        Unit assignExpr = Jimple.v().newInvokeStmt(addObj);

        unitStore_After.insertElement(
                unitStore_After.new Element(assignSignature, lastPos));
        unitStore_After.insertElement(
                unitStore_After.new Element(assignExpr, assignSignature));
        lastPos = assignExpr;
    }

    /**
     * Add a static field. This field is added to its corresponding class object.
     *
     * @param field SootField
     */
    static void addStaticFieldToObjectMap(SootField field) {
        logger.info("Adding static Field " + field.toString() + " to Object Map");

        String signature = getSignatureForField(field);

        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(RefType.v("java.lang.Object"));
        parameterTypes.add(RefType.v("java.lang.String"));

        SootClass sc = field.getDeclaringClass();

        Unit assignDeclaringClass = Jimple.v().newAssignStmt(
                local_for_Objects, ClassConstant.v(sc.getName().replace(".", "/")));

        Unit assignSignature = Jimple.v().newAssignStmt(
                local_for_Strings, StringConstant.v(signature));

        Expr addObj = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "addFieldToObjectMap", parameterTypes,
                        Scene.v().getObjectType(), false),
                local_for_Objects, local_for_Strings);
        Unit assignExpr = Jimple.v().newInvokeStmt(addObj);

        unitStore_After.insertElement(
                unitStore_After.new Element(assignDeclaringClass, lastPos));
        unitStore_After.insertElement(
                unitStore_After.new Element(assignSignature, assignDeclaringClass));
        unitStore_After.insertElement(
                unitStore_After.new Element(assignExpr, assignSignature));
        lastPos = assignExpr;

    }

    /**
     * Add a new array to objectMap.
     *
     * @param a   The Local where the array is stored.
     * @param pos Unit where the array occurs.
     */
    public static void addArrayToObjectMap(Local a, Unit pos) {
        logger.log(Level.INFO, "Add array {0} to ObjectMap in method {1}",
                new Object[]{a, b.getMethod().getName()});

        logger.log(Level.INFO, "Object type of array: " + a.getType()
                + " and type " + a.getClass());
        logger.log(Level.FINEST, "at position {0}", pos.toString());

        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(ArrayType.v(
                RefType.v("java.lang.Object"), 1));


        Expr addObj = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(
                        Scene.v().getSootClass(HANDLE_CLASS), "addArrayToObjectMap",
                        parameterTypes, VoidType.v(), false), a);
        Unit assignExpr = Jimple.v().newInvokeStmt(addObj);

        unitStore_After.insertElement(unitStore_After.new Element(assignExpr, pos));
        lastPos = assignExpr;
    }
	
	/*
	 * Change level of elements
	 */

//	/**
//	 * Set the security-level of a local to HIGH.
//	 * @param local Local
//	 * @param pos Unit where this local occurs
//	 */
//	public static void makeLocalHigh(Local local, Unit pos) {
//		logger.log(Level.INFO, "Make Local {0} high in method {1}",
//			new Object[] {getSignatureForLocal(local), b.getMethod().getName()});
//		
//		ArrayList<Type> paramTypes = new ArrayList<Type>();
//		paramTypes.add(RefType.v("java.lang.String"));
//		
//		String signature = getSignatureForLocal(local);
//		Stmt sig = Jimple.v().newAssignStmt(local_for_Strings, StringConstant.v(signature));
//		
//		Expr invokeAddLocal = Jimple.v().newVirtualInvokeExpr(
//				hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS), 
//				"makeLocalHigh", paramTypes, VoidType.v(),false),
//				local_for_Strings);
//		Unit ass = Jimple.v().newInvokeStmt(invokeAddLocal);
//		
//
//		unitStore_After.insertElement(unitStore_After.new Element(sig, pos));
//		unitStore_After.insertElement(unitStore_After.new Element(ass, sig));
//		lastPos = ass;
//	}

    /**
     * Include a new handleStatement.setLevelOfLocal(local, level)
     *
     * @param local local
     * @param level the level to assign to the local
     * @param pos   position where to insert the handleStatmenent
     */
    public static void makeLocal(Local local, String level, Unit pos) {
        logger.info("Setting " + local + "to new level " + level);

        ArrayList<Type> paramTypes = new ArrayList<>();
        paramTypes.add(RefType.v("java.lang.String"));
        paramTypes.add(RefType.v("java.lang.String"));

        // local_for_Strings = getSignatureForLocal(local)
        String signature = getSignatureForLocal(local);
        Stmt sig = Jimple.v().newAssignStmt(local_for_Strings, StringConstant.v(signature));

        // makeLocal(local_for_Strings, "HIHG", position);
        Expr invokeSetLevel = Jimple.v().newVirtualInvokeExpr(
                hs,
                Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "makeLocal", paramTypes,
                        VoidType.v(), false),
                local_for_Strings, StringConstant.v(level));
        Unit setLevelOfL = Jimple.v().newInvokeStmt(invokeSetLevel);

        unitStore_After.insertElement(unitStore_After.new Element(sig, pos));
        unitStore_After.insertElement(unitStore_After.new Element(setLevelOfL, sig));
        lastPos = setLevelOfL;
    }

//	/**
//	 * Set the security-level of a local to LOW.
//	 * @param local Local
//	 * @param pos Unit where this local occurs
//	 */
//	public static void makeLocalLow(Local local, Unit pos) {
//		logger.log(Level.INFO, "Make Local {0} low in method {1}",
//			new Object[] {getSignatureForLocal(local), b.getMethod().getName()});
//		
//		ArrayList<Type> paramTypes = new ArrayList<Type>();
//		paramTypes.add(RefType.v("java.lang.String"));
//		
//		String signature = getSignatureForLocal(local);
//		Stmt sig = Jimple.v().newAssignStmt(local_for_Strings, StringConstant.v(signature));
//		
//		Expr invokeAddLocal = Jimple.v().newVirtualInvokeExpr(
//				hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS), 
//				"makeLocalLow", paramTypes, VoidType.v(),false), local_for_Strings);
//		Unit ass = Jimple.v().newInvokeStmt(invokeAddLocal);
//		
//
//		unitStore_After.insertElement(unitStore_After.new Element(sig, pos));
//		unitStore_After.insertElement(unitStore_After.new Element(ass, sig));
//		lastPos = ass;
//	}

    /**
     * Add the level of a local on the right side of an assign statement.
     *
     * @param local Local
     * @param pos   Unit where the local occurs
     */
    public static void addLevelInAssignStmt(Local local, Unit pos) {
        logger.info("Adding level in assign statement");

        ArrayList<Type> paramTypes = new ArrayList<>();
        paramTypes.add(RefType.v("java.lang.String"));

        String signature = getSignatureForLocal(local);

        Stmt assignSignature = Jimple.v().newAssignStmt(
                local_for_Strings, StringConstant.v(signature));

        Expr invokeAddLevel = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "joinLevelOfLocalAndAssignmentLevel", paramTypes,
                        Scene.v().getObjectType(),
                        false), local_for_Strings);
        Unit invoke = Jimple.v().newInvokeStmt(invokeAddLevel);

        // only insert the joinLevelOfLocal.. stmt if local is in fact dynamically checked
        // TODO CX is irrelevant here?
        if (varTyping.getAfter(instantiation, (Stmt) pos, local).isDynamic()) {
            unitStore_Before.insertElement(unitStore_Before.new Element(assignSignature, pos));
            unitStore_Before.insertElement(unitStore_Before.new Element(invoke, pos));
            lastPos = pos;
        }
    }


    /*******************************************************************************************
     * AssignStmt Functions.
     ******************************************************************************************/


    /**
     * Add the level of a field of an object. It can be the field of the actually
     * analyzed object or the field
     *
     * @param f   Reference to the instance field
     * @param pos The statement where this field occurs
     */
    public static void addLevelInAssignStmt(InstanceFieldRef f, Unit pos) {
        logger.log(Level.INFO, "Adding level of field {0} in assignStmt in method {1}",
                new Object[]{f.getField().getSignature(), b.getMethod().getName()});

        String fieldSignature = getSignatureForField(f.getField());

        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(RefType.v("java.lang.Object"));
        parameterTypes.add(RefType.v("java.lang.String"));

        // units.getFirst is already a reference to @this
        // Local tmpLocal = (Local) units.getFirst().getDefBoxes().get(0).getValue();
        Unit assignBase = Jimple.v().newAssignStmt(local_for_Objects, f.getBase());
        logger.info("Base " + f.getBase());

        Unit assignSignature = Jimple.v().newAssignStmt(
                local_for_Strings, StringConstant.v(fieldSignature));

        Expr addObj = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(
                        Scene.v().getSootClass(HANDLE_CLASS), "joinLevelOfFieldAndAssignmentLevel",
                        parameterTypes, Scene.v().getObjectType(), false),
                local_for_Objects, local_for_Strings);
        Unit assignExpr = Jimple.v().newInvokeStmt(addObj);

        // TODO CANNOT CAST ..
        //if (varTyping.getAfter(instantiation, (Stmt) pos, (Local) f).isDynamic()) {
            unitStore_Before.insertElement(unitStore_Before.new Element(assignBase, pos));
            unitStore_Before.insertElement(unitStore_Before.new Element(assignSignature, pos));
            unitStore_Before.insertElement(unitStore_Before.new Element(assignExpr, pos));
            lastPos = pos;
        //}
    }

    /**
     * @param f   the field
     * @param pos the position where to insert the statement
     */
    public static void addLevelInAssignStmt(StaticFieldRef f, Unit pos) {
        logger.info("Adding Level of static Field " + f.toString() + " in assign stmt");

        SootField field = f.getField();

        String signature = getSignatureForField(field);

        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(RefType.v("java.lang.Object"));
        parameterTypes.add(RefType.v("java.lang.String"));

        SootClass sc = field.getDeclaringClass();

        Unit assignDeclaringClass = Jimple.v().newAssignStmt(
                local_for_Objects, ClassConstant.v(sc.getName().replace(".", "/")));

        Unit assignSignature = Jimple.v().newAssignStmt(
                local_for_Strings, StringConstant.v(signature));

        Expr addObj = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "joinLevelOfFieldAndAssignmentLevel", parameterTypes,
                        Scene.v().getObjectType(), false),
                local_for_Objects, local_for_Strings);
        Unit assignExpr = Jimple.v().newInvokeStmt(addObj);

        // TODO cannot cast StaticFieldref to Local!
        //if (varTyping.getAfter(instantiation, (Stmt) pos, (Local) f).isDynamic()) {
            unitStore_Before.insertElement(
                    unitStore_Before.new Element(assignDeclaringClass, pos));
            unitStore_Before.insertElement(unitStore_Before.new Element(assignSignature, pos));
            unitStore_Before.insertElement(unitStore_Before.new Element(assignExpr, pos));
            lastPos = pos;
        //}
    }

    /**
     * Add the level of a read array field to the security-level-list.
     *
     * @param a   -ArrayRef- The referenced array field
     * @param pos -Unit- The position where this reference occurs
     */
    public static void addLevelInAssignStmt(ArrayRef a, Unit pos) {
        logger.info("Add Level of Array " + a.toString() + " in assign stmt");

        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(RefType.v("java.lang.Object"));
        parameterTypes.add(RefType.v("java.lang.String"));

        String signature = getSignatureForArrayField(a);
        Unit assignSignature = Jimple.v().newAssignStmt(
                local_for_Strings, StringConstant.v(signature));

        unitStore_Before.insertElement(unitStore_Before.new Element(assignSignature, pos));
        lastPos = assignSignature;

        Expr addObj = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "joinLevelOfArrayFieldAndAssignmentLevel", parameterTypes,
                        Scene.v().getObjectType(), false),
                a.getBase(), local_for_Strings);

        Unit assignExpr = Jimple.v().newInvokeStmt(addObj);

        // TODO CANNOT CAST ...
        //if (varTyping.getAfter(instantiation, (Stmt) pos, (Local) a).isDynamic()) {
            unitStore_Before.insertElement(unitStore_Before.new Element(assignExpr, pos));
            lastPos = pos;
        //}
    }


    public static void setLevelOfAssignStmt(Local l, Unit pos) {
        logger.info("Setting level in assign statement");

        ArrayList<Type> paramTypes = new ArrayList<>();
        paramTypes.add(RefType.v("java.lang.String"));

        String signature = getSignatureForLocal(l);

        Stmt assignSignature = Jimple.v().newAssignStmt(
                local_for_Strings, StringConstant.v(signature));

        // insert setLevelOfLocal, which accumulates the PC and the right-hand side of the assign stmt.
        // The local's sec-value is then set to that sec-value.
        Expr invokeSetLevel = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "setLevelOfLocal", paramTypes,
                        Scene.v().getObjectType(),
                        false), local_for_Strings);
        Unit invoke = Jimple.v().newInvokeStmt(invokeSetLevel);

        // insert checkLocalPC to perform NSU check (aka check that level of local greater/equal level of lPC)
        // only needs to be done if CxTyping of Statement is Dynamic
        Expr checkLocalPC = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "checkLocalPC", paramTypes,
                        VoidType.v(),
                        false), local_for_Strings);
        Unit checkLocalPCExpr = Jimple.v().newInvokeStmt(checkLocalPC);

        // TODO i did comment this out for some reason .. but why?
        // if variable l is not dynamic after stmt pos, we do not need to call setLevelOfLocal at all,
        // and we especially do not need to perform a NSU check!
        if (varTyping.getAfter(instantiation, (Stmt) pos, l).isDynamic()) {
        unitStore_Before.insertElement(unitStore_Before.new Element(assignSignature, pos));

        // insert NSU check only if PC is dynamic!
        if (cxTyping.get(instantiation, (Stmt) pos).isDynamic()) {
            unitStore_Before.insertElement(unitStore_Before.new Element(checkLocalPCExpr, pos));
        }
        unitStore_Before.insertElement(unitStore_Before.new Element(invoke, pos));
        }
        lastPos = pos;
    }

    /**
     * Set the level of a field of an object. It can be the field of the actually
     * analyzed object or the field
     *
     * @param f   Reference to the instance field
     * @param pos The statement where this field occurs
     */
    public static void setLevelOfAssignStmt(InstanceFieldRef f, Unit pos) {
        logger.log(Level.INFO, "Set level to field {0} in assignStmt in method {1}",
                new Object[]{f.getField().getSignature(), b.getMethod().getName()});

//		if (!(units.getFirst() instanceof IdentityStmt) 
//				|| !(units.getFirst().getUseBoxes().get(0).getValue() 
//				instanceof ThisRef)) {
//			System.out.println(units.getFirst().getUseBoxes().toString());
//			throw new InternalAnalyzerException("Expected @this reference");
//		}

        String fieldSignature = getSignatureForField(f.getField());

        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(RefType.v("java.lang.Object"));
        parameterTypes.add(RefType.v("java.lang.String"));


        // Retrieve the object it belongs to
        Local tmpLocal = (Local) f.getBase();

        Unit assignSignature = Jimple.v().newAssignStmt(
                local_for_Strings, StringConstant.v(fieldSignature));

        // push and pop security level of instance to globalPC
        Unit pushInstanceLevelToGlobalPC = Jimple.v().newInvokeStmt(
                Jimple.v().newVirtualInvokeExpr(
                        hs, Scene.v().makeMethodRef(
                                Scene.v().getSootClass(HANDLE_CLASS), "pushInstanceLevelToGlobalPC",
                                Collections.<Type>singletonList(RefType.v("java.lang.String")),
                                VoidType.v(), false),
                        StringConstant.v(getSignatureForLocal(tmpLocal)))
        );

        Unit popGlobalPC = Jimple.v().newInvokeStmt(
                Jimple.v().newVirtualInvokeExpr(
                        hs, Scene.v().makeMethodRef(
                                Scene.v().getSootClass(HANDLE_CLASS), "popGlobalPC",
                                Collections.<Type>emptyList(), RefType.v("java.lang.Object"), false))
        );

        // insert: checkGlobalPC(Object, String)
        // why do we check the global PC? Because the field is possibily visible everywhere, check that sec-value of field is greater
        // or equal than the global PC.
        Expr checkGlobalPC = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(
                        Scene.v().getSootClass(HANDLE_CLASS), "checkGlobalPC",
                        parameterTypes, VoidType.v(), false),
                tmpLocal, local_for_Strings);
        Unit checkGlobalPCExpr = Jimple.v().newInvokeStmt(checkGlobalPC);

        // insert setLevelOfField, which sets Level of Field to the join of gPC and right-hand side of assign stmt sec-value join
        Expr addObj = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(
                        Scene.v().getSootClass(HANDLE_CLASS), "setLevelOfField",
                        parameterTypes, Scene.v().getObjectType(), false),
                tmpLocal, local_for_Strings);
        Unit assignExpr = Jimple.v().newInvokeStmt(addObj);

        // pushInstanceLevelToGlobalPC and popGlobalPC take the instance, push to global pc; and pop afterwards.
        // see NSU_FieldAccess tests why this is needed
        unitStore_Before.insertElement(unitStore_Before.new Element(pushInstanceLevelToGlobalPC, pos));
        unitStore_Before.insertElement(unitStore_Before.new Element(assignSignature, pos));
        // only if context ist dynamic / pc is dynamc
        if (cxTyping.get(instantiation, (Stmt) pos).isDynamic()) {
            unitStore_Before.insertElement(unitStore_After.new Element(checkGlobalPCExpr, pos));
        }
        unitStore_Before.insertElement(unitStore_After.new Element(assignExpr, pos));
        unitStore_Before.insertElement(unitStore_Before.new Element(popGlobalPC, pos));
        lastPos = pos;
    }


    public static void setLevelOfAssignStmt(StaticFieldRef f, Unit pos) {
        logger.info("Set Level of static Field " + f.toString() + " in assign stmt");

        SootField field = f.getField();

        String signature = getSignatureForField(field);
        logger.info("Signature of static field in jimple injector " + signature);

        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(RefType.v("java.lang.Object"));
        parameterTypes.add(RefType.v("java.lang.String"));

        SootClass sc = field.getDeclaringClass();

        Unit assignDeclaringClass = Jimple.v().newAssignStmt(
                local_for_Objects, ClassConstant.v(sc.getName().replace(".", "/")));

        Unit assignSignature = Jimple.v().newAssignStmt(
                local_for_Strings, StringConstant.v(signature));

        // insert: checkGlobalPC(Object, String)
        Expr checkGlobalPC = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(
                        Scene.v().getSootClass(HANDLE_CLASS), "checkGlobalPC",
                        parameterTypes, VoidType.v(), false),
                local_for_Objects, local_for_Strings);
        Unit checkGlobalPCExpr = Jimple.v().newInvokeStmt(checkGlobalPC);

        // Add setLevelOfField
        Expr addObj = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "setLevelOfField", parameterTypes,
                        Scene.v().getObjectType(), false),
                local_for_Objects, local_for_Strings);
        Unit assignExpr = Jimple.v().newInvokeStmt(addObj);

        unitStore_Before.insertElement(
                unitStore_Before.new Element(assignDeclaringClass, pos));
        unitStore_Before.insertElement(unitStore_Before.new Element(assignSignature, pos));
        if (cxTyping.get(instantiation, (Stmt) pos).isDynamic()) {
            unitStore_Before.insertElement(unitStore_Before.new Element(checkGlobalPCExpr, pos));
        }
        unitStore_Before.insertElement(unitStore_Before.new Element(assignExpr, pos));
        lastPos = pos;
    }

    /**
     * Inject a method of HandleStmt to set the level of an array-field. This method
     * distinguishes two cases, one case where the index of the referenced array-field
     * is a constant number and the other case, where the index is stored in a local variable.
     * In the second case, the signature of the local variable also must be passed as an
     * argument to {@link analyzer.level2.HandleStmt
     * #setLevelOfArrayField(Object o, int field, String localForObject,
     * String localForIndex)} .
     *
     * @param a   -ArrayRef. The reference to the array-field
     * @param pos -Unit- The assignStmt in the analyzed methods body, where this
     *            reference appears.
     */
    public static void setLevelOfAssignStmt(ArrayRef a, Unit pos) {
        logger.info("Set level of array " + a.toString() + " in assign stmt");

        // Add extra locals for arguments
        if (!extralocals) {
            locals.add(local_for_Strings2);
            locals.add(local_for_Strings3);
            extralocals = true;
        }

        // Define the types of the arguments for HandleStmt.setLevelOfArrayField()
        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(RefType.v("java.lang.Object")); // for Object o
        parameterTypes.add(RefType.v("java.lang.String")); // for String field
        parameterTypes.add(RefType.v("java.lang.String")); // for String localForObject

        Value objectO = a.getBase();
        String signatureForField = getSignatureForArrayField(a);
        String signatureForObjectLocal = getSignatureForLocal((Local) a.getBase());

        // List for the arguments for HandleStmt.setLevelOfArrayField()
        List<Value> args = new ArrayList<>();
        args.add(objectO);

        // Store all string-arguments in locals for strings and assign the locals to the
        // argument list.
        Unit assignFieldSignature = Jimple.v().newAssignStmt(
                local_for_Strings, StringConstant.v(signatureForField));
        Unit assignObjectSignature = Jimple.v().newAssignStmt(local_for_Strings2,
                StringConstant.v(signatureForObjectLocal));

        args.add(local_for_Strings);
        args.add(local_for_Strings2);


        if (!(a.getIndex() instanceof Local)) {
            // Case where the index is a constant.
            // The needed arguments are "Object o, String field, String localForObject".

            logger.fine("Index value for the array field is a constant value");

        } else if (a.getIndex() instanceof Local) {
            // The index is a local and must be given as a parameter.
            // The needed arguments are
            // "Object o, String field, String localForObject, String localForIndex".

            logger.fine("Index value for the array field is stored in a local");

            // add a further parameter type for String localForIndex and
            // add it to the arguments-list.
            parameterTypes.add(RefType.v("java.lang.String"));
            String localSignature = getSignatureForLocal((Local) a.getIndex());
            Unit assignIndexSignature = Jimple.v().newAssignStmt(local_for_Strings3,
                    StringConstant.v(localSignature));
            args.add(local_for_Strings3);

            unitStore_Before.insertElement(unitStore_Before.new Element(
                    assignIndexSignature, pos));

        }

        // checkArrayWithGlobalPC
        Expr checkArrayGlobalPC = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "checkArrayWithGlobalPC", parameterTypes,
                        VoidType.v(), false), args);
        Unit checkArrayGlobalPCExpr = Jimple.v().newInvokeStmt(checkArrayGlobalPC);


        // setLevelOfArrayField
        Expr addObj = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "setLevelOfArrayField", parameterTypes,
                        Scene.v().getObjectType(), false), args);
        Unit assignExpr = Jimple.v().newInvokeStmt(addObj);

        unitStore_Before.insertElement(
                unitStore_Before.new Element(assignFieldSignature, pos));
        unitStore_Before.insertElement(
                unitStore_Before.new Element(assignObjectSignature, pos));
        unitStore_Before.insertElement(
                unitStore_Before.new Element(checkArrayGlobalPCExpr, pos));
        unitStore_Before.insertElement(
                unitStore_Before.new Element(assignExpr, pos));
        lastPos = pos;
    }

    /**
     * Note: Altough method is not used by jimpleInjector, the corresponding handleStatement method is used in the manually instrumented tests.
     */
    @SuppressWarnings("unused")
    public static void assignReturnLevelToLocal(Local l, Unit pos) {
        logger.log(Level.INFO, "Assign return level of invoked method to local {0}",
                getSignatureForLocal(l));

        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(RefType.v("java.lang.String"));

        Expr assignRet = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(
                        Scene.v().getSootClass(HANDLE_CLASS), "assignReturnLevelToLocal",
                        parameterTypes, VoidType.v(), false),
                StringConstant.v(getSignatureForLocal(l)));
        Unit assignExpr = Jimple.v().newInvokeStmt(assignRet);

        unitStore_After.insertElement(unitStore_After.new Element(assignExpr, pos));
        lastPos = assignExpr;
    }

    public static void assignArgumentToLocal(int posInArgList, Local local) {
        logger.log(Level.INFO, "Assign argument level to local " + local.toString());

        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(IntType.v());
        parameterTypes.add(RefType.v("java.lang.String"));
        Expr assignArg = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(
                        Scene.v().getSootClass(HANDLE_CLASS), "assignArgumentToLocal",
                        parameterTypes, Scene.v().getObjectType(), false),
                IntConstant.v(posInArgList),
                StringConstant.v(getSignatureForLocal(local))
        );

        Unit assignExpr = Jimple.v().newInvokeStmt(assignArg);

        // only assign Argument to Local if Argument is of Dynamic Type
        if (instantiation.get(posInArgList).isDynamic()) {
            unitStore_After.insertElement(unitStore_After.new Element(assignExpr, lastPos));
            lastPos = assignExpr;
        }
    }


    /*******************************************************************************************
     *	Inter-scope functions.
     ******************************************************************************************/


    public static void returnConstant(Unit retStmt) {
        logger.log(Level.INFO, "Return a constant value");

        ArrayList<Type> parameterTypes = new ArrayList<>();

        Expr returnConst = Jimple.v().newVirtualInvokeExpr(hs, Scene.v().makeMethodRef(
                Scene.v().getSootClass(HANDLE_CLASS), "returnConstant",
                parameterTypes, VoidType.v(), false));

        if (instantiation.getReturn().isDynamic()) {
            unitStore_Before.insertElement(unitStore_Before.new Element(
                    Jimple.v().newInvokeStmt(returnConst), retStmt));
        }
    }

    public static void returnLocal(Local l, Unit pos) {
        logger.log(Level.INFO, "Return Local {0}", getSignatureForLocal(l));

        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(RefType.v("java.lang.String"));

        Stmt sig = Jimple.v().newAssignStmt(
                local_for_Strings, StringConstant.v(getSignatureForLocal(l)));

        Expr returnLocal = Jimple.v().newVirtualInvokeExpr(hs, Scene.v().makeMethodRef(
                Scene.v().getSootClass(HANDLE_CLASS), "returnLocal", parameterTypes,
                VoidType.v(), false), local_for_Strings);

        Stmt returnL = Jimple.v().newInvokeStmt(returnLocal);

        if (instantiation.getReturn().isDynamic()) {
            unitStore_Before.insertElement(unitStore_Before.new Element(sig, pos));
            unitStore_Before.insertElement(unitStore_Before.new Element(returnL, pos));
            lastPos = pos;
        }
    }

    /**
     * Store the levels of all arguments in a list in ObjectMap. If an
     * argument is a constant, then the argument is stored as "DEFAULT_LOW".
     *
     * @param pos        position of actual statement
     * @param lArguments list of arguments
     */

    // obect map ist global erreichbar. dahin lege temporär die argumente.
    // dann beim aufruf schaut die neue local map in der neuen methode in die global
    // map und nimmt sich von da die level der gerade übergebenen argumente.
    public static void storeArgumentLevels(Unit pos, Local... lArguments) {

        logger.log(Level.INFO, "Store Arguments for next method in method {0}",
                b.getMethod().getName());

        int length = lArguments.length;

        ArrayList<Type> parameterTypes = new ArrayList<>();
        parameterTypes.add(ArrayType.v(RefType.v("java.lang.String"), 1));

        Expr paramArray = Jimple.v().newNewArrayExpr(RefType.v(
                "java.lang.String"), IntConstant.v(length));

        Unit assignNewStringArray = Jimple.v().newAssignStmt(
                local_for_String_Arrays, paramArray);

        Unit[] tmpUnitArray = new Unit[length];

        boolean dynamicArgsExist = false;
        for (int i = 0; i < length; i++) {

            // This results in code like: local_for_String_Arrays[0] = "boolean_z0";
            // for each local that is given as an argument, jimple code is injected that assings this local to a slot
            // in a vector. This vector is given to the HandleStmt.storeArgumentLevels method.
            // It's possible to get a null vector as an argument. This happens,
            // if a constant is set as argument.
            if (lArguments[i] != null && varTyping.getBefore(instantiation, (Stmt) pos, lArguments[i]).isDynamic()) {
                String signature = getSignatureForLocal(lArguments[i]);
                tmpUnitArray[i] = Jimple.v().newAssignStmt(Jimple.v().newArrayRef(
                        local_for_String_Arrays, IntConstant.v(i)),
                        StringConstant.v(signature));
                dynamicArgsExist = true;
                // else if () ..

            } else if (lArguments[i] != null && ! varTyping.getBefore(instantiation, (Stmt) pos, lArguments[i]).isDynamic()) {
                tmpUnitArray[i] = null;
            } else {
                // if arguments are all constants
                tmpUnitArray[i] = Jimple.v().newAssignStmt(Jimple.v().newArrayRef(
                        local_for_String_Arrays, IntConstant.v(i)),
                        StringConstant.v("DEFAULT_LOW"));
                dynamicArgsExist = true;        // this is neccessary, else VerifyError ...?
            }
        }

        Expr storeArgs = Jimple.v().newVirtualInvokeExpr(hs, Scene.v().makeMethodRef(
                Scene.v().getSootClass(HANDLE_CLASS), "storeArgumentLevels",
                parameterTypes, VoidType.v(), false), local_for_String_Arrays);
        Stmt invokeStoreArgs = Jimple.v().newInvokeStmt(storeArgs);

        // only store arguments if it's even neccessary (e.g. one of the arguments is public)
        // if no argument is dynamic, don't even call storeArgumentLevels
        if(dynamicArgsExist) {
            unitStore_Before.insertElement(unitStore_Before.new Element(assignNewStringArray, pos));
        }
        for (Unit el : tmpUnitArray) {
            if (el != null) {
                // if el.equals(null), the corresponding local is public. See loop above.
                unitStore_Before.insertElement(unitStore_Before.new Element(el, pos));
            }
        }
        if (dynamicArgsExist) {
            unitStore_Before.insertElement(unitStore_Before.new Element(invokeStoreArgs, pos));
            lastPos = pos;
        }

    }

    public static void checkThatLe(Local l, String level, Unit pos) {
        checkThatLe(l, level, pos, "checkThatLe");
    }

    /**
     * Insert the following check: If Local l is high, throw new IFCError
     */
    public static void checkThatLe(Local l, String level, Unit pos, String methodName) {
        logger.info("Check that " + l + " is not high");

        if (l == null) {
            throw new InternalAnalyzerException("Argument is null");
        }

        ArrayList<Type> paramTypes = new ArrayList<>();
        paramTypes.add(RefType.v("java.lang.String"));
        paramTypes.add(RefType.v("java.lang.String"));

        String signature = getSignatureForLocal(l);

        Stmt assignSignature = Jimple.v().newAssignStmt(
                local_for_Strings, StringConstant.v(signature));

        Expr invokeSetLevel = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        methodName, paramTypes, VoidType.v(), false),
                local_for_Strings, StringConstant.v(level));
        Unit invoke = Jimple.v().newInvokeStmt(invokeSetLevel);

        // TODO: why check for isDynamic here?
        // if (varTyping.getBefore(instantiation, (Stmt) pos, l).isDynamic()) {
            unitStore_Before.insertElement(unitStore_Before.new Element(assignSignature, pos));
            unitStore_Before.insertElement(unitStore_Before.new Element(invoke, pos));
            lastPos = pos;
        // }
    }

    /**
     * Method to check that PC is not of specified level, or above. Called when calling System.out.println(),
     * which must always be called in low context because of its side effects.
     *
     * @param level level that the PC must not exceed
     * @param pos   position where to insert statement
     */
    public static void checkThatPCLe(String level, Unit pos) {
        logger.info("Check that context is " + level + "or above");

        if (pos == null) {
            throw new InternalAnalyzerException("Position is Null");
        }

        ArrayList<Type> paramTypes = new ArrayList<>();
        paramTypes.add(RefType.v("java.lang.String"));

        Expr checkPC = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "checkThatPCLe", paramTypes, VoidType.v(), false), StringConstant.v(level));
        Unit invoke = Jimple.v().newInvokeStmt(checkPC);

        // only if PC is dynamic
        if (cxTyping.get(instantiation, (Stmt) pos).isDynamic()) {
            unitStore_Before.insertElement(unitStore_Before.new Element(invoke, pos));
            lastPos = pos;
        }
    }

    /**
     * Check condition of if statements. Needed parameters are all locals (no constants)
     * which occur in the if statement. If the result is high, then the lpc of the if-context
     * is set to high.
     *
     * @param pos    Position of the ifStmt in the method body.
     * @param locals An array of all locals which appear in the condition.
     */
    public static void checkCondition(Unit pos, Local... locals) {

        logger.log(Level.INFO, "Check condition in method {0}", b.getMethod());
        logger.log(Level.INFO, "IfStmt: {0}", pos.toString());

        int numberOfLocals = locals.length;
        ArrayList<Type> paramTypes = new ArrayList<>();
        paramTypes.add(RefType.v("java.lang.String"));
        paramTypes.add(ArrayType.v(RefType.v("java.lang.String"), 1)); // here

        // Add hashvalue for immediate dominator
        String domIdentity = DominatorFinder.getImmediateDominatorIdentity(pos);
        logger.info("Identity of Dominator of \"" + pos.toString()
                + "\" is " + domIdentity);
        Stmt assignHashVal = Jimple.v().newAssignStmt(
                local_for_Strings, StringConstant.v(domIdentity));

        // Add all locals to string array
        Expr newStringArray = Jimple.v().newNewArrayExpr(
                RefType.v("java.lang.String"), IntConstant.v(numberOfLocals));

        Unit assignNewArray = Jimple.v().newAssignStmt(
                local_for_String_Arrays, newStringArray);

        ArrayList<Unit> tmpUnitList = new ArrayList<>();

        for (int i = 0; i < numberOfLocals; i++) {
            Unit assignSignature = Jimple.v().newAssignStmt(
                    Jimple.v().newArrayRef(local_for_String_Arrays,
                            IntConstant.v(i)),
                    StringConstant.v(getSignatureForLocal(locals[i])));
            tmpUnitList.add(assignSignature);
        }

        // Invoke HandleStmt.checkCondition(String domHash, String... locals)
        Expr invokeCheckCondition = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "checkCondition", paramTypes, VoidType.v(), false),
                local_for_Strings, local_for_String_Arrays);
        Unit invokeCC = Jimple.v().newInvokeStmt(invokeCheckCondition);

        unitStore_Before.insertElement(unitStore_Before.new Element(assignNewArray, pos));
        lastPos = assignNewArray;
        for (Unit u : tmpUnitList) {
            unitStore_After.insertElement(unitStore_After.new Element(u, lastPos));
            lastPos = u;
        }
        unitStore_After.insertElement(unitStore_After.new Element(assignHashVal, lastPos));
        lastPos = assignHashVal;
        unitStore_After.insertElement(unitStore_After.new Element(invokeCC, lastPos));
        lastPos = invokeCC;

    }

    /**
     * If a stmt is a postdominator of an ifStmt then the if-context ends before this stmt.
     * The method exitInnerScope pops the localPCs for all ifStmts which end here.
     *
     * @param pos The position of this stmt.
     */
    public static void exitInnerScope(Unit pos) {
        logger.log(Level.INFO, "Exit inner scope in method {0}", b.getMethod().getName());

        ArrayList<Type> paramTypes = new ArrayList<>();
        paramTypes.add(RefType.v("java.lang.String"));

        String domIdentity = DominatorFinder.getIdentityForUnit(pos);
        logger.info("Dominator \"" + pos.toString()
                + "\" has identity " + domIdentity);

        Stmt assignHashVal = Jimple.v().newAssignStmt(
                local_for_Strings, StringConstant.v(domIdentity));


        Expr specialIn = Jimple.v().newVirtualInvokeExpr(
                hs, Scene.v().makeMethodRef(Scene.v().getSootClass(HANDLE_CLASS),
                        "exitInnerScope", paramTypes, VoidType.v(), false),
                local_for_Strings);

        Unit inv = Jimple.v().newInvokeStmt(specialIn);

        unitStore_Before.insertElement(
                unitStore_Before.new Element(assignHashVal, pos));
        unitStore_Before.insertElement(unitStore_Before.new Element(inv, pos));
        lastPos = pos;
    }
	
/*
 * Internal methods
 */

    /**
     *
     */
    static void addUnitsToChain() {

        // First add all elements from unitStore_Before
        Iterator<Element> uIt = unitStore_Before.getElements().iterator();

        // If needed for debugging, you can remove the comment
        // unitStore_Before.print();

        while (uIt.hasNext()) {
            Element item = uIt.next();
            if (item.getPosition() == null) {
                units.addFirst(item.getUnit());
            } else {
                // logger.finest("Starting to insert: " + item.getUnit().toString()
                //	+ " after position " + item.getPosition().toString());
                units.insertBefore(item.getUnit(), item.getPosition());
                // logger.finest("Insertion completed");
            }
        }

        // Now add all elements from unitStore_After
        uIt = unitStore_After.getElements().iterator();

        // If needed for debugging, you can remove the comment
        // unitStore_After.print();

        while (uIt.hasNext()) {
            Element item = uIt.next();

            if (item.getPosition() == null) {
                units.addFirst(item.getUnit());
            } else {
                // logger.finest("Starting to insert: " + item.getUnit().toString()
                //	+ " after position " + item.getPosition().toString());
                units.insertAfter(item.getUnit(), item.getPosition());
                // logger.finest("Insertion completed");
            }
        }

        unitStore_After.flush();
        unitStore_Before.flush();

        // print the units in the right order for debugging.
        // printUnits();

        b.validate();
    }

    /**
     * Add all locals which are needed from JimpleInjector to store values
     * of parameters for invoked methods.
     */
    static void addNeededLocals() {
        locals.add(local_for_Strings);
        locals.add(local_for_String_Arrays);
        locals.add(local_for_Objects);

        b.validate();
    }

    /**
     * @param l the local which signature is to be retrieved
     * @return corresponding signature
     */
    private static String getSignatureForLocal(Local l) {
        return l.getType() + "_" + l.getName();
    }

    /**
     * @param f field
     * @return signature of field
     */
    private static String getSignatureForField(SootField f) {
        return f.getSignature();
    }

    /**
     * Create the signature of an array-field based on the index.
     * It simply returns the int-value as string.
     *
     * @param a -ArrayRef-
     * @return -String- The signature for the array-field.
     */
    private static String getSignatureForArrayField(ArrayRef a) {
        logger.fine("Type of index: " + a.getIndex().getType());
        String result;
        if (Objects.equals(a.getIndex().getType().toString(), "int")) {
            result = a.getIndex().toString();
        } else {
            throw new InternalAnalyzerException("Unexpected type of index");
        }
        logger.info("Signature of array field in jimple injector is: " + result);
        return result;
    }

    /**
     * Method which calculates the start position for inserting further units.
     * It depends on the number of arguments and whether it is a static method,
     * or not, because there are statements, which shouldn't be preceeded by
     * other statements.
     *
     * @return the computed start position as int.
     */
    private static int getStartPos(Body b) {
        int startPos = 0;

        SootMethod m = b.getMethod();

        // If the method is not static the @this reference must be skipped.
        if (!m.isStatic()) {
            startPos++;
        }

        if (m.isConstructor()) {
            startPos++;
        }

        // Skip the @parameter-references
        int numOfParams = m.getParameterCount();
        startPos += numOfParams;

        logger.info("Calculated start position: " + startPos);

        return startPos;
    }

    /**
     * This method is only for debugging purposes.
     */
    @SuppressWarnings("unused")
    private static void printUnits() {
        Iterator<Unit> uIt = units.iterator();
        int i = 0;
        System.out.println("Actual method: " + b.getMethod().toString());
        while (uIt.hasNext()) {
            System.out.println(i + " " + uIt.next().toString());
            i++;
        }
    }

    static <Lvel> void setStaticAnalaysisResults(VarTyping<Lvel> varTy, CxTyping<Lvel> cxTy, Instantiation<Lvel> inst,
                                                 Casts c) {
        varTyping = varTy;
        cxTyping = cxTy;
        instantiation = inst;
        casts = c;
    }

    /**
     * Handle Casts.cast(String s, T local) method
     * @param aStmt         Jimple assign statement whose right-hand side is the cast
     */
    public static void handleCast(AssignStmt aStmt) {

        if (casts.isValueCast(aStmt)) {
            Casts.ValueConversion conversion = casts.getValueCast(aStmt);
            logger.info("Found value cast " + conversion);

            if (conversion.getSrcType().isDynamic() && !conversion.getDestType().isDynamic()) {
                // Check eines Security Wert: x = (? => LOW) y
                Option<Value> srcValue = conversion.getSrcValue();
                if (srcValue.isDefined()) {
                    Local rightHandLocal = (Local) srcValue.get();

                    logger.info(
                            "Check that " + getSignatureForLocal(rightHandLocal)
                            + " is less/equal "
                            + conversion.getDestType().getLevel());
                    checkThatLe(rightHandLocal, conversion.getDestType().getLevel().toString(), aStmt, "checkCastToStatic");

                } else {
                    logger.info("Found public src value in cast. Omitting checks.");
                }
            } else if ( !conversion.getSrcType().isDynamic() && conversion.getDestType().isDynamic()) {
                // Initialisierung eines Security Wert: x = (H => ? ) y
               makeLocal((Local) aStmt.getLeftOp(), conversion.getSrcType().getLevel().toString(), aStmt);
            } else if ( conversion.getSrcType().isDynamic() && conversion.getDestType().isDynamic()) {
                // Dynamic -> Dynamic is treated like assign stmt, no extra instrumentation.
            } else {
                // Static to Static, invalid casts should be caught by static analyzer. We double check:
                if (! SecurityLevel.le( SecurityLevel.readLevel(conversion.getSrcType().getLevel().toString()) ,
                                        SecurityLevel.readLevel(conversion.getDestType().getLevel().toString()))) {
                   // can't use that here, because exceptions during soot analysis will cause all UnitTests to fail
                   // throw new IFCError("illegal cast from Static[" + conversion.getSrcType().getLevel()+"] to Static["+ conversion.getDestType().getLevel() + "]");
                }
                // else: treat like assign stmt, no extra instrumentation.
            }
        }
    }
}
