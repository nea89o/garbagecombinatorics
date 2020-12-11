package com.romangraef.garbagecombinatorics.interpreter

import com.romangraef.garbagecombinatorics.ast.FunctionDeclaration
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

sealed class Value
data class IntValue(val value: Int) : Value()
data class StrValue(val value: String) : Value()

sealed class Function {
	abstract val name: String
	abstract val argNames: List<String>
}

abstract class IntrinsicFunction(override val name: String, override val argNames: List<String>) : Function() {
	abstract operator fun invoke(args: List<Value?>): Value?
}

data class DeclaredFunction(val functionDeclaration: FunctionDeclaration) : Function() {
	override val argNames by functionDeclaration::argNames
	override val name by functionDeclaration::name
}

class Context(val parentContext: Context? = null) {
	val data = mutableMapOf<String, Value>()
	val functions = mutableMapOf<String, Function>()
	operator fun get(name: String): Value? = data[name] ?: parentContext?.get(name)
	operator fun set(name: String, value: Value?) {
		if (value == null) data.remove(name)
		else data[name] = value
	}

	fun getFunction(name: String): Function? = functions[name] ?: parentContext?.getFunction(name)
	fun setFunction(name: String, value: Function) {
		functions[name] = value
	}

	fun saveFunction(value: FunctionDeclaration) = setFunction(value.name, DeclaredFunction(value))
	fun delegate(name: String) = ContextDelegate(this, name)
}

class ContextDelegate(val context: Context, val name: String) : ReadWriteProperty<Any?, Value?> {
	override fun setValue(thisRef: Any?, property: KProperty<*>, value: Value?) {
		context[name] = value
	}

	override fun getValue(thisRef: Any?, property: KProperty<*>): Value? = context[name]
}