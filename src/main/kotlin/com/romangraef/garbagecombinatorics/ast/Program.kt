package com.romangraef.garbagecombinatorics.ast

data class Program(
	val statements: List<Statement>
)

enum class BinOp {
	PLUS,
	MINUS,
	TIMES,
	DIVIDE
}

sealed class Statement
sealed class NonFuncStatement : Statement()
sealed class Expr
sealed class Const : Expr()
data class ConstString(val value: String) : Const()
data class ConstInt(val value: Int) : Const()
data class UnaryMinus(val expr: Expr) : Expr()
data class FunctionExpr(val name: String, val args: List<Expr>) : Expr()
data class VariableExpr(val name: String) : Expr()
data class BinaryExpr(val left: Expr, val op: BinOp, val right: Expr) : Expr()
data class Assignment(val name: String, val value: Expr) : NonFuncStatement()
sealed class TypeExpression
data class FunctionTypeExpression(val args: List<TypeExpression>, val returnType: TypeExpression) : TypeExpression()
data class NormalTypeExpression(val name: String) : TypeExpression()
data class TypeStatement(val name: String, val type: TypeExpression) : NonFuncStatement()
data class ExprStatement(val expr: Expr) : NonFuncStatement()
data class FunctionDeclaration(
	val name: String,
	val argNames: List<String>,
	val statements: List<NonFuncStatement>,
	val returnExpr: Expr?
) :
	Statement()
