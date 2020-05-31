# in-rules-engine
Kotlin rules engine DSL for maintaining rules and business logic in code.

## Basic Syntax

The rule DSL syntax uses Kotlin to implement readable rule expressions using given-when-then specifications. Unfortunately, "when" is a keyword in kotlin that would require escaping to use, so in the most basic examples, "and" is used instead.

For example, to write the expression:

    Given a string
    when it is empty
    then print "Empty string"

The rule in Kotlin would look like:

```kotlin
val emptyRule = rule("Empty string") {
  given {
    anyString()
  } and {
    it.isEmpty()
  } thenDo {
    println("Empty string")
  }
}

emptyRule("")            // Prints "Empty string"
emptyRule("not empty")   // Prints nothing
```

The `rule` helper creates a rule expression, starting with a description and a body called a rule definition.
A rule expression is a Kotlin function that takes `Any` as a parameter, and attempts to evaluate a single rule.

Inside the rule body (referred to as a "let clause" in the framework), the `given` helper defines the type of parameter expected by the rule, the `and` helper (standing in for `when`) defines the condition, and the `thenDo` defines the outcome of the rule.

**Note:** The `and` helper is an alias for `expect`, but often reads better as a stand-in for the "when" in given-when-then in Kotlin code.

You can also add an else condition to the rule that does something when the rule does not match:

```kotlin
val emptyRule = rule("Empty string") {
  given {
    anyString()
  } and {
    it.isEmpty()
  } thenDo {
    println("Empty string")
  } otherwiseDo {
    println("Not an empty string")
  }
}

emptyRule("")            // Prints "Empty string"
emptyRule("not empty")   // Prints "Not an empty string"
```

## Rule Matching

In the previous examples, the rule was being used to do something when it was applied. In the case of `thenDo` as the then expression, the rule expression does not return anything explicitly. In this case, the framework returns `true` when the rule matches or `false` when it fails to match the parameter:

```kotlin
println(emptyRule(""))            // Prints "true"
println(emptyRule("not empty"))   // Prints "false"
```

This allows you to extract the rule expression from some other code (including the outcome of the rule), and simply determine whether it matches or not.

```kotlin
if (emptyRule(str)) {
  // Do something in your application when the rule matches
}
```

## Return Values

In order to always simply return `true` or `false` when evaluating a condition, the given-when-then can be exchanged for a given-return specification:

```kotlin
val emptyRule = rule("Empty string") {
  given {
    anyString()
  } alwaysReturn {
    it.isEmpty()
  }
}

println(emptyRule(""))            // Prints "true"
println(emptyRule("not empty"))   // Prints "false"
```

In this case, it may not be obvious but the framework is always evaluating the condition (when part of given-when-then) as `true`, and evaluating and returning the result of the outcome (then part). Since the framework doesn't particularly care whether the rule matches or not, the end result is what you expect, and the rule expression simply returns `true` or `false`.

In order to return something other than `true` or `false`, you can use a given-when-return specification that is similar to the above example but requires an alternate or default return value:

```kotlin
val greetingRule = rule("Hello or Good bye") {
  given {
    anyString()
  } and {
    it == "Hi"
  } thenReturn {
    "Hello"
  } otherwiseReturn {
    "Good bye"
  }
}

println(greetingRule("Hi"))               // Prints "Hello"
println(greetingRule("Do I know you?"))   // Prints "Good bye"
```

**Note:** The `thenReturn` and `otherwiseReturn` are aliases for `then` and `otherwise` and can be used interchangeably.

Since this is just Kotlin code, you can also compact parts of your rule that you prefer to write in code. Here's an example with the same result as above using an if statement as the return value:

```kotlin
val greetingRule = rule("Hello or Good bye") {
  given {
    anyString()
  } alwaysReturn {
    if (it == "Hi") "Hello" else "Good bye"
  }
}

println(greetingRule("Hi"))               // Prints "Hello"
println(greetingRule("Do I know you?"))   // Prints "Good bye"
```

## Type Safety

The rule expressions start with a `given` that defines the input type. There are numerous ways of defining more interesting and useful input types, and the DSL allows type-safety within the rule expression. For example, to use a `Map` input type with strings:

