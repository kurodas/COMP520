package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

public class Parser {

	private Scanner scanner;
	private ErrorReporter reporter;
	private Token token;
	private boolean trace = true;

	public Parser(Scanner scanner, ErrorReporter reporter) {
		this.scanner = scanner;
		this.reporter = reporter;
	}


	/**
	 * SyntaxError is used to unwind parse stack when parse fails
	 *
	 */
	class SyntaxError extends Error {
		private static final long serialVersionUID = 1L;
	}

	/**
	 * start parse
	 */
	public void parse() {
		token = scanner.scan();
		try {
			parseProgram();
		}
		catch (SyntaxError e) { }
	}
	
	//Program ::= (ClassDeclaration)* eot
	private void parseProgram(){
		while(token.kind!= TokenKind.EOT){
			parseClassDeclaration();
		}
		accept(TokenKind.EOT);
	}
	
	//ClassDeclaration ::= class id { ( FieldOrMethodDeclaration )* } 
	private void parseClassDeclaration(){
		accept(TokenKind.CLASS);
		accept(TokenKind.ID);
		accept(TokenKind.LEFTBRACKET);
		while(token.kind != TokenKind.RIGHTBRACKET){
			parseFieldOrMethodDeclaration();
		}
		accept(TokenKind.RIGHTBRACKET);
	}
	/*
	 * FieldOrMethodDeclaration = Visibility Access
			(
			FieldOrNonVoidMethodDeclaration
			| VoidMethodDeclaration 
			)
	 */
	private void parseFieldOrMethodDeclaration(){
		parseVisibility();
		parseAccess();
		switch(token.kind){
		case ID:{
			parseFieldOrNonVoidMethodDeclaration();
			return;
		}
		case VOID:{
			parseVoidMethodDeclaration();
			return;
		}
		default:
			parseError("Invalid Term - expecting ID or VOID but found " + token.kind);
		}
	}
	/*
	 * Type_Reference_ArrayReference id  
	 *			( ; | (ParameterList?) { Statement* } )
	 */
	private void parseFieldOrNonVoidMethodDeclaration(){
		parseTypeReferenceOrArrayReference();
		accept(TokenKind.ID);
		switch(token.kind){
		case SEMICOLON:
			acceptIt();
			return;
		case ID:{
			parseParameterList();
			accept(TokenKind.LEFTBRACKET);
			while(token.kind != TokenKind.RIGHTBRACKET)
				parseStatement();
			accept(TokenKind.RIGHTBRACKET);
			return;
		}
		default:
			parseError("Invalid Term - expecting ID or SEMICOLON but found " + token.kind);
		}
	}
	
	//void id (ParameterList?) { Statement* } 
	private void parseVoidMethodDeclaration(){
		accept(TokenKind.VOID);
		accept(TokenKind.ID);
		parseParameterList();
		accept(TokenKind.LEFTBRACKET);
		while(token.kind != TokenKind.RIGHTBRACKET)
			parseStatement();
		accept(TokenKind.RIGHTBRACKET);
		return;
	}
	
	//Visibility ::= (public | private)?
	private void parseVisibility(){
		switch(token.kind){
		case PUBLIC:
			acceptIt();
			return;
		case PRIVATE:
			acceptIt();
			return;
		default:
			return;
		}	
	}
	
