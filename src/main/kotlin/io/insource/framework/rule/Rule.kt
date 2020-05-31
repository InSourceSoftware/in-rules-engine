@file:Suppress("unused")

package io.insource.framework.rule

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlin.reflect.KClass

/**
 * Represents a rule function to be evaluated for a result.
 *
 * @param description The description of the rule function
 * @param ruleDefinition The definition of the rule
 * @param T The return type of the rule function
 */
class Rule<E, T>(val description: String, val ruleDefinition: RuleDefinition<E, T>) {
  /**
   * Execute the rule as a function.
   *
   * @param arg The rule argument, which can be any type the rule expects to handle
   */
  operator fun invoke(arg: Any): T = ruleDefinition(arg).let {
    if (ruleDefinition.whenClause(it)) {
      ruleDefinition.thenClause(it)
    } else {
      ruleDefinition.elseClause(it)
    }
  }
}

/**
 * Represents a rule definition (e.g. an if-then expression).
 *
 * @param whenClause The if-part of a rule expression
 * @param thenClause The then-part of a rule expression
 */
class RuleDefinition<E, T>(
  val givenClause: GivenExpression.() -> E,
  val whenClause: (E) -> Boolean,
  val thenClause: (E) -> T,
  val elseClause: (E) -> T
) {
  /**
   * Execute the given clause of a rule definition to retrieve a strongly-typed
   * rule argument.
   *
   * @param arg The rule argument, which can be any type the rule expects to handle
   */
  internal operator fun invoke(arg: Any): E = GivenExpression(arg).givenClause()

  /**
   * Override the else-part of a rule expression with an action.
   *
   * @param elseClause The else-part of a rule expression
   * @return A rule definition
   */
  infix fun otherwiseDo(elseClause: (E) -> Unit): RuleDefinition<E, T> = RuleDefinition(givenClause, whenClause, thenClause, { elseClause(it); this.elseClause(it) })
}

/**
 * A transparent type representing a rule expression.
 */
class RuleExpression {
  /**
   * Provide the pre-condition of a rule expression.
   *
   * @param givenClause The pre-condition of a rule expression
   * @return A when expression
   */
  infix fun <E> given(givenClause: GivenExpression.() -> E): WhenExpression<E> = WhenExpression(givenClause)
}

/**
 * A transparent type used to extend the capabilities of a rule expression.
 *
 * @param arg The rule argument, which can be any type the rule expects to handle
 */
class GivenExpression(private val arg: Any) {
  companion object {
    /**
     * Accessor function to retrieve values from a Map. Null values throw a
     * `NoSuchElementException`.
     */
    val MapAccessStrategy = fun(key: String, arg: Any): Any = (arg as Map<*, *>).let {
      if (it.containsKey(key)) it[key]!! else throw NoSuchElementException("Key $key not found")
    }

    /**
     * Accessor function to retrieve values from a JSON object. Null values throw a
     * `NoSuchElementException`.
     */
    @Suppress("IMPLICIT_CAST_TO_ANY")
    val JsonAccessStrategy = fun(key: String, arg: Any): Any = (arg as ObjectNode).let { obj ->
      if (obj.has(key)) obj.get(key).let {
        when {
          it.isShort -> it.asInt().toShort()
          it.isInt -> it.asInt()
          it.isLong -> it.asLong()
          it.isFloat -> it.asDouble().toFloat()
          it.isDouble -> it.asDouble()
          it.isBoolean -> it.asBoolean()
          it.isTextual -> it.asText()
          else -> it
        }
      }
      else throw NoSuchElementException("Key $key not found")
    }

    /**
     * Default accessor function for rule expressions.
     */
    var DefaultAccessStrategy = JsonAccessStrategy
  }

  /**
   * Apply the desired access strategy to this rule expression in order to
   * access a nested field.
   *
   * @param accessStrategy An accessor function to retrieve values from nested
   * objects.
   */
  fun using(accessStrategy: (String, Any) -> Any): NestedGivenExpression = NestedGivenExpression(arg, accessStrategy)

  /**
   * Apply the `MapAccessStrategy` to this rule expression in order to access
   * nested fields in a `Map`.
   */
  fun usingMap() = using(MapAccessStrategy)

