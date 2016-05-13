package utils.parser;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * Class for parsing command-line arguments.
 * @author Regina König
 */
@SuppressWarnings("deprecation")
public class ArgumentParser {

	private Options options;
	private CommandLineParser parser;
	private CommandLine cmd;
	HelpFormatter formater;
	
	/**
	 * Definition of command-line options and parsing of given arguments.
	 * @param args Command-line arguments
	 */
	@SuppressWarnings({ })
	public ArgumentParser(String[] args) {
		formater = new HelpFormatter();
		
		Option outputFormat = new Option("f", true, "Determine output format");
		Option mainClass = new Option("m", "main_class", true, 
				"Class containing the main method.");
		Option classes = new Option("c","classes", true,"List of classes to be analyzed.");
		classes.setArgs(Option.UNLIMITED_VALUES);
		Option directories = new Option("d", "process_dir", true, 
						"Analyze all processes inside process-dir");
		Option levelAll = new Option("la", "lvl_all");
		Option levelInfo = new Option("li", "lvl_info");
		Option levelSevere = new Option("ls", "lvl_severe");


		OptionGroup inputOptions = new OptionGroup();
		inputOptions.addOption(classes); 
		inputOptions.addOption(directories);
		inputOptions.setRequired(true);
			
		
		OptionGroup levelOptions = new OptionGroup();
		levelOptions.addOption(levelAll);
		levelOptions.addOption(levelInfo);
		levelOptions.addOption(levelSevere);
		levelOptions.setRequired(false);
		
		
		options = new Options();
		options.addOption(outputFormat);
		options.addOption(mainClass);
		options.addOptionGroup(inputOptions);
		options.addOptionGroup(levelOptions);
		
		
		parser = new BasicParser();
		cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException pvException) {
			formater.printHelp("Usage: ", options);
			System.out.println(pvException.getMessage());
		}
	}
	
	
	/**
	 * Returns logger-level. Default is Level.ALL.
	 * @return Logger-level
	 */
	public Level getLoggerLevel() {
		Level level = Level.ALL;
		
		if (cmd.hasOption("la")) {
			level = Level.ALL;
		} else if (cmd.hasOption("li")) {
			level = Level.INFO;
		} else if (cmd.hasOption("ls")) {
			level = Level.SEVERE;
		}
		
		return level;
	}
	
	/**
	 * Extract arguments for soot.
	 * It should have one of following formats:
	 * new String[]{"-f","c", "-main-class", "main.testclasses.Simple", 
	 * 		"main.testclasses.Simple"}	
	 * or 
	 * new String[]{"-f","c", "-main-class", "main.testclasses.Simple", 
	 * 		"--process-dir", "src/main/testclasses"}
	 * 
	 * @return Array with arguments for Soot.
	 */
	public String[] getSootOptions() {
		LinkedList<String> sootOptions = new LinkedList<String>();
		sootOptions.add("-f");
		sootOptions.add(cmd.getOptionValue("f"));
		sootOptions.add("-main-class");
		sootOptions.add(cmd.getOptionValue("main_class"));
		if (cmd.hasOption("classes")) {
			sootOptions.add(cmd.getOptionValue("classes"));
		} else if (cmd.hasOption("process_dir")) {
			sootOptions.add("-process-dir");
			sootOptions.add(cmd.getOptionValue("process_dir"));
		}
		String[] result = new String[sootOptions.size()];
		sootOptions.toArray(result);
		System.out.println(Arrays.deepToString(result));
		return result;
	}
}
