package com.romangraef.garbagecombinatorics.grammar

import com.github.h0tk3y.betterParse.combinators.leftAssociative
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.times
import com.github.h0tk3y.betterParse.combinators.unaryMinus
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
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

object GarbageGrammar : Grammar<Program>() {
	private val LPAR by literalToken("(")
	private val RPAR by literalToken(")")
	private val LBR by literalToken("{")
	private val RBR by literalToken("}")
	private val MINUS by literalToken("-")
	private val PLUS by literalToken("+")
	private val STAR by literalToken("*")
	private val SLASH by literalToken("/")
	private val EQUALS by literalToken("=")
	private val COMMA by literalToken(",")
	private val NUMBER by regexToken("\\d+")
	private val STRINGLITERAL by regexToken(
		"\"((?:.*?[^\\\\])?(?:\\\\\\\\)*)?\"".toRegex(setOf(RegexOption.COMMENTS, RegexOption.DOT_MATCHES_ALL))
	) // https://regex101.com/r/ie99vH/3

	private val BAREWORD by regexToken("[a-z_A-Z][a-zA-Z0-9_]*")

	private val WS by regexToken("\\s+", ignore = true)
	private val NEWLINE by regexToken("[\r\n]+", ignore = true)

	private val numberLiteral by NUMBER map { value -> ConstInt(value.text.toInt()) }

	private val stringLiteral by STRINGLITERAL map {
		ConstString(
			it.text.drop(1).dropLast(1)
		)
	} // TODO proper unescaping of inner string

	private val anyLiteral by stringLiteral or numberLiteral

	private val functionInvoc by BAREWORD * -LPAR * separatedTerms(
		parser(this::expr),
		COMMA,
		acceptZero = true
	) * -RPAR map { (name, args) ->
		FunctionExpr(name.text, args)
	}

	private val variableAccess by BAREWORD map { VariableExpr(it.text) }

	private val negationExpr by -MINUS * parser(::term) map { UnaryMinus(it) }

	private val parenExpr by -LPAR * parser(::expr) * -RPAR

	private val term: Parser<Expr> by functionInvoc or anyLiteral or variableAccess or parenExpr or negationExpr

	private val multiOperator by STAR or SLASH
	private val multiplyTerm by leftAssociative(term, multiOperator) { left, op, right ->
		BinaryExpr(
			left, when (op.type) {
				STAR -> BinOp.TIMES
				SLASH -> BinOp.DIVIDE
				else -> throw RuntimeException()
			}, right
		)
	}

	private val addOperator by PLUS or MINUS
	private val additionTerm by leftAssociative(multiplyTerm, addOperator) { left, op, right ->
		BinaryExpr(
			left, when (op.type) {
				PLUS -> BinOp.PLUS
				MINUS -> BinOp.MINUS
				else -> throw RuntimeException()
			}, right
		)
	}

	private val expr: Parser<Expr> by additionTerm

	private val assignment by BAREWORD * -EQUALS * expr map { (name, value) -> Assignment(name.text, value) }

	private val functionStatement by functionInvoc map { ExprStatement(it) }

	private val nonFuncStatement by assignment or functionStatement

	private val functionDeclaration: Parser<Statement> by BAREWORD * -LPAR * separatedTerms(
		BAREWORD,
		COMMA,
		acceptZero = true
	) * -RPAR * -LBR * separatedTerms(
		nonFuncStatement,
		optional(NEWLINE)
	) * optional(expr) * -RBR map { (name, args, executable, retExpr) ->
		FunctionDeclaration(name.text, args.map { it.text }, executable, retExpr)
	}

	private val statement by functionDeclaration or nonFuncStatement

	private val program by separatedTerms(statement, optional(NEWLINE)) map { Program(it) }
	override val rootParser: Parser<Program>
		get() = program
}