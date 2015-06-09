package se.blea.flexiconf

import org.scalatest.{Matchers, FlatSpec}

class ConfigUsageSpec extends FlatSpec with Matchers {
  lazy val config = Parser.parseConfig(Configs.README, Schemas.README) getOrElse { fail("Couldn't load config") }
  lazy val server = config \ "server"


  behavior of "Config#directive"

  it should "allow you to select available, top level directives" in {
    assert(config.directive("server").name == "server")
  }

  it should "allow you to select available, top level directives but return unknown if none are defined" in {
    assert(config.directive("admin").name == "unknown")
  }

  it should "throw an exception for selecting directives that aren't available" in {
    intercept[IllegalStateException] {
      config.directive("foobar")
    }
  }

  behavior of "Config#directives"

  it should "return all directives at the top-level" in {
    assert(config.directives.length == 2)
    assert(config.directives(0).name == "server")
  }

  it should "allow you to select multiple, available top level directives" in {
    val result = config.directives("server", "admin")
    assert(result.length == 2)
    assert(result(0).name == "server")
    assert(result(1).name == "server")
  }

  it should "throw an exception for selecting directives that aren't available" in {
    intercept[IllegalStateException] {
      config.directives("server", "admin", "foobar", "bazqux")
    }
  }


  behavior of "Config#contains"

  it should "return true if the directive exists" in {
    val result = config ? "server"
    assert(result)
  }

  it should "return false if the directive doesn't exist" in {
    val result = config ? "admin"
    assert(!result)
  }


  behavior of "Directive#directive"

  it should "allow you to select available, directives within this context" in {
    assert(server.directive("virtualHost").name == "virtualHost")
  }

  it should "allow you to select available, top level directives but return unknown if none are defined" in {
    assert(server.directive("admin").name == "unknown")
  }

  it should "throw an exception for selecting directives that aren't available" in {
    intercept[IllegalStateException] {
      server.directive("foobar")
    }
  }


  behavior of "Directive#directives"

  it should "return all directives at the top-level" in {
    assert(config.directives.length == 2)
    assert(config.directives(0).name == "server")
    assert(config.directives(1).name == "server")
  }

  it should "allow you to select multiple, available top level directives" in {
    val result = config.directives("server", "admin")
    assert(result.length == 2)
    assert(result(0).name == "server")
    assert(result(1).name == "server")
  }

  it should "throw an exception for selecting directives that aren't available" in {
    intercept[IllegalStateException] {
      config.directives("server", "admin", "foobar", "bazqux")
    }
  }


  behavior of "Directive#contains"

  it should "return true if the directive exists" in {
    val result = (server \\ "virtualHost")(1) ? "default"
    assert(result)
  }

  it should "return false if the directive doesn't exist" in {
    val result = server \ "virtualHost" ? "default"
    assert(!result)
  }


  behavior of "Directive#apply"

  it should "return the first argument value if the directive exists and accepts arguments" in {
    val result: String = server \ "virtualHost"
    assert(result == "tristan.blea.se")
  }

  it should "return the null value if the directive doesn't exist but accepts arguments" in {
    val result: String = config \ "admin"
    assert(result == "")
  }


  behavior of "Directive#argValue"

  it should "return the named argument's value" in {
    val result = server % "address"
    assert(result == StringValue("127.0.0.1"))
  }

  it should "throw an exception if the argument doesn't exist" in {
    intercept[IllegalStateException] {
      server argValue "name"
    }
  }

  it should "return the null value if the directive and any children don't exist" in {
    val result: String = (config \\ "server")(1) \ "virtualHost" \ "admin" % "email"
    assert(result == "")
  }

  it should "throw an exception if the directive doesn't exist and the argument doesn't exist" in {
    intercept[IllegalStateException] {
      server \ "admin" % "foobar"
    }
  }

  behavior of "Directive#or"

  it should "return the named argument value if the directive exists" in {
    val result: String = server | "0.0.0.0"
    assert(result == "127.0.0.1")
  }

  it should "return the alternative value if the directive does not exist" in {
    val result: String = (server \\ "virtualHost")(2) \ "admin" | "foo@blea.se"
    assert(result == "foo@blea.se")
  }
}
