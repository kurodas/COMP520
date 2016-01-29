package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import miniJava.ErrorReporter;

public class Scanner {
	private InputStream inputStream;
	private ErrorReporter reporter;

	private char currentChar;
	private StringBuilder currentSpelling;

	public Scanner(InputStream inputStream, ErrorReporter reporter) {
		this.inputStream = inputStream;
		this.reporter = reporter;

		// initialize scanner state
		readChar();
	}

	/**
	 * skip whitespace and scan next token
	 * 
	 * @return token
	 */
	public Token scan() {
		boolean inCommentLine = false;
		boolean inCommentBlock = false;
		// skip whitespace and comments
		// try-catch block for possible IOException
		Skip: try {
			while (currentChar == ' ' || currentChar == '\t'
					|| currentChar == '\n' || currentChar == '/'
					|| inCommentLine || inCommentBlock) {
				// Look ahead one character to determine if start of comment

				if (currentChar == '/' && !inCommentLine && !inCommentBlock) {
					//Save point to restore in case this is not start of a comment
					inputStream.mark(2);
					int c = inputStream.read(); 
					char nextChar = (char) c;
					if (nextChar == '/') {// Start of inline comment
						inCommentLine = true;
						skipIt();

					} else if (nextChar == '*') { // Start of block comment
						inCommentBlock = true;
						skipIt();
					} else { // Is division operator
						//Reset inputStream to make sure no characters are skipped
						inputStream.reset();
						//Break out of skipping white space loop
						break Skip;
					}
				} 
				// Exit inline comment once newline char is encountered
				else if(inCommentLine && currentChar == '\n'){
					inCommentLine = false;
					skipIt();
				}
				// '*' may signal end of comment block
				else if(inCommentBlock && currentChar == '*'){
					//Check if next char is '/'
					nextChar();
					// End of comment block
					if(currentChar == '/'){
						inCommentBlock = false;
						skipIt();
					}
					// Comment block continues
					else
						skipIt();
				}
				else
					skipIt();
			}
		} catch (IOException e) {
			scanError("I/O Exception!");
			currentChar = '$';
		}

		// collect spelling and identify token kind
		currentSpelling = new StringBuilder();
		TokenKind kind = scanToken();

		// return new token
		return new Token(kind, currentSpelling.toString());
	}

	public TokenKind scanToken() {
		
		// scan Token
		switch (currentChar) {
		case 'a':  case 'b':  case 'c':  case 'd':  case 'e':
	    case 'f':  case 'g':  case 'h':  case 'i':  case 'j':
	    case 'k':  case 'l':  case 'm':  case 'n':  case 'o':
	    case 'p':  case 'q':  case 'r':  case 's':  case 't':
	    case 'u':  case 'v':  case 'w':  case 'x':  case 'y':
	    case 'z':
	    case 'A':  case 'B':  case 'C':  case 'D':  case 'E':
	    case 'F':  case 'G':  case 'H':  case 'I':  case 'J':
	    case 'K':  case 'L':  case 'M':  case 'N':  case 'O':
	    case 'P':  case 'Q':  case 'R':  case 'S':  case 'T':
	    case 'U':  case 'V':  case 'W':  case 'X':  case 'Y':
	    case 'Z':
	    case '_':
	      takeIt();
	      while (isAlpha(currentChar) || isDigit(currentChar) || currentChar == '_')
	        takeIt();
	      return TokenKind.ID;
	      
		case '+':
			takeIt();
			return (TokenKind.PLUS);
		case '*':
			takeIt();
			return (TokenKind.TIMES);
		case '/':
			takeIt();
			return (TokenKind.DIVIDE);
		case '(':
			takeIt();
			return (TokenKind.LEFTPAREN);
		case ')':
			takeIt();
			return (TokenKind.RIGHTPAREN);
		case '{':
			takeIt();
			return (TokenKind.LEFTBRACKET);
		case '}':
			takeIt();
			return (TokenKind.RIGHTBRACKET);
		case '[':
			takeIt();
			return (TokenKind.LEFTSQUAREBRACKET);
		case ']':
			takeIt();
			return (TokenKind.RIGHTSQUAREBRACKET);
		case ';':
			takeIt();
			return (TokenKind.SEMICOLON);
		case ',':
			takeIt();
			return (TokenKind.COMMA);
		case '.':
			takeIt();
			return (TokenKind.DOT);
		case '<':
			takeIt();
			if (currentChar == '=') {
				return (TokenKind.LESSTHANEQUAL);
			} else
				return (TokenKind.LESSTHAN);
		case '>':
			takeIt();
			if (currentChar == '=') {
				return (TokenKind.GREATERTHANEQUAL);
			} else
				return (TokenKind.GREATERTHAN);
		case '|':
			takeIt();
			if (currentChar == '|')
				return (TokenKind.OR);
		case '&':
			takeIt();
			if (currentChar == '&')
				return (TokenKind.AND);
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
			while (isDigit(currentChar))
				takeIt();
			return (TokenKind.NUM);

		case '$':
			return (TokenKind.EOT);

		default:
			scanError("Unrecognized character '" + currentChar + "' in input");
			return (TokenKind.ERROR);
		}
	}

	private void takeIt() {
		currentSpelling.append(currentChar);
		nextChar();
	}

	private void skipIt() {
		nextChar();
	}

	private boolean isDigit(char c) {
		return (c >= '0') && (c <= '9');
	}

	private boolean isAlpha(char c) {
		return ((c >= 'a') && (c <= 'z') || (c >= 'A') && (c <= 'Z'));
	}

	private boolean isOperator(char c) {
		return (c == '+' || c == '-' || c == '*' || c == '/' || c == '='
				|| c == '<' || c == '>' || c == '&' || c == '%' || c == '!');
	}

	private void scanError(String m) {
		reporter.reportError("Scan Error:  " + m);
	}

	private final static char eolUnix = '\n';
	private final static char eolWindows = '\r';
	private final static char eot = '\4';

	/**
	 * advance to next char in inputstream detect end of line or end of file and
	 * substitute '$' as distinguished eot terminal
	 */
	private void nextChar() {
		if (currentChar != '$')
			readChar();
	}

	private void readChar() {
		try {
			int c = inputStream.read();
			currentChar = (char) c;
			// Standardize newline chars
			if (currentChar == eolUnix || currentChar == eolWindows) {
				currentChar = '\n';
			}
			// Substitute '$' for eot or -1
			else if (c == -1 || c == eot) {
				currentChar = '$';
			} else if (currentChar == '$') {
				scanError("Illegal character '$' in input");
			}
		} catch (IOException e) {
			scanError("I/O Exception!");
			currentChar = '$';
		}
	}
}
