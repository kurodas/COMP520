package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassDeclList;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.ExprList;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FieldDeclList;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IndexedRef;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxAssignStmt;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodDeclList;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.NullLiteral;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.QualifiedRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.Reference;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.Type;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.WhileStmt;

public class Parser {

	private Scanner scanner;
	private ErrorReporter reporter;
	private Token token;
	private boolean trace = true;

//	private SourcePosition previousTokenPosition;
	
	public Parser(Scanner scanner, ErrorReporter reporter) {
		this.scanner = scanner;
		this.reporter = reporter;
//		previousTokenPosition = new SourcePosition();
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
	public AST parse() {
		token = scanner.scan();
		AST p = parseProgram();
		return p;
	}
	
	//Program ::= (ClassDeclaration)* eot
	private Package parseProgram() {
		SourcePosition programPosition = token.posn;
		ClassDeclList classDeclList = new ClassDeclList();
		while (token.kind != TokenKind.EOT) {
			classDeclList.add(parseClassDeclaration());
		}
		accept(TokenKind.EOT);
		return new Package(classDeclList, programPosition);
	}
	
	//ClassDeclaration ::= class id { ( FieldOrMethodDeclaration )* } 
	private ClassDecl parseClassDeclaration() throws SyntaxError{
		SourcePosition classDeclPosition = token.posn;
		accept(TokenKind.CLASS);
		String className = accept(TokenKind.ID);
		accept(TokenKind.LEFTBRACKET);
		FieldDeclList fieldDeclList = new FieldDeclList();
		MethodDeclList methodDeclList = new MethodDeclList();
		while (token.kind != TokenKind.RIGHTBRACKET) {
			Object fieldOrMethodDecl = parseFieldOrMethodDeclaration();
			if(fieldOrMethodDecl instanceof FieldDecl){
				fieldDeclList.add((FieldDecl) fieldOrMethodDecl);
			}
			else{
				methodDeclList.add((MethodDecl) fieldOrMethodDecl);
			}
		}
		accept(TokenKind.RIGHTBRACKET);
		return new ClassDecl(className, fieldDeclList, methodDeclList,classDeclPosition);
	}
	/*
	 * FieldOrMethodDeclaration = Visibility Access
			(
			Type  id  ( ; | (ParameterList?) { Statement* } ) 
			| void id (ParameterList?) { Statement* } 
			)
	 */
	private Object parseFieldOrMethodDeclaration(){
		SourcePosition currentPos = token.posn;
		boolean isPrivate = parseVisibility();
		boolean isStatic = parseAccess();
		switch (token.kind) {
		case ID: case INT: case BOOLEAN:{
			return parseFieldOrNonVoidMethodDeclaration(isPrivate, isStatic, currentPos);
		}
		case VOID: {
			return parseVoidMethodDeclaration(isPrivate, isStatic, currentPos);
		}
		default:
			parseError("Invalid Term - expecting ID, INT, BOOLEAN, or VOID but found "
					+ token.kind);
			return null;
		}
	}
	//Visibility ::= (public | private)?
	private boolean parseVisibility(){
		boolean isPrivate;
		if(token.kind == TokenKind.PRIVATE)
			isPrivate = true;
		else
			isPrivate = false;
		if (token.kind == TokenKind.PUBLIC || token.kind == TokenKind.PRIVATE){
				acceptIt();
		}
		return isPrivate;
	}
	
	//Access ::= static?
	private boolean parseAccess(){
		boolean isStatic = false;
		if(token.kind == TokenKind.STATIC){
			acceptIt();
			isStatic = true;
		}
		return isStatic;
	}
	/*
	 * Type  id  ( ; | '(' ParameterList? ')' '{' Statement* '}' ) 
	 */
	private Object parseFieldOrNonVoidMethodDeclaration(boolean isPrivate, boolean isStatic, SourcePosition pos) {
		Type type = parseType();
		String name = accept(TokenKind.ID);
		switch (token.kind) {
		//Field declaration
		case SEMICOLON:
			accept(TokenKind.SEMICOLON);
			return new FieldDecl(isPrivate, isStatic, type, name, pos);
		//	'(' ParameterList? ')' '{' Statement* '}' 
		// Method declaration
		case LEFTPAREN: {
			accept(TokenKind.LEFTPAREN);
			//Initialize empty list in case there are no parameters
			ParameterDeclList parameterDeclList = new ParameterDeclList();
			if (token.kind != TokenKind.RIGHTPAREN) {
				parameterDeclList = parseParameterList();
			}
			accept(TokenKind.RIGHTPAREN);
			accept(TokenKind.LEFTBRACKET);
			//Initialize empty list in case there are no statements
			StatementList statementList = new StatementList();
			while (token.kind != TokenKind.RIGHTBRACKET)
				statementList.add(parseStatement());
			accept(TokenKind.RIGHTBRACKET);
			return new MethodDecl(new FieldDecl(isPrivate, isStatic, type, name, pos), 
					parameterDeclList, statementList, pos);
		}
		default:
			parseError("Invalid Term - expecting SEMICOLON or LEFTPAREN but found "
					+ token.kind);
			return null;
		}
	}

	
	//VoidMethodDeclaration ::= void id '(' ParameterList? ')' { Statement* } 
	private MethodDecl parseVoidMethodDeclaration(boolean isPrivate, boolean isStatic, SourcePosition pos) {
		SourcePosition currentPos = token.posn;
		accept(TokenKind.VOID);
		String methodName = accept(TokenKind.ID);
		accept(TokenKind.LEFTPAREN);
		//Initialize empty list in case there are no parameters
		ParameterDeclList parameterDeclList = new ParameterDeclList();
		if(token.kind != TokenKind.RIGHTPAREN)
			parameterDeclList = parseParameterList();
		accept(TokenKind.RIGHTPAREN);
		accept(TokenKind.LEFTBRACKET);
		//Initialize empty list in case there are no statements
		StatementList statementList = new StatementList();
		while (token.kind != TokenKind.RIGHTBRACKET)
			statementList.add(parseStatement());
		accept(TokenKind.RIGHTBRACKET);
		return new MethodDecl(new FieldDecl(isPrivate, isStatic, new BaseType(TypeKind.VOID, currentPos), methodName, pos), parameterDeclList, statementList, pos);
	}
	
	private Type parseType(){
		SourcePosition currentPos = token.posn;
		switch(token.kind){
		case INT:
			acceptIt();
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				//int[]
				acceptIt();
				accept(TokenKind.RIGHTSQUAREBRACKET);
				return new ArrayType(new BaseType(TypeKind.INT, currentPos), currentPos);
			}
			//int
			return new BaseType(TypeKind.INT, currentPos);
		case ID:
			Identifier className = new Identifier(token);
			acceptIt();
			//id[]
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				acceptIt();
				accept(TokenKind.RIGHTSQUAREBRACKET);
				return new ArrayType(new ClassType(className, currentPos), currentPos);
			}
			//id
			return new ClassType(className, currentPos);
		case BOOLEAN:
			//boolean
			acceptIt();
			return new BaseType(TypeKind.BOOLEAN, currentPos);
		default:
			parseError("Invalid Term - expecting INT, ID, or BOOLEAN but found "
					+ token.kind);
			return null;
		}
	}
		
	private ParameterDeclList parseParameterList(){
		SourcePosition currentPos = token.posn;
		ParameterDeclList parameterDeclList = new ParameterDeclList();
		Type parameterType = parseType();
		String parameterName = accept(TokenKind.ID);
		parameterDeclList.add(new ParameterDecl(parameterType, parameterName, currentPos));
		//	( , Type id )*
		while(token.kind == TokenKind.COMMA){
			accept(TokenKind.COMMA);
			currentPos = token.posn;
			parameterType = parseType();
			parameterName = accept(TokenKind.ID);
			parameterDeclList.add(new ParameterDecl(parameterType, parameterName, currentPos));
		}
		return parameterDeclList;
	}
	
	//ArgumentList ::= Expression (',' Expression)*
	private ExprList parseArgumentList(){
		ExprList argumentList = new ExprList();
		argumentList.add(parseExprA());
		//	(',' Expression)*
		while(token.kind == TokenKind.COMMA){
			accept(TokenKind.COMMA);
			argumentList.add(parseExprA());
		}
		return argumentList;
	}
	
	/*
	 * 
	 */
	private Statement parseStatement(){
		SourcePosition currentPos = token.posn;
		switch(token.kind){
		//	{ Statement* }
		case LEFTBRACKET:
			accept(TokenKind.LEFTBRACKET);
			StatementList statementList = new StatementList();
			while(token.kind != TokenKind.RIGHTBRACKET){
				statementList.add(parseStatement());
			}
			accept(TokenKind.RIGHTBRACKET);
			return new BlockStmt(statementList, currentPos);
		case ID: 
			ClassType classType = new ClassType(new Identifier(token), currentPos);
			IdRef idRef = new IdRef(new Identifier(token), currentPos);
			acceptIt();
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				acceptIt();
				// id[] id = Expression ;
				if(token.kind == TokenKind.RIGHTSQUAREBRACKET){
					acceptIt();
					return parseIDArrayDeclarationStatement(classType);
				}
				else{ //id [ Expression ] = Expression ;
					return parseIDIndexedAssignStatement(idRef);
				}
			}
			//id id = Expression ;
			else if(token.kind == TokenKind.ID){
				String variableName = acceptIt();
				return parseIDDeclarationStatement(classType, variableName);
			}
			//id ( . id)* ( = Expression | '('ArgumentList?')' ) ;
			else if(token.kind == TokenKind.DOT 
					|| token.kind == TokenKind.ASSIGNMENTEQUAL
					|| token.kind == TokenKind.LEFTPAREN){
				return parseQualifiedStatements(idRef);
			} else {
				parseError("Invalid Term - expecting LEFTSQUAREBRACKET, ID, DOT, or ASSIGNMENTEQUAL but found "
						+ token.kind);
				//Never reached
				return null;
			}

		case INT: 
			return parseIntOrIntArrayDeclarationStatement();
		case BOOLEAN:  
			return parseBooleanDeclarationStatement();
		case THIS: // this ( . id )* ( = Expression | '('ArgumentList?')' ) ;
			acceptIt();
			Reference thisRef = new ThisRef(currentPos);
			return parseQualifiedStatements(thisRef);
		case RETURN:
			return parseReturnStatement();
		case IF:
			return parseIfStatement();
		case WHILE:
			return parseWhileStatement();
		default:
			parseError("Invalid Term - expecting LEFTBRACKET, ID, INT, BOOLEAN, THIS, RETURN, IF, or WHILE but found " + token.kind);
			return null;
		}
	}
	
	private Statement parseIDArrayDeclarationStatement(Type classType){
		String variableName = accept(TokenKind.ID);
		accept(TokenKind.ASSIGNMENTEQUAL);
		Expression expression = parseExprA();
		accept(TokenKind.SEMICOLON);
		return new VarDeclStmt(new VarDecl(new ArrayType(classType, classType.posn),
				variableName, classType.posn), expression, classType.posn);
	}
	
	private Statement parseIDIndexedAssignStatement(IdRef idRef){
		Expression arrayIndexExpression = parseExprA();
		accept(TokenKind.RIGHTSQUAREBRACKET);
		accept(TokenKind.ASSIGNMENTEQUAL);
		Expression assigningExpression = parseExprA();
		accept(TokenKind.SEMICOLON);
		return new IxAssignStmt(
				new IndexedRef(idRef, arrayIndexExpression, idRef.posn), 
					assigningExpression, idRef.posn);
	}
	
	private Statement parseIDDeclarationStatement(Type classType, String variableName){
		accept(TokenKind.ASSIGNMENTEQUAL);
		Expression expression = parseExprA();
		accept(TokenKind.SEMICOLON);
		return new VarDeclStmt(new VarDecl(classType, variableName,classType.posn),expression,classType.posn);
	}
	
	//int ([])? id = Expression ;
	private Statement parseIntOrIntArrayDeclarationStatement(){
		SourcePosition currentPos = token.posn;
		accept(TokenKind.INT);
		Type type = new BaseType(TypeKind.INT,currentPos);
		if(token.kind == TokenKind.LEFTSQUAREBRACKET){
			acceptIt();
			accept(TokenKind.RIGHTSQUAREBRACKET);
			type = new ArrayType(type, currentPos);
		}
		String name = accept(TokenKind.ID);
		accept(TokenKind.ASSIGNMENTEQUAL);
		Expression expr = parseExprA();
		accept(TokenKind.SEMICOLON);
		return new VarDeclStmt(new VarDecl(type, name, currentPos), expr, currentPos);
	}
	
	//boolean id = Expression ;
	private Statement parseBooleanDeclarationStatement(){
		SourcePosition currentPos = token.posn;
		accept(TokenKind.BOOLEAN);
		Type type = new BaseType(TypeKind.BOOLEAN,currentPos);
		String name = accept(TokenKind.ID);
		accept(TokenKind.ASSIGNMENTEQUAL);
		Expression expr = parseExprA();
		accept(TokenKind.SEMICOLON);
		return new VarDeclStmt(new VarDecl(type, name, currentPos), expr, currentPos);
	}
	
	//	return Expression? ;
	private Statement parseReturnStatement(){
		SourcePosition currentPos = token.posn;
		accept(TokenKind.RETURN);
		Expression expr = null;
		if(token.kind != TokenKind.SEMICOLON){
			expr = parseExprA();
		}
		accept(TokenKind.SEMICOLON);
		return new ReturnStmt(expr, currentPos);
	}
	
	private Statement parseIfStatement(){
		SourcePosition currentPos = token.posn;
		accept(TokenKind.IF);
		accept(TokenKind.LEFTPAREN);
		Expression ifCondition = parseExprA();
		accept(TokenKind.RIGHTPAREN);
		Statement ifStmt = parseStatement();
		Statement elseStmt = null;
		if(token.kind == TokenKind.ELSE){
			accept(TokenKind.ELSE);
			elseStmt = parseStatement();
		}
		return new IfStmt(ifCondition, ifStmt, elseStmt, currentPos);
	}
	//	while '(' Expression ')' Statement
	private Statement parseWhileStatement(){
		SourcePosition currentPos = token.posn;
		accept(TokenKind.WHILE);
		accept(TokenKind.LEFTPAREN);
		Expression whileCondition = parseExprA();
		accept(TokenKind.RIGHTPAREN);
		Statement loopContent = parseStatement();
		return new WhileStmt(whileCondition, loopContent,currentPos);
	}
	
	private Statement parseQualifiedStatements(Reference baseRef){
		QualifiedRef qRef = null;
		Identifier id = null;
		if (token.kind == TokenKind.DOT) {
			acceptIt();
			SourcePosition currentPos = token.posn;
			id = new Identifier(token);
			accept(TokenKind.ID);
			qRef = new QualifiedRef(baseRef,id,currentPos);
			while (token.kind == TokenKind.DOT) {
				acceptIt();
				currentPos = token.posn;
				id = new Identifier(token);
				accept(TokenKind.ID);
				qRef = new QualifiedRef(qRef, id, currentPos);
			}
		}
		// id ( . id)* = Expression ;
		if(token.kind == TokenKind.ASSIGNMENTEQUAL){
			acceptIt();
			Expression assignmentExpression = parseExprA();
			accept(TokenKind.SEMICOLON);
			//If there were qualified references, use qRef
			if(qRef != null){
				return new AssignStmt(qRef, assignmentExpression, qRef.posn);
			}
			//Else use baseRef
			else{
				return new AssignStmt(baseRef, assignmentExpression, baseRef.posn);
			}
		}
		// id ( . id)* '(' ArgumentList? ')' ;
		else if(token.kind == TokenKind.LEFTPAREN){
			acceptIt();
			ExprList argList = new ExprList();
			if(token.kind != TokenKind.RIGHTPAREN){
				argList = parseArgumentList();
			}
			accept(TokenKind.RIGHTPAREN);
			accept(TokenKind.SEMICOLON);
			//If there were qualified references, use qRef
			if(qRef != null){
				return new CallStmt(qRef, argList, qRef.posn);
			}
			//Else use baseRef
			else{
				return new CallStmt(baseRef, argList, baseRef.posn);
			}
		}
		else{
			parseError("Invalid Term - expecting ASSIGNMENTEQUAL or LEFTPAREN but found "
					+ token.kind);
			//Never reached
			return null;
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
	
	/*
	 * S ::= A$
	 * A ::= B (|| B)*
	 * B ::= C (&& C)*
	 * C ::= D ((== | !=) D)*
	 * D ::= E ((<= | < | > | >=) E)*
	 * E ::= F ((+|-) F)*
	 * F ::= G ((* | /) G)*
	 * G ::= (- | !)* H
	 * H ::= ( A ) | I
	 * I ::= All other expression forms
	 */
	private Expression parseExprA(){
		SourcePosition currentPos = token.posn;
		Expression expr = parseExprB();
		while(token.kind == TokenKind.OR){
			Operator oper = new Operator(token);
			acceptIt();
			Expression secondExpr = parseExprB();
			expr = new BinaryExpr(oper, expr, secondExpr, currentPos);
		}
		return expr;
	}
	
	private Expression parseExprB(){
		SourcePosition currentPos = token.posn;
		Expression expr = parseExprC();
		while(token.kind == TokenKind.AND){
			Operator oper = new Operator(token);
			acceptIt();
			Expression secondExpr = parseExprC();
			expr = new BinaryExpr(oper, expr, secondExpr, currentPos);
		}
		return expr;
	}
	
	private Expression parseExprC(){
		SourcePosition currentPos = token.posn;
		Expression expr = parseExprD();
		while (token.kind == TokenKind.LOGICALEQUAL
				|| token.kind == TokenKind.NOTEQUAL) {
			Operator oper = new Operator(token);
			acceptIt();
			Expression secondExpr = parseExprD();
			expr = new BinaryExpr(oper, expr, secondExpr, currentPos);
		}
		return expr;
	}
	private Expression parseExprD(){
		SourcePosition currentPos = token.posn;
		Expression expr = parseExprE();
		while (token.kind == TokenKind.LESSTHAN
				|| token.kind == TokenKind.LESSTHANEQUAL
				|| token.kind == TokenKind.GREATERTHAN
				|| token.kind == TokenKind.GREATERTHANEQUAL) {
			Operator oper = new Operator(token);
			acceptIt();
			Expression secondExpr = parseExprE();
			expr = new BinaryExpr(oper, expr, secondExpr, currentPos);
		}
		return expr;
	}
	private Expression parseExprE(){
		SourcePosition currentPos = token.posn;
		Expression expr = parseExprF();
		while (token.kind == TokenKind.PLUS
				|| token.kind == TokenKind.MINUSORARITHMETICNEGATIVE) {
			Operator oper = new Operator(token);
			acceptIt();
			Expression secondExpr = parseExprF();
			expr = new BinaryExpr(oper, expr, secondExpr, currentPos);
		}
		return expr;
	}
	private Expression parseExprF(){
		SourcePosition currentPos = token.posn;
		Expression expr = parseExprG();
		while (token.kind == TokenKind.TIMES
				|| token.kind == TokenKind.DIVIDE) {
			Operator oper = new Operator(token);
			acceptIt();
			Expression secondExpr = parseExprG();
			expr = new BinaryExpr(oper, expr, secondExpr, currentPos);
		}
		return expr;
	}
	private Expression parseExprG(){
		SourcePosition currentPos = token.posn;
		Expression expr = null;
		if (token.kind == TokenKind.LOGICALNEGATIVE
				|| token.kind == TokenKind.MINUSORARITHMETICNEGATIVE) {
			Operator oper = new Operator(token);
			acceptIt();
			Expression innerExpr = parseExprG();
			expr = new UnaryExpr(oper, innerExpr, currentPos);
//			while (token.kind == TokenKind.LOGICALNEGATIVE
//					|| token.kind == TokenKind.MINUSORARITHMETICNEGATIVE) {
//				oper = new Operator(token);
//				acceptIt();
//				innerExpr = parseExprG();
//				expr = new UnaryExpr(oper, innerExpr, null);
//			}
		}
		else{
			expr = parseExprH();
		}
		return expr;
	}
	private Expression parseExprH(){
		Expression expr;
		if(token.kind == TokenKind.LEFTPAREN){
			acceptIt();
			expr = parseExprA();
			accept(TokenKind.RIGHTPAREN);
		}
		else{
			expr = parseExprI();
		}
		return expr;
	}

	private Expression parseExprI(){
		SourcePosition currentPos = token.posn;
		switch(token.kind){
		case ID:
			IdRef idRef = new IdRef(new Identifier(token), currentPos);
			acceptIt();
			// id '[' Expression ']'
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				acceptIt();
				Expression indexExpression = parseExprA();
				accept(TokenKind.RIGHTSQUAREBRACKET);
				return new RefExpr(new IndexedRef(idRef, indexExpression, currentPos), currentPos);
			}
			// id ( . id)* ( '(' ArgumentList? ')' )? 
			else if(token.kind == TokenKind.LEFTPAREN || token.kind == TokenKind.DOT){
				return parseQualifiedExpressions(idRef);
			}
			// id
			else{
				return new RefExpr(idRef, currentPos);
			}
		//this ( . id )* ( '(' ArgumentList? ')' )?
		case THIS:
			acceptIt();
			Reference thisRef = new ThisRef(currentPos);
			return parseQualifiedExpressions(thisRef);
		//	num | true | false
		case NUM:
		case TRUE:
		case FALSE:
		case NULL:
			return parseLiteralExpr();
		//	new (id ( ( ) | [ Expression ] ) | int [ Expression ])
		case NEW:
			acceptIt();
			return parseNewExpressions();
		default:
			parseError("Invalid Term - expecting ID, THIS, LEFTPAREN, NUM, TRUE, FALSE, or NEW but found "
					+ token.kind);
			//Never reached
			return null;
		}
	}
	
	private Expression parseQualifiedExpressions(Reference baseRef){
		QualifiedRef qRef = null;
		Identifier id = null;
		if (token.kind == TokenKind.DOT) {
			acceptIt();
			SourcePosition currentPos = token.posn;
			id = new Identifier(token);
			accept(TokenKind.ID);
			qRef = new QualifiedRef(baseRef,id,currentPos);
			while (token.kind == TokenKind.DOT) {
				acceptIt();
				currentPos = token.posn;
				id = new Identifier(token);
				accept(TokenKind.ID);
				qRef = new QualifiedRef(qRef, id, currentPos);
			}
		}
		ExprList argList = new ExprList();
		if (token.kind == TokenKind.LEFTPAREN) {
			accept(TokenKind.LEFTPAREN);
			if (token.kind != TokenKind.RIGHTPAREN) {
				argList = parseArgumentList();
			}
			accept(TokenKind.RIGHTPAREN);
			// If there were qualified references, use qRef
			if (qRef != null) {
				return new CallExpr(qRef, argList, qRef.posn);
			}
			// Else use baseRef
			else {
				return new CallExpr(baseRef, argList, baseRef.posn);
			}
		}
		// id ( . id)* 
		//If there were qualified references, use qRef
		else if(qRef != null){
			return new RefExpr(qRef,qRef.posn);
		}
		//Else use baseRef
		else{
			return new RefExpr(baseRef, baseRef.posn);
		}
	}
	
	private LiteralExpr parseLiteralExpr(){
		SourcePosition currentPos = token.posn;
		LiteralExpr expr = null;
		switch(token.kind){
		case NUM:
			expr = new LiteralExpr(new IntLiteral(token), currentPos);
			acceptIt();
			break;
		case TRUE:
		case FALSE:
			expr = new LiteralExpr(new BooleanLiteral(token),currentPos);
			acceptIt();
			break;
		case NULL:
			expr = new LiteralExpr(new NullLiteral(token), currentPos);
			acceptIt();
			break;
		default:
			parseError("Invalid Term - expecting NUM, TRUE, or FALSE but found "
					+ token.kind);
		}
		return expr;
	}
	
	private Expression parseNewExpressions(){
		SourcePosition currentPos = token.posn;
		if(token.kind == TokenKind.ID){
			//new id '(' ')'
			Identifier classId = new Identifier(token);
			ClassType classType = new ClassType(classId, currentPos);
			acceptIt();
			if(token.kind == TokenKind.LEFTPAREN){
				acceptIt();
				accept(TokenKind.RIGHTPAREN);
				return new NewObjectExpr(classType, currentPos);
			}
			//new id '[' Expression ']'
			else if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				acceptIt();
				Expression arraySizeExpr = parseExprA();
				accept(TokenKind.RIGHTSQUAREBRACKET);
				return new NewArrayExpr(classType, arraySizeExpr, currentPos);
			}
			else{
				parseError("Invalid Term - expecting LEFTSQUAREBRACKET or LEFTPAREN but found " + token.kind);
				//Never reached
				return null;
			}
				
		}
		else if(token.kind == TokenKind.INT){
			return parseNewIntArrayExpression(currentPos);
		}
		else{
			parseError("Invalid Term - expecting ID or INT but found " + token.kind);
			//Never reached
			return null;
		}
	}
	
	//new int '[' Expression ']'
	private Expression parseNewIntArrayExpression(SourcePosition newPos){
		SourcePosition currentPos = token.posn;
		accept(TokenKind.INT);
		accept(TokenKind.LEFTSQUAREBRACKET);
		Expression arraySizeExpr = parseExprA();
		accept(TokenKind.RIGHTSQUAREBRACKET);
		return new NewArrayExpr(new BaseType(TypeKind.INT, currentPos), arraySizeExpr, newPos);
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
	private String acceptIt() throws SyntaxError {
		return accept(token.kind);
	}

	/**
	 * verify that current token in input matches expected token and advance to next token
	 * @param expectedToken
	 * @throws SyntaxError  if match fails
	 */
	private String accept(TokenKind expectedTokenKind) throws SyntaxError {
		if (token.kind == expectedTokenKind) {
			if (trace)
				pTrace();
			String tokenSpelling = token.spelling;
//			previousTokenPosition = token.posn;
			token = scanner.scan();
			return tokenSpelling;
		}
		else{
			parseError("expecting '" + expectedTokenKind +
					"' but found '" + token.kind + "'");
			return null;
		}
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
