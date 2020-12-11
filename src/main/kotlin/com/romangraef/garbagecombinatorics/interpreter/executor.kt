package com.romangraef.garbagecombinatorics.interpreter

import com.romangraef.garbagecombinatorics.ast.Assignment
import com.romangraef.garbagecombinatorics.ast.BinOp
import com.romangraef.garbagecombinatorics.ast.BinaryExpr
import com.romangraef.garbagecombinatorics.ast.ConstInt
import com.romangraef.garbagecombinatorics.ast.ConstString
import com.romangraef.garbagecombinatorics.ast.Expr
import com.romangraef.garbagecombinatorics.ast.ExprStatement
import com.romangraef.garbagecombinatorics.ast.FunctionDeclaration
import com.romangraef.garbagecombinatorics.ast.FunctionExpr
import com.romangraef.garbagecombinatorics.ast.Program
import com.romangraef.garbagecombinatorics.ast.Statement
import com.romangraef.garbagecombinatorics.ast.UnaryMinus
import com.romangraef.garbagecombinatorics.ast.VariableExpr

class Executor(val context: Context = Context()) {
	fun execute(program: Program) {
		execute(program.statements)
	}

	private fun execute(statement: List<Statement>) = statement.forEach { execute(it) }
	private fun execute(statement: Statement) {
		when (statement) {
			is Assignment -> context[statement.name] = evaluate(statement.value)
			is ExprStatement -> evaluate(statement.expr)
			is FunctionDeclaration -> context.saveFunction(statement)
		}
	}

	private fun evaluate(expr: Expr): Value? = expr.run {
		when (expr) {
			is ConstString -> StrValue(expr.value)
			is ConstInt -> IntValue(expr.value)
			is UnaryMinus -> unaryMinus(evaluate(expr.expr))
			is FunctionExpr -> callFunction(expr.name, expr.args.map { evaluate(it) })
			is VariableExpr -> context[expr.name]
			is BinaryExpr -> binaryOp(expr)
		}
	}

	private fun binaryOp(expr: BinaryExpr): IntValue {
		val left = onlyInts(evaluate(expr.left)).value
		val right = onlyInts(evaluate(expr.right)).value
		return IntValue(
			when (expr.op) {
				BinOp.PLUS -> left + right
				BinOp.MINUS -> left - right
				BinOp.TIMES -> left * right
				BinOp.DIVIDE -> left / right
			}
		)
	}

	private fun unaryMinus(value: Value?): Value = IntValue(-onlyInts(value).value)

	private fun onlyInts(value: Value?): IntValue {
		if (value !is IntValue)
			throw RuntimeException("Only ints allowed for value $value")
		return value
	}

	private fun noNull(): Nothing = throw RuntimeException("Can't use a null value.")
	private fun callFunction(name: String, args: List<Value?>): Value? {
		if (args.filter { it == null }.any()) noNull()
		val func = context.getFunction(name) ?: throw RuntimeException("Unknown function $name")
		if (func.argNames.size != args.size) throw RuntimeException("Invalid arg length for function ${func.name}")
		return when (func) {
			is DeclaredFunction -> executeDeclaredFunction(func.functionDeclaration, args)
			is IntrinsicFunction -> func.invoke(args)
		}
	}

	private fun executeDeclaredFunction(func: FunctionDeclaration, args: List<Value?>): Value? {
		val executor = Executor(Context(this.context))
		func.argNames.zip(args).forEach { (name, value) -> executor.context[name] = value }
		executor.execute(func.statements)
		if (func.returnExpr != null) {
			return executor.evaluate(func.returnExpr)
		}
		return null
	}

	fun provideIntrinsic(name: String, argNames: List<String>, function: (args: List<Value?>) -> Value?) {
		context.setFunction(name, object : IntrinsicFunction(name, argNames) {
			override fun invoke(args: List<Value?>): Value? = function(args)
		})
	}
}