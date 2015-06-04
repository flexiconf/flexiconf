package se.blea.flexiconf.examples

import org.scalatest.{Matchers, FlatSpec}
import se.blea.flexiconf.{Configs, Schemas, Parser}

class ReadmeExampleSpec extends FlatSpec with Matchers {
  behavior of "README example"

  case class VirtualServer(name: String, aliases: List[String], root: String, admin: String)
  case class Server(address: String, port: Int, virtualServers: List[VirtualServer])

  it should "work flawlessly with the example in the readme" in {
    Parser.parseConfig(Configs.README, Schemas.README) map { config =>
      val server = config \ "server"
      val (address, port) = server % ("address", "port")

      val virtualHosts = (server \\ "virtualHost") map { vhost =>
        val name = vhost % "name"
        val (root, admin) = vhost \ ("root", "admin")
        val aliases = (vhost \\ "alias") flatMap (_.stringValue)
        new VirtualServer(name, aliases, root | "/var/www", admin)
      }

      new Server(address, port, virtualHosts)
    } map { server =>
      assert(server.address == "127.0.0.1")
      assert(server.port == 9000)

      assert(server.virtualServers.length == 3)

      assert(server.virtualServers(0) == VirtualServer(
        name = "tristan.blea.se",
        aliases = List("foo1.blea.se", "foo2.blea.se"),
        root = "/var/www/se/blea/tristan",
        admin = "tristan@blea.se"))

      assert(server.virtualServers(1) == VirtualServer(
        name = "flexiconf.blea.se",
        aliases = Nil,
        root = "/var/www/se/blea/flexiconf",
        admin = "nobody@blea.se"))

      assert(server.virtualServers(2) == VirtualServer(
        name = "other.blea.se",
        aliases = Nil,
        root = "/var/www",
        admin = ""))
    } orElse {
      fail("Couldn't parse config")
    }
  }
}
