# Flexiconf

[![Build Status](https://travis-ci.org/flexiconf/flexiconf.svg?branch=add-travis-ci)](https://travis-ci.org/flexiconf/flexiconf)

Flexible configuration for JVM projects.

**Config**
- Syntax similar to nginx and other open source projects
- Includes built-in directives (`include`, `group`, and `use`) for organizing configuration

**Schema**
- Define your own schema with configuration directives that accept typed parameters and blocks
- Define in flat files or build programmatically at runtime
- Generate automated documentation for your schema using Markdown and mustache templates

**API**
- Scala API
- Java-friendly API

**CLI tooling**
- Config/schema inspection and debugging
- Config/schema validation (great for running as part of your build)
- Schema documentation generation

### Sample schema

```
## ## Servers
##
## Defines a server and the interface this server should listen on
## - <pre>server :9000 { ... }</pre>
## - <pre>server 127.0.0.1:8080 { ... }</pre>
##
server address:String port:Int {

    ## ### Virtual hosts
    ## Defines a virtual host for this server
    ## - <pre>virtualHost tristan.blea.se { ... }</pre>
    ##
    virtualHost name:String {

        ## Defines additional aliases to use for this virtual host
        ## - <pre>alias foo.blea.se;</pre>
        ##
        alias name:String;

        ## Defines the path on the filesystem to serve for this virtual host's root
        root path:String [once];

    }
}
```

### Sample configuration

```
server 127.0.0.1 9000 {
    virtualHost tristan.blea.se {
        alias foo.blea.se;
        root /var/www/se/blea/tristan;
    }

    virtualHost flexiconf.blea.se {
        root /var/www/se/blea/flexiconf;
    }
    
    virtualHost other.blea.se {
        include /path/to/other/config.conf; 
    }
}

### Sample usage

Add the following to your project's `build.sbt`:

```scala
libraryDependencies += "se.blea.flexiconf" %% "flexiconf-core" % "0.0.1"
```

And, in your code add the following:

```scala
import se.blea.flexiconf.Parser

val confPath = "/path/to/config.conf"
val schemaPath = "/path/to/schema.conf"

Parser.parseConfig(confPath, schemaPath) map { config =>
  // Use \ to select the first directive named "server"
  val server = config \ "server"
  
  // Use % to pluck arguments named "port" and "address" from the "server" 
  // directive we selected above
  val (address, port) = server % ("address", "port")

  // Use \\ to select all directives matching the name "virtualHost"
  val virtualHosts = (server \\ "virtualHost") map { vhost =>
    val name = vhost % "name" 
  
    // Use \ to select the first directives named "alias" and "root"
    val (alias, root) = vhost \ ("alias", "root")
  
    // Use the Directive or ArgumentValue in place of Strings, Ints, 
    // Floats, Doubles, and Booleans. Use | to specify default values 
    // if a Directive or ArgumentValue may not exist
    new VirtualServer(name, alias, root | "/var/www")
  }

  new Server(address, port, virtualHosts)
}
```

### Documentation

See the [wiki](https://github.com/flexiconf/flexiconf/wiki) for more information about usage, configs, schemas, and tools.

### License

MIT