	//Access ::= static?
	private void parseAccess(){
		if(token.kind == TokenKind.STATIC)
			acceptIt();
	}
	/*
	 * Type_Reference_ArrayReference ::= 
		id 
		( 
			([ (Expression ]  | ] ) )?
			| ( . id)* 
		) 
		| int ([ ])?	
		| boolean	
		| (this ( . id)*)	

	 */
	private void parseTypeReferenceOrArrayReference(){
		switch(token.kind){
		case ID:
			innerCaseSwitchForIDCaseInParseTypeReferenceOrArrayReference();
		case INT:
			accept(TokenKind.INT);
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				accept(TokenKind.LEFTSQUAREBRACKET);
				accept(TokenKind.RIGHTSQUAREBRACKET);
			}
			return;
		case BOOLEAN:
			accept(TokenKind.BOOLEAN);
			return;
		case THIS: 	
			accept(TokenKind.THIS);
			while(token.kind == TokenKind.DOT){
				accept(TokenKind.DOT);
				accept(TokenKind.ID);
			}
			return;
		default:
			parseError("Invalid Term - expecting ID, INT, BOOLEAN, or THIS but found " + token.kind);
		}	
	}
	
	/*
	 * 		id 
	 *		( 
	 *			( '[' ( Expression ']'  | ']' ) )?
	 *			| ( '.' id)* 
	 *		) 
	 */
	private void innerCaseSwitchForIDCaseInParseTypeReferenceOrArrayReference(){
		accept(TokenKind.ID);
		switch(token.kind){
		case LEFTSQUAREBRACKET:
			accept(TokenKind.LEFTSQUAREBRACKET);
			if(token.kind == TokenKind.RIGHTSQUAREBRACKET){
				accept(TokenKind.RIGHTSQUAREBRACKET);
			}
			else{
				parseExpression();
				accept(TokenKind.RIGHTSQUAREBRACKET);
			}
			return;
		case DOT:
			while(token.kind == TokenKind.DOT){
				accept(TokenKind.DOT);
				accept(TokenKind.ID);
			}
			return;
		default:
			return;
		}
	}
	
	//ParameterList ::= Type_Reference_ArrayReference id ( , Type_Reference_ArrayReference id )*	
	private void parseParameterList(){
		parseTypeReferenceOrArrayReference();
		accept(TokenKind.ID);
		while(token.kind == TokenKind.COMMA){
			accept(TokenKind.COMMA);
			parseTypeReferenceOrArrayReference();
			accept(TokenKind.ID);
		}
		
	}
	
	//ArgumentList ::= Expression (',' Expression)*
	private void parseArgumentList(){
		parseExpression();
		while(token.kind == TokenKind.COMMA){
			accept(TokenKind.COMMA);
			parseExpression();
		}
	}
	
	/*
	 * 	Statement ::=
	 * 		{ Statement* }
	 * 		| Type_Reference_ArrayReference 
	 * 		(
	 *			id = Expression
	 *			| = Expression 
	 *			| (ArgumentList?) 
	 *		) ;
	 *		| return Expression? ;
	 *		| if ( Expression ) Statement (else Statement)? 
	 *		| while ( Expression ) Statement
	 */
	private void parseStatement(){
		switch(token.kind){
		case LEFTBRACKET:
			accept(TokenKind.LEFTBRACKET);
			while(token.kind != TokenKind.RIGHTBRACKET){
				parseStatement();
			}
			return;
		case ID:
			innerCaseSwitchForIDCaseInParseStatement();
			return;
		case RETURN:
			accept(TokenKind.RETURN);
			if(token.kind != TokenKind.SEMICOLON){
				parseExpression();
			}
			accept(TokenKind.SEMICOLON);
			return;
		case IF:
			accept(TokenKind.IF);
			accept(TokenKind.LEFTPAREN);
			parseExpression();
			accept(TokenKind.RIGHTPAREN);
			parseStatement();
			if(token.kind == TokenKind.ELSE){
				accept(TokenKind.ELSE);
				parseStatement();
			}
			return;
		case WHILE:
			accept(TokenKind.WHILE);
			accept(TokenKind.LEFTPAREN);
			parseExpression();
			accept(TokenKind.RIGHTPAREN);
			parseStatement();
			return;
		default:
			parseError("Invalid Term - expecting LEFTBRACKET, ID, RETURN, IF, or WHILE but found " + token.kind);
		}
		
	}
	/*
	 * Type_Reference_ArrayReference 
	 * 	(
	 *		id = Expression
	 *		| = Expression 
	 *		| (ArgumentList?) 
	 *	) 
	 * ;
	 */
	private void innerCaseSwitchForIDCaseInParseStatement(){
		parseTypeReferenceOrArrayReference();
		switch(token.kind){
		case ID:
			accept(TokenKind.ID);
			accept(TokenKind.ASSIGNMENTEQUAL);
			parseExpression();
			break;
		case ASSIGNMENTEQUAL:
			accept(TokenKind.ASSIGNMENTEQUAL);
			parseExpression();
			break;
		case LEFTPAREN:
			accept(TokenKind.LEFTPAREN);
			if(token.kind != TokenKind.RIGHTPAREN){
				parseArgumentList();
			}
			accept(TokenKind.RIGHTPAREN);
			break;
		default:
			parseError("Invalid Term - expecting ID, ASSIGNMENTEQUALS, or LEFTPAREN but found " + token.kind);
		}
		accept(TokenKind.SEMICOLON);
	}
	
	/*Expression ::=
		Type_Reference_ArrayReference ( ( ArgumentList? ) )? (binop Expression)*	
		| unop Expression (binop Expression)*
		| ( Expression ) (binop Expression)*
		| num (binop Expression)*
		| true (binop Expression)*
		| false (binop Expression)*
		| new (id ( ( ) | [ Expression ] ) | int [ Expression ]) (binop Expression)*
	 * 
	 */
	private void parseExpression(){
		switch(token.kind){
		case ID:
			parseTypeReferenceOrArrayReference();
//			if(token.kind ==)
		}
	}
	
	private void parseBinop(){
		
	}
	
	private void parseUnop(){
		
	}
	
	//    S ::= E$
	private void parseS() throws SyntaxError {
		parseE();
		accept(TokenKind.EOT);
	}

	//    E ::= T (("+" | "*") T)*     
	private void parseE() throws SyntaxError {
		parseT();
		while (token.kind == TokenKind.PLUS || token.kind == TokenKind.TIMES) {
			acceptIt();
			parseT();
		} 
	}

	//   T ::= num | "(" E ")"
	private void parseT() throws SyntaxError {
		switch (token.kind) {

		case NUM:
			acceptIt();
			return;

		case LEFTPAREN:
			acceptIt();
			parseE();
			accept(TokenKind.RIGHTPAREN);
			return;

		default:
			parseError("Invalid Term - expecting NUM or LPAREN but found " + token.kind);
		}
	}

	/**
	 * accept current token and advance to next token
	 */
	private void acceptIt() throws SyntaxError {
		accept(token.kind);
	}

	/**
	 * verify that current token in input matches expected token and advance to next token
	 * @param expectedToken
	 * @throws SyntaxError  if match fails
	 */
	private void accept(TokenKind expectedTokenKind) throws SyntaxError {
		if (token.kind == expectedTokenKind) {
			if (trace)
				pTrace();
			token = scanner.scan();
		}
		else
			parseError("expecting '" + expectedTokenKind +
					"' but found '" + token.kind + "'");
	}

	/**
	 * report parse error and unwind call stack to start of parse
	 * @param e  string with error detail
	 * @throws SyntaxError
	 */
	private void parseError(String e) throws SyntaxError {
		reporter.reportError("Parse error: " + e);
		throw new SyntaxError();
	}

	// show parse stack whenever terminal is  accepted
	private void pTrace() {
		StackTraceElement [] stl = Thread.currentThread().getStackTrace();
		for (int i = stl.length - 1; i > 0 ; i--) {
			if(stl[i].toString().contains("parse"))
				System.out.println(stl[i]);
		}
		System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
		System.out.println();
	}

}