```kotlin
val john = mapOf(
  "id" to "123",
  "firstName" to "John",
  "lastName" to "Smith"
)
val jane = mapOf(
  "id" to "456",
  "firstName" to "Jane",
  "lastName" to "Doe"
)

val firstNameRule = rule("Customer first name is John") {
  given {
    usingMap().anyString("firstName")
  } and {
    it == "John"
  } thenReturn {
    "Hello, John"
  } otherwiseReturn {
    "Good bye, $it"
  }
}

println(firstNameRule(john))   // Prints "Hello, John"
println(firstNameRule(jane))   // Prints "Good bye, Jane"
```

Types can also be coerced into any primitive, such as an `Int`:

```kotlin
val idRule = rule("Customer is John") {
  given {
    usingMap().anyInt("id")
  } and {
    it == 123
  } thenReturn {
    "Hello, John"
  } otherwiseReturn {
    "Good bye, user $it"
  }
}

println(idRule(john))   // Prints "Hello, John"
println(idRule(jane))   // Prints "Good bye, user 456"
```

**Note:** The same accessor methods such as `anyString`, `anyShort`, `anyInt`, `anyLong`, `anyFloat`, `anyDouble`, and `anyBoolean` work with `usingMap` and `usingJson` as well as by themselves with no parameters. 

## Complex Types

Possibly the most useful `given` expression is one that gives you access to an existing class you are using in your application. For example, to use a class called `Request`:

```kotlin
data class Request(val path: String)

val helloRequest = Request("/api/v1/hello")
val userRequest = Request("/api/v1/user")

val helloRule = rule("Path is hello") {
  given {
    any(Request::class)
  } whose {
    path == "/api/v1/hello"
  } thenReturn {
    "Hello"
  } otherwiseReturn {
    "Good bye"
  }
}

println(helloRule(helloRequest))   // Prints "Hello"
println(helloRule(userRequest))    // Prints "Good bye"
```

The `any` helper takes a class argument to give the rule expression access to that input parameter. Additionally, the `whose` helper (standing in for `when` in given-when-then) is extremely useful and gives you access to the fields of `Request` directly for more natural expression syntax.

Using Kotlin's extension functions, we can alias `any(Request::class)` as well:

```kotlin
fun GivenExpression.anyRequest() = any(Request::class)

val helloRule = rule("Path is hello") {
  given {
    anyRequest()
  } whose {
    path == "/api/v1/hello"
  } thenReturn {
    "Hello"
  } otherwiseReturn {
    "Good bye"
  }
}

println(helloRule(helloRequest))   // Prints "Hello"
println(helloRule(userRequest))    // Prints "Good bye"
```

## Rule Sets

You can use the framework to build a list of rules, called a rule set. There's nothing particularly fancy about this, as the framework does not implement complex functionality on top of rule sets. You can use them however you choose.

To implement a set of rules, use the `rules` helper which is an alias for `arrayOf`. Here's a more complex example:

```kotlin
fun GivenExpression.anyRequest() = any(Request::class)

fun requestFor(path: String, queryString: String? = null): Request {
  return Request(
    "Cookies: sid=abc123\nSM_USER=johndoe",
    "https://www.ecommerce.biz",
    path,
    queryString
  )
}

val ruleSet = rules(
  rule("request is login") {
    given {
      anyRequest()
    } and { request ->
      request.requestPath.contains("/login")
    } thenDo { request ->
      println("Hello, ${request.requestPath}")
    }
  },
  rule("request is send order") {
    given {
      anyRequest()
    } alwaysReturn { request ->
      request.requestPath.endsWith("/orders")
    }
  },
  rule("request is logout") {
    given {
      anyRequest()
    } alwaysReturn { request ->
      request.requestPath.contains("/logout")
    }
  },
  rule("request is search") {
    given {
      anyRequest()
    } whose {
      requestPath.contains("/search")
    } thenDo {
      println("printing a message in thenDo")
    }
  }
)

val requests = listOf(
  requestFor("/api/v1/login"),
  requestFor("/api/v1/user"),
  requestFor("/api/v1/messages"),
  requestFor("/api/v1/dashboard"),
  requestFor("/api/v1/search", "category=Kitchen&q=Kitchen%20Aid%20Mixer"),
  requestFor("/api/v1/orders"),
  requestFor("/api/v1/orders/123"),
  requestFor("/api/v1/logout")
)


for (i in requests.indices) {
  val request = requests[i]
  println("Processing event ${i + 1} with request path ${request.requestPath}")

  for (rule in ruleSet) {
    if (rule(request)) {
      println("+Rule \"${rule.description}\" matched")
    }
  }
}
```