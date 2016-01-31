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
		} catch (SyntaxError e) {

		}
	}
	
	//Program ::= (ClassDeclaration)* eot
	private void parseProgram() {
		while (token.kind != TokenKind.EOT) {
			parseClassDeclaration();
		}
		accept(TokenKind.EOT);
	}
	
	//ClassDeclaration ::= class id { ( FieldOrMethodDeclaration )* } 
	private void parseClassDeclaration() {
		accept(TokenKind.CLASS);
		accept(TokenKind.ID);
		accept(TokenKind.LEFTBRACKET);
		while (token.kind != TokenKind.RIGHTBRACKET) {
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
		switch (token.kind) {
		case ID: case INT: case BOOLEAN: case THIS:{
			parseFieldOrNonVoidMethodDeclaration();
			return;
		}
		case VOID: {
			parseVoidMethodDeclaration();
			return;
		}
		default:
			parseError("Invalid Term - expecting ID or VOID but found "
					+ token.kind);
		}
	}
	/*
	 * FieldOrNonVoidMethodDeclaration ::=
	 * Type_Reference_ArrayReference id  
	 *			( ; | '(' ParameterList? ')' '{' Statement* '}' )
	 */
	private void parseFieldOrNonVoidMethodDeclaration() {
		parseTypeReferenceOrArrayReference();
		accept(TokenKind.ID);
		switch (token.kind) {
		case SEMICOLON:
			accept(TokenKind.SEMICOLON);
			return;
			
		//	'(' ParameterList? ')' '{' Statement* '}' 
		case LEFTPAREN: {
			accept(TokenKind.LEFTPAREN);
			if (token.kind != TokenKind.RIGHTPAREN) {
				parseParameterList();
			}
			accept(TokenKind.RIGHTPAREN);
			accept(TokenKind.LEFTBRACKET);
			while (token.kind != TokenKind.RIGHTBRACKET)
				parseStatement();
			accept(TokenKind.RIGHTBRACKET);
			return;
		}
		default:
			parseError("Invalid Term - expecting SEMICOLON or LEFTPAREN but found "
					+ token.kind);
		}
	}
	
	//VoidMethodDeclaration ::= void id '(' ParameterList? ')' { Statement* } 
	private void parseVoidMethodDeclaration() {
		accept(TokenKind.VOID);
		accept(TokenKind.ID);
		accept(TokenKind.LEFTPAREN);
		if(token.kind != TokenKind.RIGHTPAREN);
			parseParameterList();
		accept(TokenKind.RIGHTPAREN);
		accept(TokenKind.LEFTBRACKET);
		while (token.kind != TokenKind.RIGHTBRACKET)
			parseStatement();
		accept(TokenKind.RIGHTBRACKET);
		return;
	}
	
	//Visibility ::= (public | private)?
	private void parseVisibility(){
		if (token.kind == TokenKind.PUBLIC || token.kind == TokenKind.PRIVATE)
			acceptIt();
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
			('[' (Expression ']' | ']' ) )?
			| ( '.' id)* 
		) 
		| int ('[' ']')?	
		| boolean	
		| (this ( . id)*)	

	 */
	private void parseTypeReferenceOrArrayReference(){
		switch(token.kind){
		case ID:
			innerCaseSwitchForIDCaseInParseTypeReferenceOrArrayReference();
			return;	
		//	int ([ ])?
		case INT:
			accept(TokenKind.INT);
			if (token.kind == TokenKind.LEFTSQUAREBRACKET) {
				accept(TokenKind.LEFTSQUAREBRACKET);
				accept(TokenKind.RIGHTSQUAREBRACKET);
			}
			return;
			
		//	boolean
		case BOOLEAN:
			accept(TokenKind.BOOLEAN);
			return;
			
		//	(this ( . id)*)
		case THIS: 	
			accept(TokenKind.THIS);
			while(token.kind == TokenKind.DOT){
				accept(TokenKind.DOT);
				accept(TokenKind.ID);
			}
			return;
		default:
			parseError("Invalid Term - expecting ID, INT, BOOLEAN, or THIS but found "
					+ token.kind);
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
		//	( '[' ( Expression ']'  | ']' ) )?
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
			
		//	( '.' id)* 
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
	
	//ParameterList ::= Type_Reference_ArrayReference id ( ',' Type_Reference_ArrayReference id )*	
	private void parseParameterList(){
		parseTypeReferenceOrArrayReference();
		accept(TokenKind.ID);
		//	( , Type_Reference_ArrayReference id )*
		while(token.kind == TokenKind.COMMA){
			accept(TokenKind.COMMA);
			parseTypeReferenceOrArrayReference();
			accept(TokenKind.ID);
		}
		
	}
	
	//ArgumentList ::= Expression (',' Expression)*
	private void parseArgumentList(){
		parseExpression();
		//	(',' Expression)*
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
		//	{ Statement* }
		case LEFTBRACKET:
			accept(TokenKind.LEFTBRACKET);
			while(token.kind != TokenKind.RIGHTBRACKET){
				parseStatement();
			}
			accept(TokenKind.RIGHTBRACKET);
			return;
		case ID: case INT: case BOOLEAN: case THIS:
			innerCaseSwitchForIDINTBOOLEANTHISCasesInParseStatement();
			return;
		//	return Expression? ;
		case RETURN:
			accept(TokenKind.RETURN);
			if(token.kind != TokenKind.SEMICOLON){
				parseExpression();
			}
			accept(TokenKind.SEMICOLON);
			return;
		//	if '(' Expression ')' Statement (else Statement)?
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
		//	while '(' Expression ')' Statement
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
	private void innerCaseSwitchForIDINTBOOLEANTHISCasesInParseStatement(){
		parseTypeReferenceOrArrayReference();
		switch(token.kind){
		//	id = Expression
		case ID:
			accept(TokenKind.ID);
			accept(TokenKind.ASSIGNMENTEQUAL);
			parseExpression();
			break;
		//	= Expression 
		case ASSIGNMENTEQUAL:
			accept(TokenKind.ASSIGNMENTEQUAL);
			parseExpression();
			break;
		//	'(' ArgumentList? ')' 
		case LEFTPAREN:
			accept(TokenKind.LEFTPAREN);
			if(token.kind != TokenKind.RIGHTPAREN){
				parseArgumentList();
			}
			accept(TokenKind.RIGHTPAREN);
			break;
		default:
			parseError("Invalid Term - expecting ID, ASSIGNMENTEQUAL, or LEFTPAREN but found " + token.kind);
		}
		accept(TokenKind.SEMICOLON);
	}
	
	/*Expression ::=
	 * (
	 * 		unop Expression
	 *		| Type_Reference_ArrayReference ( ( ArgumentList? ) )? 
	 *		| '(' Expression ')'
	 *		| num | true | false
	 *		| new (id ( ( ) | [ Expression ] ) | int [ Expression ])
	 * )
	 * (binop Expression)*
	 * 
	 */
	private void parseExpression(){
		switch(token.kind){
		//	unop Expression
		case LOGICALNEGATIVE:
		case MINUSORARITHMETICNEGATIVE:
			acceptIt();
			parseExpression();
			break;
		//	Type_Reference_ArrayReference ( ( ArgumentList? ) )? 
		case ID: case INT: case BOOLEAN: case THIS:
			parseTypeReferenceOrArrayReference();
			if(token.kind == TokenKind.LEFTPAREN){
				accept(TokenKind.LEFTPAREN);
				if(token.kind != TokenKind.RIGHTPAREN){
					parseArgumentList();
				}
				accept(TokenKind.RIGHTPAREN);
			}
			break;
		//	'(' Expression ')'
		case LEFTPAREN:
			accept(TokenKind.LEFTPAREN);
			parseExpression();
			accept(TokenKind.RIGHTPAREN);
			break;
		//	num | true | false
		case NUM:
		case TRUE:
		case FALSE:
			acceptIt();
			break;
		//	new (id ( ( ) | [ Expression ] ) | int [ Expression ])
		case NEW:
			accept(TokenKind.NEW);
			innerCaseSwitchForNEWCaseInParseExpression();
			break;
		default:
			parseError("Invalid Term - expecting LOGICALNEGATIVE, MINUSORARITHMETICNEGATIVE, ID, INT, BOOLEAN, THIS, LEFTPAREN, NUM, TRUE, FALSE, or NEW but found "
					+ token.kind);
		}
		//	(binop Expression)*
		while (isBinop(token)) {
			acceptIt();
			parseExpression();
		}
		
	}
	/*	id 
	 * 		( 
	 * 		'(' ')' 
	 * 		| '[' Expression ']' 
	 * 		) 
	 * 	| int '[' Expression ']'
	 *  
	 */
	private void innerCaseSwitchForNEWCaseInParseExpression(){
		switch(token.kind){
		case ID:
			accept(TokenKind.ID);
			//	'(' ')'
			if(token.kind == TokenKind.LEFTPAREN){
				accept(TokenKind.LEFTPAREN);
				accept(TokenKind.RIGHTPAREN);
			}
			//	'[' Expression ']'
			else if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				accept(TokenKind.LEFTSQUAREBRACKET);
				parseExpression();
				accept(TokenKind.RIGHTSQUAREBRACKET);
			}
			else{
				parseError("Invalid Term - expecting LEFTPAREN or LEFTSQUAREBRACKET but found " + token.kind);
			}
			break;
		//	int '[' Expression ']'
		case INT:
			accept(TokenKind.INT);
			accept(TokenKind.LEFTSQUAREBRACKET);
			parseExpression();
			accept(TokenKind.RIGHTSQUAREBRACKET);
			break;
		default:
			parseError("Invalid Term - expecting ID or INT but found " + token.kind);
		}
	}
	
	private boolean isBinop(Token t){
		return t.kind == TokenKind.LESSTHAN 
				|| t.kind == TokenKind.GREATERTHAN
				|| t.kind == TokenKind.LOGICALEQUAL
				|| t.kind == TokenKind.LESSTHANEQUAL
				|| t.kind == TokenKind.GREATERTHANEQUAL
				|| t.kind == TokenKind.NOTEQUAL
				|| t.kind == TokenKind.AND
				|| t.kind == TokenKind.OR
				|| t.kind == TokenKind.PLUS
				|| t.kind == TokenKind.MINUSORARITHMETICNEGATIVE
				|| t.kind == TokenKind.TIMES
				|| t.kind == TokenKind.DIVIDE;
	}
	
	private boolean isUnop(Token t){
		return t.kind == TokenKind.LOGICALNEGATIVE
				|| t.kind == TokenKind.MINUSORARITHMETICNEGATIVE;
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

	// show parse stack whenever terminal is accepted
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