  /**
   * Apply the `JsonAccessStrategy` to this rule expression in order to access
   * nested fields in a JSON object.
   */
  fun usingJson() = using(JsonAccessStrategy)

  /**
   * Coerce the rule argument into a particular type.
   *
   * @param T The runtime type
   * @return The argument, as the desired runtime type
   */
  fun <T : Any> any(clazz: KClass<T>): T = clazz.java.cast(arg)

  /**
   * Coerce the rule argument into a `Map`.
   *
   * @return The argument, as a `Map`
   */
  fun anyMap(): Map<*, *> = arg as Map<*, *>

  /**
   * Coerce the rule argument into a `MutableMap`.
   *
   * @return The argument, as a `MutableMap`
   */
  fun anyMutableMap(): MutableMap<*, *> = arg as MutableMap<*, *>

  /**
   * Coerce the rule argument into an `ObjectNode`.
   *
   * @return The argument, as an `ObjectNode`
   */
  fun anyObject(): ObjectNode = arg as ObjectNode

  /**
   * Coerce the rule argument into an `ArrayNode`.
   *
   * @return The argument, as an `ArrayNode`
   */
  fun anyArray(): ArrayNode = arg as ArrayNode

  /**
   * Coerce the argument into a `String`.
   *
   * @return The argument, as a `String`
   */
  fun anyString(): String = arg.toString()

  /**
   * Coerce the argument into a `Short`.
   *
   * @return The argument, as a `Short`
   */
  fun anyShort(): Short = when (arg) {
    is Short -> arg
    is Number -> arg.toShort()
    else -> arg.toString().toShort()
  }

  /**
   * Coerce the argument into an `Int`.
   *
   * @return The argument, as an `Int`
   */
  fun anyInt(): Int = when (arg) {
    is Int -> arg
    is Number -> arg.toInt()
    else -> arg.toString().toInt()
  }

  /**
   * Coerce the argument into a `Long`.
   *
   * @return The argument, as a `Long`
   */
  fun anyLong(): Long = when (arg) {
    is Long -> arg
    is Number -> arg.toLong()
    else -> arg.toString().toLong()
  }

  /**
   * Coerce the argument into a `Float`.
   *
   * @return The argument, as a `Float`
   */
  fun anyFloat(): Float = when (arg) {
    is Float -> arg
    is Number -> arg.toFloat()
    else -> arg.toString().toFloat()
  }

  /**
   * Coerce the argument into a `Double`.
   *
   * @return The argument, as a `Double`
   */
  fun anyDouble(): Double = when (arg) {
    is Double -> arg
    is Number -> arg.toDouble()
    else -> arg.toString().toDouble()
  }

  /**
   * Coerce the argument into a `Boolean`.
   *
   * @return The argument, as a `Boolean`
   */
  fun anyBoolean(): Boolean = when (arg) {
    is Boolean -> arg
    else -> arg.toString().toLowerCase().let { str ->
      when (str) {
        "t", "true", "y", "yes", "1", "+" -> true
        else -> false
      }
    }
  }

  /**
   * Coerce a nested field into a `String`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as a `String`
   */
  fun anyString(key: String): String = using(DefaultAccessStrategy).anyString(key)

  /**
   * Coerce a nested field into a `Short`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as a `Short`
   */
  fun anyShort(key: String): Short = using(DefaultAccessStrategy).anyShort(key)

  /**
   * Coerce a nested field into an `Int`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as an `Int`
   */
  fun anyInt(key: String): Int = using(DefaultAccessStrategy).anyInt(key)

  /**
   * Coerce a nested field into a `Long`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as a `Long`
   */
  fun anyLong(key: String): Long = using(DefaultAccessStrategy).anyLong(key)

  /**
   * Coerce a nested field into a `Float`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as a `Float`
   */
  fun anyFloat(key: String): Float = using(DefaultAccessStrategy).anyFloat(key)

  /**
   * Coerce a nested field into a `Double`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as a `Double`
   */
  fun anyDouble(key: String): Double = using(DefaultAccessStrategy).anyDouble(key)

  /**
   * Coerce a nested field into a `Boolean`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as a `Boolean`
   */
  fun anyBoolean(key: String): Boolean = using(DefaultAccessStrategy).anyBoolean(key)
}

