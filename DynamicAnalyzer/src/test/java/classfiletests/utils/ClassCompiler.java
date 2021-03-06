package classfiletests.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import analyzer.level2.storage.LowMediumHigh;
import de.unifreiburg.cs.proglang.jgs.constraints.TypeDomain;
import de.unifreiburg.cs.proglang.jgs.instrumentation.*;
import soot.SootMethod;
import utils.Controller;
import utils.logging.L1Logger;
import main.Main;
import utils.parser.ArgParser;
import utils.parser.ArgumentContainer;
import utils.staticResults.BeforeAfterContainer;
import utils.staticResults.MIMap;
import utils.staticResults.MSLMap;
import utils.staticResults.MSMap;
import utils.staticResults.implementation.MethodsImpl;
import utils.staticResults.implementation.Types;

/**
 * Compiles a given class using the main.Main.main method.
 * @author Nicolas Müller
 */
public class ClassCompiler {

	private static Logger logger = L1Logger.getLogger();

	/**
	 * Static Helper Method to compile a given test, and put it into
	 * sootOutput/outputDir
	 * 
	 * @param name		name of the test
	 * @param outputDir	subfolder of the sootOutput/ directory to put compiled binary
	 */
	public static void compile(String name, String outputDir) {

		String[] args = {"-m", "testclasses." + name, "-o", "sootOutput/" + outputDir};
		logger.info("Compilation of src file started");
		Main.main(args);
		logger.info("Compilation successful, binary put in sootOutput/"
				+ outputDir);
	}

	public static void compileWithFakeTyping(String name, String outputDir,
											 MSLMap<BeforeAfterContainer> varTyping,
											 MSMap<Types> cxTyping,
											 MIMap<Types> instantiation,
											 Controller isActive,
											 int expectedException) {

		String[] args = {"-m", "testclasses." + name, "-o", "sootOutput/" + outputDir};
		logger.info("Compilation of src file started. Using fake static analysis results");

		// create the methods object
		Methods methods = new MethodsImpl(varTyping, cxTyping, instantiation);
		Casts c = new CastsFromConstants<>(new TypeDomain<>(new LowMediumHigh()),
						"<de.unifreiburg.cs.proglang.jgs.support.Casts: java.lang.Object cast(java.lang.String,java.lang.Object)>",
						"de.unifreiburg.cs.proglang.jgs.instrumentation.Casts.castCx",
						"de.unifreiburg.cs.proglang.jgs.instrumentation.Casts.castCxEnd");
		Main.execute(args, true, methods,
				isActive.equals(Controller.ACTIVE) ? true : false, expectedException, c);
		logger.info("Compilation successful, binary put in sootOutput/"
				+ outputDir);
	}
}
