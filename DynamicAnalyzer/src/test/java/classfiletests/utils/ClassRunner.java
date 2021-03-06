package classfiletests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;

import utils.exceptions.IFCError;
import utils.exceptions.IllegalFlowError;
import utils.exceptions.NSUError;
import utils.exceptions.SuperfluousInstrumentation.AssignArgumentToLocalExcpetion;

import utils.exceptions.InternalAnalyzerException;
import utils.exceptions.SuperfluousInstrumentation.LocalPcCalledException;
import utils.exceptions.SuperfluousInstrumentation.joinLevelOfLocalAndAssignmentLevelException;
import utils.logging.L1Logger;
import utils.staticResults.superfluousInstrumentation.ExpectedException;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;
import java.io.File;

/**
 * Helper Class to run a given binary class file, and see whether of not it
 * throws the desired exception. Used by the end-to-end tests.
 * 
 * @author Regina Koenig, Nicolas Müller
 */
public class ClassRunner {

	private static Logger logger = L1Logger.getLogger();
	private static final String TARGET_NAME = "exec";

	/**
	 * Runs the given class via Apache Ant.
	 * 
	 * @param className
	 *            class to be run
	 */
	private static void runClass(String className, String packageName, String outputDir) {

		Project project = new Project();
		project.setName("ClassRunner");
		project.init();

		Target target = new Target();
		target.setName(TARGET_NAME);

		Java task = new Java();
		Path path = task.createClasspath();
		path.createPathElement().setPath("./sootOutput/" + outputDir);
		path.createPathElement().setPath("./bin");

		for (URL url : ((URLClassLoader) ClassLoader.getSystemClassLoader())
				.getURLs()) {
			path.createPathElement().setPath(url.getPath());
		}
		task.setClasspath(path);
		task.setClassname(packageName + "." + className);
		task.setFork(false);
		task.setFailonerror(true);
		task.setProject(project);

		target.addTask(task);

		project.addTarget(target);
		project.setDefault(TARGET_NAME);

		target.execute();
		project.executeTarget(project.getDefaultTarget());

	}

	/**
	 * Run class and check whether the expected exception is found.
	 * @author Regina Koenig, Nicolas Müller
	 */
	public static void testClass(String className, String outputDir, String packageDir,
                                 ExpectedException expEx, String... involvedVars) {
		
		logger.info("Trying to run test " + className);

		String fullPath = System.getProperty("user.dir") + "/sootOutput/"
				+ outputDir + "/" + packageDir + "/" + className + ".class";

		if (!new File(fullPath).isFile()) {
			logger.severe("File "
					+ fullPath
					+ " not found. Something weird is going on, because the test should automatically"
					+ "compile the desired file and put it into the correct folder.");
			fail();
		}

		try {
			// TODO: deal w/ package names correctly
			runClass(className, packageDir.replace('/', '.'), outputDir);

			// Fail if the class was running but an exception was expected
			if (! expEx.equals(ExpectedException.NONE)) {
				logger.severe("Expected exception is not found");
				fail();
			}
		} catch (Exception e) {
			logger.severe("Found exception " + e.getClass().toString());
			e.printStackTrace();

			switch (expEx) {
				case NONE:
				    // fail if no expection was expected
					logger.severe("Fail because exception was not expected");
					fail();
				case ILLEGAL_FLOW:
					// TODO: is there no better way of doing this comparison?
					assertEquals(IllegalFlowError.class.toString(), e.getCause()
														  .getClass().toString());

					if (involvedVars.length < 1) {
						logger.warning("No variables supplied for IllegalFlowError");
					}
					for (String var : involvedVars) {
						logger.info("Check whether " + var + " is contained in: "
								+ e.getMessage() + " ... ");
						if (e.getMessage().contains(var)) {
							logger.info("Success!");
						} else {
							logger.severe("Fail, Exception thrown, but incorrect variable!");
						}
						assertTrue(e.getMessage().contains(var));
					}
					break;
				case NSU_FAILURE:
					assertEquals(NSUError.class.toString(), e.getCause()
															 .getClass().toString());
					break;
				case CHECK_LOCAL_PC_CALLED:
					assertEquals(LocalPcCalledException.class.toString(), e.getCause()
							.getClass().toString());
					break;
				case ASSIGN_ARG_TO_LOCAL:
					assertEquals(AssignArgumentToLocalExcpetion.class.toString(), e.getCause()
							.getClass().toString());
					break;
				case JOIN_LEVEL_OF_LOCAL_AND_ASSIGNMENT_LEVEL:
					assertEquals(joinLevelOfLocalAndAssignmentLevelException.class.toString(), e.getCause()
							.getClass().toString());
					break;
				case SET_RETURN_AFTER_INVOKE:
					assertEquals(joinLevelOfLocalAndAssignmentLevelException.class.toString(), e.getCause()
							.getClass().toString());
					break;
				default:
					throw new InternalAnalyzerException("Unknown exception found!");
			}
		}

	}

}