/**
 * A specialized rule expression for nested values.
 *
 * @param arg The rule argument, which can be any type the rule expects to handle
 * @param accessStrategy Accessor function to retrieve values from nested objects, e.g. an `ObjectNode` or `Map`
 */
class NestedGivenExpression(private val arg: Any, private var accessStrategy: (String, Any) -> Any) {
  /**
   * Coerce a nested field into a `Map`.
   *
   * @return The argument, as a `Map`
   */
  fun anyMap(key: String): Map<*, *> = accessStrategy(key, arg) as Map<*, *>

  /**
   * Coerce a nested field into an `ObjectNode`.
   *
   * @return The argument, as an `ObjectNode`
   */
  fun anyObject(key: String): ObjectNode = accessStrategy(key, arg) as ObjectNode

  /**
   * Coerce a nested field into an `ArrayNode`.
   *
   * @return The argument, as an `ArrayNode`
   */
  fun anyArray(key: String): ArrayNode = accessStrategy(key, arg) as ArrayNode

  /**
   * Coerce a nested field into a `String`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as a `String`
   */
  fun anyString(key: String): String = accessStrategy(key, arg).toString()

  /**
   * Coerce a nested field into a `Short`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as a `Short`
   */
  fun anyShort(key: String): Short = accessStrategy(key, arg).let {
    when (it) {
      is Short -> it
      is Number -> it.toShort()
      else -> it.toString().toShort()
    }
  }

  /**
   * Coerce a nested field into an `Int`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as an `Int`
   */
  fun anyInt(key: String): Int = accessStrategy(key, arg).let {
    when (it) {
      is Int -> it
      is Number -> it.toInt()
      else -> it.toString().toInt()
    }
  }

  /**
   * Coerce a nested field into a `Long`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as a `Long`
   */
  fun anyLong(key: String): Long = accessStrategy(key, arg).let {
    when (it) {
      is Long -> it
      is Number -> it.toLong()
      else -> it.toString().toLong()
    }
  }

  /**
   * Coerce a nested field into a `Float`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as a `Float`
   */
  fun anyFloat(key: String): Float = accessStrategy(key, arg).let {
    when (it) {
      is Float -> it
      is Number -> it.toFloat()
      else -> it.toString().toFloat()
    }
  }

  /**
   * Coerce a nested field into a `Double`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as a `Double`
   */
  fun anyDouble(key: String): Double = accessStrategy(key, arg).let {
    when (it) {
      is Double -> it
      is Number -> it.toDouble()
      else -> it.toString().toDouble()
    }
  }

  /**
   * Coerce a nested field into a `Boolean`.
   *
   * @param key The key of a nested property to retrieve
   * @return The argument, as a `Boolean`
   */
  fun anyBoolean(key: String): Boolean = accessStrategy(key, arg).let {
    when (it) {
      is Boolean -> it
      else -> it.toString().toLowerCase().let { str ->
        when (str) {
          "t", "true", "y", "yes", "1", "+" -> true
          else -> false
        }
      }
    }
  }
}

/**
 * A transparent type used to extend the capabilities of a rule expression.
 *
 * @param givenClause The pre-condition of a rule expression
 */
class WhenExpression<E>(val givenClause: GivenExpression.() -> E) {
  /**
   * Provide the if-part of a rule expression.
   *
   * @param whenClause The if-part of a rule expression
   * @return A then expression
   */
  infix fun expect(whenClause: (E) -> Boolean): ThenExpression<E> = ThenExpression(givenClause, whenClause)

  /**
   * Alias for `expect`.
   *
   * @param whenClause The if-part of a rule expression
   * @return A then expression
   */
  infix fun and(whenClause: (E) -> Boolean): ThenExpression<E> = expect(whenClause)

  /**
   * Provide the if-part of a rule expression, as the argument supplied by the given.
   *
   * @param whenClause The if-part of a rule expression
   * @return A then expression
   */
  infix fun whose(whenClause: E.() -> Boolean): ThenExpression<E> = ThenExpression(givenClause, { it.whenClause() })

  /**
   * Always return a value. This has the side effect of the rule always matching.
   *
   * @param thenClause Then then-part of a rule expression
   * @return A rule definition
   */
  infix fun <T> alwaysReturn(thenClause: (E) -> T): RuleDefinition<E, T> = expect { true } thenReturn(thenClause) otherwiseReturn(thenClause)

