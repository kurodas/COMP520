package miniJava;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Compiler {
	/**
	 * @param args  if no args provided parse from keyboard input
	 *              else args[0] is name of file containing input to be parsed  
	 */
	public static void main(String[] args) {

		BufferedInputStream inputStream = null;
		if (args.length == 0) {
			System.out.println("Enter Expression");
			inputStream = new BufferedInputStream(System.in);
		}
		else {
			try {
				String fileExtension = getFileExtension(args[0]);
				if (fileExtension.equalsIgnoreCase("java")
						|| fileExtension.equalsIgnoreCase("mjava")) {
					inputStream = new BufferedInputStream(new FileInputStream(
							args[0]));
				} else {
					System.out
							.println("Input file is not of type java or mjava");
					System.exit(1);
				}
			} catch (FileNotFoundException e) {
				System.out.println("Input file " + args[0] + " not found");
				System.exit(1);
			}		
		}

		ErrorReporter reporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, reporter);
		Parser parser = new Parser(scanner, reporter);

		System.out.println("Syntactic analysis ... ");
		AST ast;
		try {
			ast = parser.parse();
			System.out.print("Syntactic analysis complete:  ");
			if (reporter.hasErrors()) {
				System.out.println("INVALID MiniJava");
				System.exit(4);
			}
			else {
				System.out.println("valid MiniJava");
				ASTDisplay astDisplay = new ASTDisplay();
				astDisplay.showTree(ast);
				System.exit(0);
			}
		} catch (Throwable e) {
			if (reporter.hasErrors()) {
				System.out.println("Syntactic analysis complete: INVALID MiniJava");
			}
			System.exit(4);
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
