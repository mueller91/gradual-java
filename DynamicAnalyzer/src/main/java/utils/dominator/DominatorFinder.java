package utils.dominator;

import soot.Body;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.MHGPostDominatorsFinder;
import soot.toolkits.graph.UnitGraph;
import utils.exceptions.MaximumNumberExceededException;
import utils.logging.L1Logger;

import java.lang.Long;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Find postdominators for if-statements to determine their scope. Postdominators are represented as integer IDs.
 * 
 * @author koenigr, fennell
 *
 */

public class DominatorFinder {
	private static MHGPostDominatorsFinder pdfinder;
	private static UnitGraph graph;
	private static HashMap<Unit, String> domList;
	
	// ID-counter for identifying postdominators 
	private static long identity;
	// Distinguished ID to be used if the virtual postdominator at the end of a method. We use this virtual postdominator to work around the fact that Jimple does not guarantee a single return statement.
	private final static String POSTDOM_ID_END_OF_METHOD = "" + Integer.MIN_VALUE; 
	private static Logger logger = L1Logger.getLogger();
  
	/**
	 * Constructor. Has only to be called once in BodyAnalyzer.
	 * @param body The body of the actual analyzed method.
	 */
	public static void init(Body body) {
		graph = new BriefUnitGraph(body);
		pdfinder = new MHGPostDominatorsFinder(graph);
		domList = new HashMap<Unit, String>();
		identity = 0;
	}

	/**
	 * Get the hashvalue of the immediate dominator of given IfStmt. 
	 * The unit is stored in an internal list for later analysis
	 * (if it's not already in the list).
	 * @param node IfStmt.
	 * @return Hashvalue of immerdiate dominator.
	 */
	public static String getImmediateDominatorIdentity(Unit node) {
		Unit dom = (Unit) pdfinder.getImmediateDominator(node); 
		if (dom != null) {
		    String id = getIdentityForUnit(dom);
		    // we have an immediate postdominator
		 logger.info("Postdominator \"" + dom.toString()
				+ "\" has Identity " + id);
		     return id;
		} else {
		 logger.info("Postdominator for node " + node.toString() + "is the end of the method");
		 return POSTDOM_ID_END_OF_METHOD;
		}
	}

	/**
	 * Check whether the given unit is a dominator of an IfStmt.
	 * @param node A Unit.
	 * @return Returns true if the given unit is a dominator of a previously 
	 *     called ifStmt.
	 */
	public static boolean containsStmt(Unit node) {
		return domList.containsKey(node);
	}
	
	/**
	 * Remove given Unit from DominatorList.
	 * @param node A Unit.
	 *     called ifStmt.
	 */
	public static void removeStmt(Unit node) {
		if (domList.containsKey(node)) {
			domList.remove(node);
		}
	}
	
	/** Get the identity for given Unit or create a new identity.
	 * @param dom The Object.
	 * @return The hash-value for given object.
	 */
	public static String getIdentityForUnit(Unit dom) {
		if (domList.containsKey(dom)) {
			return domList.get(dom);
		} else {
			if (identity < Long.MAX_VALUE) {
				identity++;
				domList.put(dom, Long.toString(identity));
			} else {
				new MaximumNumberExceededException("You have exceeded the maximum "
					+ "number of allowed if-statements "
					+ "within a method which is "
					+ Long.toString(identity));
			}
		}
		return domList.get(dom);
	}
	
	public static void printDomList() {
		System.out.println(domList.toString());
	}
  
}
