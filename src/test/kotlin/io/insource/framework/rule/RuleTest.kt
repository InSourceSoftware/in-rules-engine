package io.insource.framework.rule

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Test

class RuleTest {
  @Before
  fun setUp() {
    useMapAccessStrategy()
  }

  @Test
  fun testRule_Scratch() {
    // Request class
    data class Request(val path: String)

    // Given accessor
    fun GivenExpression.anyRequest() = any(Request::class)

    val r = rule("Test rule with whose") {
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

    val helloRequest = Request("/api/v1/hello")
    val userRequest = Request("/api/v1/user")

    assertThat(r(helloRequest), equalTo("Hello"))
    assertThat(r(userRequest), equalTo("Good bye"))
  }

  @Test
  fun testRule_ThenReturn() {
    val r = rule("Test rule thenReturn") {
      given {
        anyString()
      } and {
        it == "test"
      } thenReturn {
        "Hello"
      } otherwiseReturn {
        "Good bye"
      }
    }

    assertThat(r("test"), equalTo("Hello"))
    assertThat(r("nothing"), equalTo("Good bye"))
  }

  @Test
  fun testRule_ThenDo() {
    val r = rule("Test rule thenDo") {
      given {
        anyString()
      } and {
        it == "test"
      } thenDo {
        println("thenDo: Hello")
      }
    }

    assertThat(r("test"), equalTo(true))
    assertThat(r("nothing"), equalTo(false))
  }

  @Test
  fun testRule_MapKeyAsString() {
    val r = rule("Test rule with type String") {
      given {
        anyString("field1")
      } and {
        it == "x"
      } thenReturn {
        "Hello"
      } otherwiseReturn {
        "Good bye"
      }
    }

    val m = mapOf(
      "field1" to "x",
      "field2" to "y",
      "field3" to "z"
    )
    assertThat(r(m), equalTo("Hello"))
  }

  @Test
  fun testRule_MapKeyAsInt() {
    val r = rule("Test rule with type Int") {
      given {
        anyInt("field1")
      } and {
        it == 1
      } thenReturn {
        "Hello"
      } otherwiseReturn {
        "Good bye"
      }
    }

    val m = mapOf(
      "field1" to 1,
      "field2" to 2,
      "field3" to 3
    )
    assertThat(r(m), equalTo("Hello"))
  }

  @Test
  fun testRule_MapKeyAsBoolean() {
    val r = rule("Test rule with type Boolean") {
      given {
        anyBoolean("field1")
      } and {
        it
      } thenReturn {
        "Hello"
      } otherwiseReturn {
        "Good bye"
      }
    }

    val m = mapOf(
      "field1" to true,
      "field2" to false
    )

    assertThat(r(m), equalTo("Hello"))
  }

  @Test(expected = NoSuchElementException::class)
  fun testRule_MapValueIsNull() {
    val r = rule("Test rule with type String, value is null") {
      given {
        anyString("field4")
      } and {
        it == ""
      } thenReturn {
        "Hello"
      } otherwiseReturn {
        "Good bye"
      }
    }

    val m = mapOf(
      "field1" to "x",
      "field2" to "y",
      "field3" to "z"
    )

    r(m)
  }

  @Test
  fun testRule_AlwaysReturn() {
    val r = rule("Test rule with alwaysReturn") {
      given {
        anyString()
      } alwaysReturn {
        it == "test"
      }
    }

    assertThat(r("test"), equalTo(true))
    assertThat(r("nothing"), equalTo(false))
  }

  @Test
  fun testRule_AlwaysReturnBooleanInMap() {
    val r = rule("Test rule with alwaysReturn, type boolean") {
      given { anyBoolean("field3") } alwaysReturn { it }
    }

    val m = mapOf(
      "field1" to "0",
      "field2" to "false",
      "field3" to "t"
    )

    assertThat(r(m), equalTo(true))
  }

  @Test
  fun testRule_IfStatement() {
    val r = rule("Test rule with if-statement") {
      given {
        anyString()
      } alwaysReturn {
        if (it == "test") "Hello" else "Good bye"
      }
    }

    assertThat(r("test"), equalTo("Hello"))
    assertThat(r("other"), equalTo("Good bye"))
  }

  @Test
  fun testRule_CompoundWhen() {
    val r = rule("Test rule with compound when") {
      given {
        anyString()
      } and {
        it == "test"
      } or {
        it == "something"
      } thenReturn {
        "Hello"
      } otherwiseReturn {
        "Good bye"
      }
    }

    assertThat(r("test"), equalTo("Hello"))
    assertThat(r("something"), equalTo("Hello"))
    assertThat(r("nothing"), equalTo("Good bye"))
  }

  @Test
  fun testRule_Fluent() {
    val r = rule("Test rule with fluent syntax") {
      val givenClause: GivenExpression.() -> String = { anyString() }
      val whenClause = { s: String -> s == "test" }
      val thenClause = { s: String -> "Hello" }
      val elseClause = { s: String -> "Good bye" }

      given(givenClause)
        .and(whenClause)
        .thenReturn(thenClause)
        .otherwiseReturn(elseClause)
    }

    assertThat(r("test"), equalTo("Hello"))
    assertThat(r("nothing"), equalTo("Good bye"))
  }

  @Test
  fun testRule_Whose() {
    // Request class
    data class Request(val path: String)

    // Given accessor
    fun GivenExpression.anyRequest() = any(Request::class)

    val r = rule("Test rule with whose") {
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

    val helloRequest = Request("/api/v1/hello")
    val userRequest = Request("/api/v1/user")

    assertThat(r(helloRequest), equalTo("Hello"))
    assertThat(r(userRequest), equalTo("Good bye"))
  }
}