  /**
   * Always perform an action. This has the side effect of the rule always matching.
   *
   * @param thenClause Then then-part of a rule expression
   * @return A rule definition
   */
  infix fun alwaysDo(thenClause: (E) -> Unit): RuleDefinition<E, Boolean> = expect { true } thenDo(thenClause)
}

/**
 * A transparent type used to extend the capabilities of a rule expression.
 *
 * @param givenClause The pre-condition of a rule expression
 * @param whenClause The if-part of a rule expression
 */
class ThenExpression<E>(val givenClause: GivenExpression.() -> E, val whenClause: (E) -> Boolean) {
  /**
   * Provide an additional if-part of a rule expression, AND'd to the first part.
   *
   * @param whenClause The if-part of a rule expression
   * @return A then expression
   */
  infix fun and(whenClause: (E) -> Boolean): ThenExpression<E> = ThenExpression(givenClause, { this.whenClause(it) && whenClause(it) })

  /**
   * Provide an additional if-part of a rule expression, AND'd to the first part.
   *
   * @param whenClause The if-part of a rule expression
   * @return A then expression
   */
  infix fun or(whenClause: (E) -> Boolean): ThenExpression<E> = ThenExpression(givenClause, { this.whenClause(it) || whenClause(it) })

  /**
   * Provide the then-part of a rule expression with a return value.
   *
   * @param thenClause Then then-part of a rule expression
   * @return An otherwise expression
   */
  infix fun <T> thenReturn(thenClause: (E) -> T): OtherwiseExpression<E, T> = OtherwiseExpression(givenClause, whenClause, thenClause)

  /**
   * Provide the then-part of a rule expression with an action.
   *
   * @param thenClause Then then-part of a rule expression
   * @return A rule definition
   */
  infix fun thenDo(thenClause: (E) -> Unit): RuleDefinition<E, Boolean> = RuleDefinition(givenClause, whenClause, { thenClause(it); true }, { false })

  /**
   * Alias for `thenReturn`.
   *
   * @param thenClause Then then-part of a rule expression
   * @return An otherwise expression
   */
  infix fun <T> then(thenClause: (E) -> T): OtherwiseExpression<E, T> = thenReturn(thenClause)
}

/**
 * A transparent type used to extend the capabilities of a rule expression.
 *
 * @param givenClause The pre-condition of a rule expression
 * @param whenClause The if-part of a rule expression
 * @param thenClause The then-part of a rule expression
 */
class OtherwiseExpression<E, T>(val givenClause: GivenExpression.() -> E, val whenClause: (E) -> Boolean, val thenClause: (E) -> T) {
  /**
   * Override the else-part of a rule expression with a return value.
   *
   * @param elseClause The else-part of a rule expression
   * @return A rule definition
   */
  infix fun otherwiseReturn(elseClause: (E) -> T): RuleDefinition<E, T> = RuleDefinition(givenClause, whenClause, thenClause, elseClause)

  /**
   * Alias for `otherwiseReturn`.
   *
   * @param elseClause The else-part of a rule expression
   * @return A rule definition
   */
  infix fun otherwise(elseClause: (E) -> T): RuleDefinition<E, T> = otherwiseReturn(elseClause)
}

/**
 * Generate a rule function.
 *
 * @param description The description of the rule function
 * @param letClause The definition of the rule, as a function that returns a rule definition
 * @param T The return type of the rule function
 * @return A rule function
 */
fun <E, T> rule(description: String, letClause: RuleExpression.() -> RuleDefinition<E, T>): Rule<E, T> {
  return Rule(description, RuleExpression().letClause())
}

/**
 * Alias for <code>arrayOf(...)</code>.
 *
 * @param rules
 */
fun <E, T> rules(vararg rules: Rule<E, T>): Array<out Rule<E, T>> = rules

/**
 * Set the access strategy to retrieve values from a JSON object.
 */
fun useJsonAccessStrategy() {
  GivenExpression.DefaultAccessStrategy = GivenExpression.JsonAccessStrategy
}

/**
 * Set the access strategy to retrieve values from a Map.
 */
fun useMapAccessStrategy() {
  GivenExpression.DefaultAccessStrategy = GivenExpression.MapAccessStrategy
}
