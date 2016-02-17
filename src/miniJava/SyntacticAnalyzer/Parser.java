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
			Type  id  ( ; | (ParameterList?) { Statement* } ) 
			| void id (ParameterList?) { Statement* } 
			)
	 */
	private void parseFieldOrMethodDeclaration(){
		parseVisibility();
		parseAccess();
		switch (token.kind) {
		case ID: case INT: case BOOLEAN:{
			parseFieldOrNonVoidMethodDeclaration();
			return;
		}
		case VOID: {
			parseVoidMethodDeclaration();
			return;
		}
		default:
			parseError("Invalid Term - expecting ID, INT, BOOLEAN, or VOID but found "
					+ token.kind);
		}
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
	 * Type  id  ( ; | '(' ParameterList? ')' '{' Statement* '}' ) 
	 */
	private void parseFieldOrNonVoidMethodDeclaration() {
		parseType();
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
		System.out.print(token.kind);
		if(token.kind != TokenKind.RIGHTPAREN)
			parseParameterList();
		accept(TokenKind.RIGHTPAREN);
		accept(TokenKind.LEFTBRACKET);
		while (token.kind != TokenKind.RIGHTBRACKET)
			parseStatement();
		accept(TokenKind.RIGHTBRACKET);
		return;
	}
	
	private void parseType(){
		switch(token.kind){
		case INT: case ID:
			acceptIt();
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				acceptIt();
				accept(TokenKind.RIGHTSQUAREBRACKET);
			}
			return;
		case BOOLEAN:
			acceptIt();
			return;
		default:
			parseError("Invalid Term - expecting INT, ID, or BOOLEAN but found "
					+ token.kind);
		}
	}
	
	private void parseReference(){
		switch(token.kind){
		case THIS: case ID:
			acceptIt();
			while(token.kind == TokenKind.DOT){
				acceptIt();
				accept(TokenKind.ID);
			}
		default:
			parseError("Invalid Term - expecting THIS or ID but found "
					+ token.kind);
		}
	}
	
	private void parseArrayReference(){
		accept(TokenKind.ID);
		accept(TokenKind.LEFTSQUAREBRACKET);
		parseExpression();
		accept(TokenKind.RIGHTSQUAREBRACKET);
	}
		
	private void parseParameterList(){
		parseType();
		accept(TokenKind.ID);
		//	( , Type id )*
		while(token.kind == TokenKind.COMMA){
			accept(TokenKind.COMMA);
			parseType();
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
		case ID: 
			acceptIt();
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				acceptIt();
				// id[] id = Expression ;
				if(token.kind == TokenKind.RIGHTSQUAREBRACKET){
					acceptIt();
					accept(TokenKind.ID);
					accept(TokenKind.ASSIGNMENTEQUAL);
					parseExpression();
					accept(TokenKind.SEMICOLON);
					return;
				}
				else{ //id [ Expression ] = Expression ;
					parseExpression();
					accept(TokenKind.RIGHTSQUAREBRACKET);
					accept(TokenKind.ASSIGNMENTEQUAL);
					parseExpression();
					accept(TokenKind.SEMICOLON);
					return;
				}
			}
			//id id = Expression ;
			else if(token.kind == TokenKind.ID){
				acceptIt();
				accept(TokenKind.ASSIGNMENTEQUAL);
				parseExpression();
				accept(TokenKind.SEMICOLON);
				return;
			}
			//id ( . id)* ( = Expression | '('ArgumentList?')' ) ;
			else if(token.kind == TokenKind.DOT){
				while(token.kind == TokenKind.DOT){
					acceptIt();
					accept(TokenKind.ID);
				}
				// id ( . id)* = Expression ;
				if(token.kind == TokenKind.ASSIGNMENTEQUAL){
					acceptIt();
					parseExpression();
					accept(TokenKind.SEMICOLON);
					return;
				}
				// id ( . id)* '(' ArgumentList? ')' ;
				else if(token.kind == TokenKind.LEFTPAREN){
					acceptIt();
					if(token.kind != TokenKind.RIGHTPAREN){
						parseArgumentList();
					}
					accept(TokenKind.RIGHTPAREN);
					accept(TokenKind.SEMICOLON);
					return;
				}
				else{
					parseError("Invalid Term - expecting ASSIGNMENTEQUAL or LEFTPAREN but found "
							+ token.kind);
				}
			} else {
				parseError("Invalid Term - expecting LEFTSQUAREBRACKET, ID, or DOT but found "
						+ token.kind);
			}

		case INT: //int ([])? id = Expression ;
			acceptIt();
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				acceptIt();
				accept(TokenKind.RIGHTSQUAREBRACKET);
			}
			accept(TokenKind.ID);
			accept(TokenKind.ASSIGNMENTEQUAL);
			parseExpression();
			accept(TokenKind.SEMICOLON);
			return;
		case BOOLEAN: //boolean id = Expression ; 
			acceptIt();
			accept(TokenKind.ID);
			accept(TokenKind.ASSIGNMENTEQUAL);
			parseExpression();
			accept(TokenKind.SEMICOLON);
			return;
		case THIS: // this ( . id )* ( = Expression | '('ArgumentList?')' ) ;
			acceptIt();
			while(token.kind == TokenKind.DOT){
				acceptIt();
				accept(TokenKind.ID);
			}
			//this ( . id)* '=' Expression ;
			if(token.kind == TokenKind.ASSIGNMENTEQUAL){
				acceptIt();
				parseExpression();
				accept(TokenKind.SEMICOLON);
				return;
			}
			//this ( . id)* '(' ArgumentList? ')' ;
			else if(token.kind == TokenKind.LEFTPAREN){
				acceptIt();
				if(token.kind != TokenKind.RIGHTPAREN){
					parseArgumentList();
				}
				accept(TokenKind.RIGHTPAREN);
				accept(TokenKind.SEMICOLON);
				return;
			}
			else{
				parseError("Invalid Term - expecting LEFTPAREN or ASSIGNMENTEQUAL but found "
						+ token.kind);
			}
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
			parseError("Invalid Term - expecting LEFTBRACKET, ID, INT, BOOLEAN, THIS, RETURN, IF, or WHILE but found " + token.kind);
		}
		
	}
	
	/*Expression ::=
	 * (unop)*
	 * (
	 * 		unop Expression
	 *		| NonPrimitiveReferenceOrArrayReference ( ( ArgumentList? ) )? 
	 *		| '(' Expression ')'
	 *		| num | true | false
	 *		| new (id ( ( ) | [ Expression ] ) | int [ Expression ])
	 * )
	 * (binop Expression)*
	 * 
	 */
	private void parseExpression(){
		//( unop )*
		while(isUnop(token)){
			acceptIt();
		}
		switch(token.kind){
		case ID:
			acceptIt();
			// id '[' Expression ']'
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				acceptIt();
				parseExpression();
				accept(TokenKind.RIGHTSQUAREBRACKET);
				break;
			}
			// id ( . id)* ( '(' ArgumentList? ')' )? 
			else if(token.kind == TokenKind.LEFTPAREN || token.kind == TokenKind.DOT){
				while(token.kind == TokenKind.DOT){
					acceptIt();
					accept(TokenKind.ID);
				}
				if(token.kind == TokenKind.LEFTPAREN){
					accept(TokenKind.LEFTPAREN);
					if(token.kind != TokenKind.RIGHTPAREN){
						parseArgumentList();
					}
					accept(TokenKind.RIGHTPAREN);
				}
				break;
			}
			else{
				break;
			}
		//this ( . id )* ( '(' ArgumentList? ')' )?
		case THIS:
			acceptIt();
			while(token.kind == TokenKind.DOT){
				acceptIt();
				accept(TokenKind.ID);
			}
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
			acceptIt();
			if(token.kind == TokenKind.ID){
				//new id '(' ')'
				acceptIt();
				if(token.kind == TokenKind.LEFTPAREN){
					acceptIt();
					accept(TokenKind.RIGHTPAREN);
					break;
				}
				//new id '[' Expression ']'
				else if(token.kind == TokenKind.LEFTSQUAREBRACKET){
					acceptIt();
					parseExpression();
					accept(TokenKind.LEFTSQUAREBRACKET);
					break;
				}
				else{
					parseError("Invalid Term - expecting LEFTSQUAREBRACKET or LEFTPAREN but found " + token.kind);
				}
					
			}
			//new int '[' Expression ']'
			else if(token.kind == TokenKind.INT){
				acceptIt();
				accept(TokenKind.LEFTSQUAREBRACKET);
				parseExpression();
				accept(TokenKind.RIGHTSQUAREBRACKET);
			}
			else{
				parseError("Invalid Term - expecting ID or INT but found " + token.kind);
			}
			break;
		default:
			parseError("Invalid Term - expecting ID, THIS, LEFTPAREN, NUM, TRUE, FALSE, or NEW but found "
					+ token.kind);
		}
		//	(binop Expression)*
		while (isBinop(token)) {
			acceptIt();
			parseExpression();
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
