import com.github.h0tk3y.betterParse.parser.toParsedOrThrow
import com.romangraef.garbagecombinatorics.grammar.GarbageGrammar
import com.romangraef.garbagecombinatorics.interpreter.Executor
import com.romangraef.garbagecombinatorics.interpreter.IntValue
import com.romangraef.garbagecombinatorics.interpreter.StrValue
import com.romangraef.garbagecombinatorics.typechecker.TypeChecker

val x = """
a : int
print : (int)->void
a = 10
b : (int)->int
b(x) {
	z : int
	z = 2 * x
	z + 10
}
print(b(a))
"""
val testA = "a=10+2*6-(-2*4)\nb=10+a\nx=print(-f())"


fun main() {
	val tokens = GarbageGrammar.tokenizer.tokenize(x)
	val result = GarbageGrammar.tryParse(tokens, 0).toParsedOrThrow()
	val ast = result.value
	println(ast)
	val next = tokens.getNotIgnored(result.nextPosition)
	if (next != null) {
		println("Unparsed remainder: $next")
	}

	TypeChecker().typeCheck(ast)

	val executor = Executor()
	executor.provideIntrinsic("print", listOf("output")) { (arg) ->
		println(
			when (arg) {
				is IntValue -> arg.value.toString()
				is StrValue -> arg.value
				null -> "null"
			}
		)
		null
	}
	executor.execute(ast)
}