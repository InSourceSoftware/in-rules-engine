package io.insource.framework.rule

import org.junit.Before
import org.junit.Test

class RulesEngineTest {
  @Before
  fun setUp() {
    useMapAccessStrategy()
  }

  @Test
  fun testRulesEngine_Map() {
    val ruleSet = rules(
      rule("request is login") {
        given {
          anyString("requestPath")
        } alwaysReturn {
          it.contains("/login")
        }
      },
      rule("request is send order") {
        given {
          anyString("requestPath")
        } alwaysReturn {
          it.endsWith("/orders")
        }
      },
      rule("request is logout") {
        given {
          anyString("requestPath")
        } alwaysReturn {
          it.contains("/logout")
        }
      }
    )

    val events = listOf(
      eventFor("/api/v1/login"),
      eventFor("/api/v1/user"),
      eventFor("/api/v1/messages"),
      eventFor("/api/v1/dashboard"),
      eventFor("/api/v1/search", "category=Kitchen&q=Kitchen%20Aid%20Mixer"),
      eventFor("/api/v1/orders"),
      eventFor("/api/v1/orders/123"),
      eventFor("/api/v1/logout")
    )

    for (i in events.indices) {
      val event = events[i]
      println("Processing event ${i + 1} with request path ${event["requestPath"]}")

      for (rule in ruleSet) {
        if (rule(event)) {
          println("+Rule \"${rule.description}\" matched")
        }
      }
    }
  }

  @Test
  fun testRulesEngine_Request() {
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
  }

  fun GivenExpression.anyRequest() = any(Request::class)

  companion object {
    private fun eventFor(path: String, queryString: String? = null): Map<String, String?> {
      return mapOf(
        "headers" to "Cookies: sid=abc123\nSM_USER=johndoe",
        "requestURL" to "https://www.ecommerce.biz",
        "requestPath" to path,
        "queryString" to queryString
      )
    }

    private fun requestFor(path: String, queryString: String? = null): Request {
      return Request(
        "Cookies: sid=abc123\nSM_USER=johndoe",
        "https://www.ecommerce.biz",
        path,
        queryString
      )
    }
  }

  data class Request(
    val headers: String,
    val requestUrl: String,
    val requestPath: String,
    val queryString: String?
  )
}