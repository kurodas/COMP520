package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Compiler {
	/**
	 * @param args  if no args provided parse from keyboard input
	 *              else args[0] is name of file containing input to be parsed  
	 */
	public static void main(String[] args) {

		InputStream inputStream = null;
		if (args.length == 0) {
			System.out.println("Enter Expression");
			inputStream = System.in;
		}
		else {
			try {
				String fileExtension = getFileExtension(args[0]);
				if(fileExtension.equalsIgnoreCase("java")
						|| fileExtension.equalsIgnoreCase("mjava")){
					inputStream = new FileInputStream(args[0]);
				}
				else
					System.out.println("Input file is not of type java or mjava");
			} catch (FileNotFoundException e) {
				System.out.println("Input file " + args[0] + " not found");
				System.exit(1);
			}		
		}

		ErrorReporter reporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, reporter);
		Parser parser = new Parser(scanner, reporter);

		System.out.println("Syntactic analysis ... ");
		parser.parse();
		System.out.print("Syntactic analysis complete:  ");
		if (reporter.hasErrors()) {
			System.out.println("INVALID arithmetic expression");
			System.exit(4);
		}
		else {
			System.out.println("valid arithmetic expression");
			System.exit(0);
		}
	}
	
	private static String getFileExtension(String arg){
		int index = arg.lastIndexOf('.');
		String fileExtension = "";
		if(index > 0){
			fileExtension = arg.substring(index+1);
		}
		return fileExtension;
	}
}
