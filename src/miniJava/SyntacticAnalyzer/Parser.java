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
	public AST parse() {
		token = scanner.scan();
		AST p = parseProgram();
		return p;
	}
	
	//Program ::= (ClassDeclaration)* eot
	private Package parseProgram() {
		ClassDeclList classDeclList = new ClassDeclList();
		while (token.kind != TokenKind.EOT) {
			classDeclList.add(parseClassDeclaration());
		}
		accept(TokenKind.EOT);
		return new Package(classDeclList, null);
	}
	
	//ClassDeclaration ::= class id { ( FieldOrMethodDeclaration )* } 
	private ClassDecl parseClassDeclaration() {
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
		return new ClassDecl(className, fieldDeclList, methodDeclList,null);
	}
	/*
	 * FieldOrMethodDeclaration = Visibility Access
			(
			Type  id  ( ; | (ParameterList?) { Statement* } ) 
			| void id (ParameterList?) { Statement* } 
			)
	 */
	private Object parseFieldOrMethodDeclaration(){
		boolean isPrivate = parseVisibility();
		boolean isStatic = parseAccess();
		switch (token.kind) {
		case ID: case INT: case BOOLEAN:{
			return parseFieldOrNonVoidMethodDeclaration(isPrivate, isStatic);
		}
		case VOID: {
			return parseVoidMethodDeclaration(isPrivate, isStatic);
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
	private Object parseFieldOrNonVoidMethodDeclaration(boolean isPrivate, boolean isStatic) {
		Type type = parseType();
		String name = accept(TokenKind.ID);
		switch (token.kind) {
		case SEMICOLON:
			accept(TokenKind.SEMICOLON);
			return new FieldDecl(isPrivate, isStatic, type, name, null);
		//	'(' ParameterList? ')' '{' Statement* '}' 
		case LEFTPAREN: {
			accept(TokenKind.LEFTPAREN);
			ParameterDeclList parameterDeclList = null;
			if (token.kind != TokenKind.RIGHTPAREN) {
				parameterDeclList = parseParameterList();
			}
			accept(TokenKind.RIGHTPAREN);
			accept(TokenKind.LEFTBRACKET);
			StatementList statementList = new StatementList();
			while (token.kind != TokenKind.RIGHTBRACKET)
				statementList.add(parseStatement());
			accept(TokenKind.RIGHTBRACKET);
			return new MethodDecl(new FieldDecl(isPrivate, isStatic, type, name, null), 
					parameterDeclList, statementList, null);
		}
		default:
			parseError("Invalid Term - expecting SEMICOLON or LEFTPAREN but found "
					+ token.kind);
			return null;
		}
	}

	
	//VoidMethodDeclaration ::= void id '(' ParameterList? ')' { Statement* } 
	private MethodDecl parseVoidMethodDeclaration(boolean isPrivate, boolean isStatic) {
		accept(TokenKind.VOID);
		String methodName = accept(TokenKind.ID);
		accept(TokenKind.LEFTPAREN);
		ParameterDeclList parameterDeclList = null;
		if(token.kind != TokenKind.RIGHTPAREN)
			parameterDeclList = parseParameterList();
		accept(TokenKind.RIGHTPAREN);
		accept(TokenKind.LEFTBRACKET);
		StatementList statementList = new StatementList();
		while (token.kind != TokenKind.RIGHTBRACKET)
			statementList.add(parseStatement());
		accept(TokenKind.RIGHTBRACKET);
		return new MethodDecl(new FieldDecl(isPrivate, isStatic, new BaseType(TypeKind.VOID, null), methodName, null), parameterDeclList, statementList, null);
	}
	
	private Type parseType(){
		switch(token.kind){
		case INT:
			acceptIt();
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				acceptIt();
				accept(TokenKind.RIGHTSQUAREBRACKET);
				return new ArrayType(new BaseType(TypeKind.INT, null), null);
			}
			return new BaseType(TypeKind.INT, null);
		case ID:
			Identifier className = new Identifier(token);
			acceptIt();
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				acceptIt();
				accept(TokenKind.RIGHTSQUAREBRACKET);
				return new ArrayType(new ClassType(className, null), null);
			}
			return new ClassType(className, null);
		case BOOLEAN:
			acceptIt();
			return new BaseType(TypeKind.BOOLEAN, null);
		default:
			parseError("Invalid Term - expecting INT, ID, or BOOLEAN but found "
					+ token.kind);
			return null;
		}
	}
	
//	private Reference parseReference(){
//		switch(token.kind){
//		case THIS: case ID:
//			acceptIt();
//			while(token.kind == TokenKind.DOT){
//				acceptIt();
//				accept(TokenKind.ID);
//			}
//		default:
//			parseError("Invalid Term - expecting THIS or ID but found "
//					+ token.kind);
//		}
//	}
//	
//	private IndexedRef parseArrayReference(){
//		accept(TokenKind.ID);
//		accept(TokenKind.LEFTSQUAREBRACKET);
//		parseExpression();
//		accept(TokenKind.RIGHTSQUAREBRACKET);
//	}
		
	private ParameterDeclList parseParameterList(){
		ParameterDeclList parameterDeclList = new ParameterDeclList();
		Type parameterType = parseType();
		String parameterName = accept(TokenKind.ID);
		parameterDeclList.add(new ParameterDecl(parameterType, parameterName, null));
		//	( , Type id )*
		while(token.kind == TokenKind.COMMA){
			accept(TokenKind.COMMA);
			parameterType = parseType();
			parameterName = accept(TokenKind.ID);
			parameterDeclList.add(new ParameterDecl(parameterType, parameterName, null));
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
		String name;
		Type type;
		Expression expr;
		Identifier id;
		switch(token.kind){
		//	{ Statement* }
		case LEFTBRACKET:
			accept(TokenKind.LEFTBRACKET);
			StatementList statementList = new StatementList();
			while(token.kind != TokenKind.RIGHTBRACKET){
				statementList.add(parseStatement());
			}
			accept(TokenKind.RIGHTBRACKET);
			return new BlockStmt(statementList, null);
		case ID: 
			ClassType classType = new ClassType(new Identifier(token), null);
			IdRef idRef = new IdRef(new Identifier(token), null);
			acceptIt();
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				acceptIt();
				// id[] id = Expression ;
				if(token.kind == TokenKind.RIGHTSQUAREBRACKET){
					acceptIt();
					String variableName = accept(TokenKind.ID);
					accept(TokenKind.ASSIGNMENTEQUAL);
					Expression expression = parseExprA();
					accept(TokenKind.SEMICOLON);
					return new VarDeclStmt(new VarDecl(new ArrayType(classType, null),variableName, null), expression, null);
				}
				else{ //id [ Expression ] = Expression ;
					Expression arrayIndexExpression = parseExprA();
					accept(TokenKind.RIGHTSQUAREBRACKET);
					accept(TokenKind.ASSIGNMENTEQUAL);
					Expression assigningExpression = parseExprA();
					accept(TokenKind.SEMICOLON);
					return new IxAssignStmt(
							new IndexedRef(idRef, arrayIndexExpression, null), 
								assigningExpression, null);
				}
			}
			//id id = Expression ;
			else if(token.kind == TokenKind.ID){
				String variableName = acceptIt();
				accept(TokenKind.ASSIGNMENTEQUAL);
				Expression expression = parseExprA();
				accept(TokenKind.SEMICOLON);
				return new VarDeclStmt(new VarDecl(classType, variableName,null),expression,null);
			}
			//id ( . id)* ( = Expression | '('ArgumentList?')' ) ;
			else if(token.kind == TokenKind.DOT 
					|| token.kind == TokenKind.ASSIGNMENTEQUAL
					|| token.kind == TokenKind.LEFTPAREN){
				QualifiedRef qRef = null;
				id = null;
				if (token.kind == TokenKind.DOT) {
					acceptIt();
					id = new Identifier(token);
					accept(TokenKind.ID);
					qRef = new QualifiedRef(idRef,id,null);
					while (token.kind == TokenKind.DOT) {
						acceptIt();
						id = new Identifier(token);
						accept(TokenKind.ID);
						qRef = new QualifiedRef(qRef, id, null);
					}
				}
				// id ( . id)* = Expression ;
				if(token.kind == TokenKind.ASSIGNMENTEQUAL){
					acceptIt();
					Expression assignmentExpression = parseExprA();
					accept(TokenKind.SEMICOLON);
					//If there were qualified references, use qRef
					if(qRef != null){
						return new AssignStmt(qRef, assignmentExpression, null);
					}
					//Else use idRef
					else{
						return new AssignStmt(idRef, assignmentExpression, null);
					}
				}
				// id ( . id)* '(' ArgumentList? ')' ;
				else if(token.kind == TokenKind.LEFTPAREN){
					acceptIt();
					ExprList argList = null;
					if(token.kind != TokenKind.RIGHTPAREN){
						argList = parseArgumentList();
					}
					accept(TokenKind.RIGHTPAREN);
					accept(TokenKind.SEMICOLON);
					//If there were qualified references, use qRef
					if(qRef != null){
						return new CallStmt(qRef, argList, null);
					}
					//Else use idRef
					else{
						return new CallStmt(idRef, argList, null);
					}
				}
				else{
					parseError("Invalid Term - expecting ASSIGNMENTEQUAL or LEFTPAREN but found "
							+ token.kind);
				}
			} else {
				parseError("Invalid Term - expecting LEFTSQUAREBRACKET, ID, DOT, or ASSIGNMENTEQUAL but found "
						+ token.kind);
			}

		case INT: //int ([])? id = Expression ;
			acceptIt();
			type = new BaseType(TypeKind.INT,null);
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				acceptIt();
				accept(TokenKind.RIGHTSQUAREBRACKET);
				type = new ArrayType(type, null);
			}
			name = accept(TokenKind.ID);
			accept(TokenKind.ASSIGNMENTEQUAL);
			expr = parseExprA();
			accept(TokenKind.SEMICOLON);
			return new VarDeclStmt(new VarDecl(type, name, null), expr, null);
		case BOOLEAN: //boolean id = Expression ; 
			acceptIt();
			type = new BaseType(TypeKind.BOOLEAN,null);
			name = accept(TokenKind.ID);
			accept(TokenKind.ASSIGNMENTEQUAL);
			expr = parseExprA();
			accept(TokenKind.SEMICOLON);
			return new VarDeclStmt(new VarDecl(type, name, null), expr, null);
		case THIS: // this ( . id )* ( = Expression | '('ArgumentList?')' ) ;
			acceptIt();
			Reference thisRef = new ThisRef(null);
			QualifiedRef qRef = null;
			id = null;
			if (token.kind == TokenKind.DOT) {
				acceptIt();
				id = new Identifier(token);
				accept(TokenKind.ID);
				qRef = new QualifiedRef(thisRef,id,null);
				while (token.kind == TokenKind.DOT) {
					acceptIt();
					id = new Identifier(token);
					accept(TokenKind.ID);
					qRef = new QualifiedRef(qRef, id, null);
				}
			}
			//this ( . id)* '=' Expression ;
			if(token.kind == TokenKind.ASSIGNMENTEQUAL){
				acceptIt();
				expr = parseExprA();
				accept(TokenKind.SEMICOLON);
				//If there were qualified references, use qRef
				if(qRef != null){
					return new AssignStmt(qRef,expr,null);
				}
				//Else use idRef
				else{
					return new AssignStmt(thisRef,expr, null);
				}
			}
			//this ( . id)* '(' ArgumentList? ')' ;
			else if(token.kind == TokenKind.LEFTPAREN){
				acceptIt();
				ExprList argList = null;
				if(token.kind != TokenKind.RIGHTPAREN){
					argList = parseArgumentList();
				}
				accept(TokenKind.RIGHTPAREN);
				accept(TokenKind.SEMICOLON);
				//If there were qualified references, use qRef
				if(qRef != null){
					return new CallStmt(qRef, argList, null);
				}
				//Else use thisRef
				else{
					return new CallStmt(thisRef, argList, null);
				}
			}
			else{
				parseError("Invalid Term - expecting LEFTPAREN or ASSIGNMENTEQUAL but found "
						+ token.kind);
			}
		//	return Expression? ;
		case RETURN:
			accept(TokenKind.RETURN);
			expr = null;
			if(token.kind != TokenKind.SEMICOLON){
				expr = parseExprA();
			}
			accept(TokenKind.SEMICOLON);
			return new ReturnStmt(expr, null);
		//	if '(' Expression ')' Statement (else Statement)?
		case IF:
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
			return new IfStmt(ifCondition, ifStmt, elseStmt, null);
		//	while '(' Expression ')' Statement
		case WHILE:
			accept(TokenKind.WHILE);
			accept(TokenKind.LEFTPAREN);
			Expression whileCondition = parseExprA();
			accept(TokenKind.RIGHTPAREN);
			Statement loopContent = parseStatement();
			return new WhileStmt(whileCondition, loopContent,null);
		default:
			parseError("Invalid Term - expecting LEFTBRACKET, ID, INT, BOOLEAN, THIS, RETURN, IF, or WHILE but found " + token.kind);
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
		Expression expr = parseExprB();
		while(token.kind == TokenKind.OR){
			Operator oper = new Operator(token);
			acceptIt();
			Expression secondExpr = parseExprB();
			expr = new BinaryExpr(oper, expr, secondExpr, null);
		}
		return expr;
	}
	
	private Expression parseExprB(){
		Expression expr = parseExprC();
		while(token.kind == TokenKind.AND){
			Operator oper = new Operator(token);
			acceptIt();
			Expression secondExpr = parseExprC();
			expr = new BinaryExpr(oper, expr, secondExpr, null);
		}
		return expr;
	}
	
	private Expression parseExprC(){
		Expression expr = parseExprD();
		while (token.kind == TokenKind.LOGICALEQUAL
				|| token.kind == TokenKind.NOTEQUAL) {
			Operator oper = new Operator(token);
			acceptIt();
			Expression secondExpr = parseExprD();
			expr = new BinaryExpr(oper, expr, secondExpr, null);
		}
		return expr;
	}
	private Expression parseExprD(){
		Expression expr = parseExprE();
		while (token.kind == TokenKind.LESSTHAN
				|| token.kind == TokenKind.LESSTHANEQUAL
				|| token.kind == TokenKind.GREATERTHAN
				|| token.kind == TokenKind.GREATERTHANEQUAL) {
			Operator oper = new Operator(token);
			acceptIt();
			Expression secondExpr = parseExprE();
			expr = new BinaryExpr(oper, expr, secondExpr, null);
		}
		return expr;
	}
	private Expression parseExprE(){
		Expression expr = parseExprF();
		while (token.kind == TokenKind.PLUS
				|| token.kind == TokenKind.MINUSORARITHMETICNEGATIVE) {
			Operator oper = new Operator(token);
			acceptIt();
			Expression secondExpr = parseExprF();
			expr = new BinaryExpr(oper, expr, secondExpr, null);
		}
		return expr;
	}
	private Expression parseExprF(){
		Expression expr = parseUnaryExprG();
		while (token.kind == TokenKind.TIMES
				|| token.kind == TokenKind.DIVIDE) {
			Operator oper = new Operator(token);
			acceptIt();
			Expression secondExpr = parseUnaryExprG();
			expr = new BinaryExpr(oper, expr, secondExpr, null);
		}
		return expr;
	}
	private Expression parseUnaryExprG(){
		Expression expr = null;
		if (token.kind == TokenKind.LOGICALNEGATIVE
				|| token.kind == TokenKind.MINUSORARITHMETICNEGATIVE) {
			Operator oper = new Operator(token);
			acceptIt();
			Expression innerExpr = parseExprH();
			expr = new UnaryExpr(oper, innerExpr, null);
			while (token.kind == TokenKind.LOGICALNEGATIVE
					|| token.kind == TokenKind.MINUSORARITHMETICNEGATIVE) {
				oper = new Operator(token);
				acceptIt();
				innerExpr = parseExprH();
				expr = new UnaryExpr(oper, innerExpr, null);
			}
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
//		//( unop )*
//		while(isUnop(token)){
//			acceptIt();
//		}
		Expression expr;
		switch(token.kind){
		case ID:
			IdRef idRef = new IdRef(new Identifier(token), null);
			acceptIt();
			// id '[' Expression ']'
			if(token.kind == TokenKind.LEFTSQUAREBRACKET){
				acceptIt();
				Expression indexExpression = parseExprA();
				accept(TokenKind.RIGHTSQUAREBRACKET);
				expr = new RefExpr(new IndexedRef(idRef, indexExpression, null), null);
				break;
			}
			// id ( . id)* ( '(' ArgumentList? ')' )? 
			else if(token.kind == TokenKind.LEFTPAREN || token.kind == TokenKind.DOT){
				QualifiedRef qRef = null;
				Identifier id = null;
				if (token.kind == TokenKind.DOT) {
					acceptIt();
					id = new Identifier(token);
					accept(TokenKind.ID);
					qRef = new QualifiedRef(idRef,id,null);
					while (token.kind == TokenKind.DOT) {
						acceptIt();
						id = new Identifier(token);
						accept(TokenKind.ID);
						qRef = new QualifiedRef(qRef, id, null);
					}
				}
				ExprList argList = null;
				if (token.kind == TokenKind.LEFTPAREN) {
					accept(TokenKind.LEFTPAREN);
					if (token.kind != TokenKind.RIGHTPAREN) {
						argList = parseArgumentList();
					}
					accept(TokenKind.RIGHTPAREN);
					// If there were qualified references, use qRef
					if (qRef != null) {
						expr = new CallExpr(qRef, argList, null);
					}
					// Else use idRef
					else {
						expr = new CallExpr(idRef, argList, null);
					}
				}
				// id ( . id)* 
				//If there were qualified references, use qRef
				else if(qRef != null){
					expr = new RefExpr(qRef, null);
				}
				//Else use idRef
				else{
					expr = new RefExpr(idRef, null);
				}
			}
			else{
				expr = new RefExpr(idRef, null);
			}
			break;
		//this ( . id )* ( '(' ArgumentList? ')' )?
		case THIS:
			acceptIt();
			Reference thisRef = new ThisRef(null);
			QualifiedRef qRef = null;
			Identifier id = null;
			if (token.kind == TokenKind.DOT) {
				acceptIt();
				id = new Identifier(token);
				accept(TokenKind.ID);
				qRef = new QualifiedRef(thisRef,id,null);
				while (token.kind == TokenKind.DOT) {
					acceptIt();
					id = new Identifier(token);
					accept(TokenKind.ID);
					qRef = new QualifiedRef(qRef, id, null);
				}
			}
			ExprList argList = null;
			if(token.kind == TokenKind.LEFTPAREN){
				accept(TokenKind.LEFTPAREN);
				if(token.kind != TokenKind.RIGHTPAREN){
					argList = parseArgumentList();
				}
				accept(TokenKind.RIGHTPAREN);
				// If there were qualified references, use qRef
				if (qRef != null) {
					expr = new CallExpr(qRef, argList, null);
				}
				// Else use idRef
				else {
					expr = new CallExpr(thisRef, argList, null);
				}
			}// If there were qualified references, use qRef
			else if(qRef != null){
				expr = new RefExpr(qRef, null);
			}
			//Else use thisRef
			else{
				expr = new RefExpr(thisRef, null);
			}
			break;
//		//	'(' Expression ')'
//		case LEFTPAREN:
//			accept(TokenKind.LEFTPAREN);
//			expr = parseExpression();
//			accept(TokenKind.RIGHTPAREN);
//			break;
		//	num | true | false
		case NUM:
		case TRUE:
		case FALSE:
			expr = parseLiteralExpr();
			break;
		//	new (id ( ( ) | [ Expression ] ) | int [ Expression ])
		case NEW:
			acceptIt();
			if(token.kind == TokenKind.ID){
				//new id '(' ')'
				Identifier classId = new Identifier(token);
				ClassType classType = new ClassType(classId, null);
				acceptIt();
				if(token.kind == TokenKind.LEFTPAREN){
					acceptIt();
					accept(TokenKind.RIGHTPAREN);
					expr = new NewObjectExpr(classType, null);
					break;
				}
				//new id '[' Expression ']'
				else if(token.kind == TokenKind.LEFTSQUAREBRACKET){
					acceptIt();
					Expression arraySizeExpr = parseExprA();
					accept(TokenKind.RIGHTSQUAREBRACKET);
					expr = new NewArrayExpr(classType, arraySizeExpr, null);
					break;
				}
				else{
					parseError("Invalid Term - expecting LEFTSQUAREBRACKET or LEFTPAREN but found " + token.kind);
					//Never reached
					expr = null;
				}
					
			}
			//new int '[' Expression ']'
			else if(token.kind == TokenKind.INT){
				acceptIt();
				accept(TokenKind.LEFTSQUAREBRACKET);
				Expression arraySizeExpr = parseExprA();
				accept(TokenKind.RIGHTSQUAREBRACKET);
				expr = new NewArrayExpr(new BaseType(TypeKind.INT, null), arraySizeExpr, null);
			}
			else{
				parseError("Invalid Term - expecting ID or INT but found " + token.kind);
				//Never reached
				expr = null;
			}
			break;
		default:
			parseError("Invalid Term - expecting ID, THIS, LEFTPAREN, NUM, TRUE, FALSE, or NEW but found "
					+ token.kind);
			//Never reached
			expr = null;
		}
		//	(binop Expression)*
//		Expression secondExpr = null;
//		while (isBinop(token)) {
//			acceptIt();
//			parseExpression();
//		}
		return expr;
	}
	
	private LiteralExpr parseLiteralExpr(){
		LiteralExpr expr = null;
		switch(token.kind){
		case NUM:
			expr = new LiteralExpr(new IntLiteral(token), null);
			acceptIt();
			break;
		case TRUE:
		case FALSE:
			expr = new LiteralExpr(new BooleanLiteral(token),null);
			acceptIt();
			break;
		default:
			parseError("Invalid Term - expecting NUM, TRUE, or FALSE but found "
					+ token.kind);
		}
		return expr;
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
