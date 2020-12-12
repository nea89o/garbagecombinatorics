package com.romangraef.garbagecombinatorics.typechecker

import com.romangraef.garbagecombinatorics.ast.Assignment
import com.romangraef.garbagecombinatorics.ast.BinaryExpr
import com.romangraef.garbagecombinatorics.ast.ConstInt
import com.romangraef.garbagecombinatorics.ast.ConstString
import com.romangraef.garbagecombinatorics.ast.Expr
import com.romangraef.garbagecombinatorics.ast.ExprStatement
import com.romangraef.garbagecombinatorics.ast.FunctionDeclaration
import com.romangraef.garbagecombinatorics.ast.FunctionExpr
import com.romangraef.garbagecombinatorics.ast.FunctionTypeExpression
import com.romangraef.garbagecombinatorics.ast.NormalTypeExpression
import com.romangraef.garbagecombinatorics.ast.Program
import com.romangraef.garbagecombinatorics.ast.Statement
import com.romangraef.garbagecombinatorics.ast.TypeExpression
import com.romangraef.garbagecombinatorics.ast.TypeStatement
import com.romangraef.garbagecombinatorics.ast.UnaryMinus
import com.romangraef.garbagecombinatorics.ast.VariableExpr

class TypeContext(val parent: TypeContext? = null) {
	val types = mutableMapOf<String, TypeExpression>()
	operator fun get(name: String): TypeExpression? = types[name] ?: parent?.get(name)
	operator fun set(name: String, value: TypeExpression) {
		types[name] = value
	}
}

val STRTYPE = NormalTypeExpression("str")
val INTTYPE = NormalTypeExpression("int")
val VOIDTYPE = NormalTypeExpression("void")

class TypeChecker(val context: TypeContext = TypeContext()) {
	fun typeCheck(program: Program) {
		program.statements.forEach {
			typeCheck(it)
		}
	}

	private fun typeCheck(statement: Statement) {
		when (statement) {
			is Assignment -> assertPolyType(getWriteType(statement.name), typeCheckExpr(statement.value))
			is TypeStatement -> context[statement.name] = statement.type
			is ExprStatement -> typeCheckExpr(statement.expr)
			is FunctionDeclaration -> typeCheckFunction(statement)
		}
	}

	private fun typeCheckFunction(statement: FunctionDeclaration) {
		val type = getWriteType(statement.name)
		if (type !is FunctionTypeExpression) {
			throw RuntimeException("${statement.name} is not defined as a function")
		}
		val checker = TypeChecker(TypeContext(context))
		statement.argNames.zip(type.args).forEach { (argName, argType) ->
			checker.context[argName] = argType
		}
		statement.statements.forEach {
			checker.typeCheck(it)
		}
		if (statement.returnExpr != null)
			assertPolyType(checker.typeCheckExpr(statement.returnExpr), type.returnType)
		else assertPolyType(VOIDTYPE, type.returnType)
	}

	private fun typeCheckExpr(expr: Expr): TypeExpression = when (expr) {
		is ConstString -> STRTYPE
		is ConstInt -> INTTYPE
		is UnaryMinus -> {
			assertPolyType(typeCheckExpr(expr.expr), INTTYPE)
			INTTYPE
		}
		is FunctionExpr -> {
			val func = getAccessType(expr.name)
			if (func !is FunctionTypeExpression) throw RuntimeException("${expr.name} is not a callable function")
			func.args.zip(expr.args).forEach { (required, provided) ->
				assertPolyType(typeCheckExpr(provided), required)
			}
			func.returnType
		}
		is VariableExpr -> getAccessType(expr.name)
		is BinaryExpr -> {
			assertPolyType(typeCheckExpr(expr.left), INTTYPE)
			assertPolyType(typeCheckExpr(expr.right), INTTYPE)
			INTTYPE
		}
	}

	private fun getWriteType(name: String) = context.types[name] ?: throw RuntimeException("No type defined for $name")
	private fun getAccessType(name: String) = context[name] ?: throw RuntimeException("No type defined for $name")
	private fun assertPolyType(subType: TypeExpression, superType: TypeExpression) {
		if (!isPolyType(subType, superType))
			throw RuntimeException("$subType isnt subtype of $superType")
	}

	private fun isPolyType(subType: TypeExpression, superType: TypeExpression): Boolean {
		if (subType is FunctionTypeExpression && superType is FunctionTypeExpression) {
			if (!isPolyType(subType.returnType, superType.returnType)) return false
			if (subType.args.size != superType.args.size) return false
			subType.args.zip(superType.args).forEach { (subArg, superArg) ->
				if (!isPolyType(superArg, subArg))
					return false

			}
			return true
		} else if (subType is NormalTypeExpression && superType is NormalTypeExpression) {
			return subType.name == superType.name
		}
		return false
	}

}
