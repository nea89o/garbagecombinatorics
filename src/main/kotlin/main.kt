import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.romangraef.garbagecombinatorics.grammar.GarbageGrammar
import com.romangraef.garbagecombinatorics.interpreter.Executor
import com.romangraef.garbagecombinatorics.interpreter.IntValue
import com.romangraef.garbagecombinatorics.interpreter.StrValue

val x = """
a = 10
b(x) {
	z = 2 * x
	z + 10
}
print(b(a))
"""
val testA = "a=10+2*6-(-2*4)\nb=10+a\nx=print(-f())"


fun main(args: Array<String>) {
	val ast = GarbageGrammar.parseToEnd(x)
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