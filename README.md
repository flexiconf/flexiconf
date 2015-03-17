# Flexiconf

Flexible configuration for JVM projects.

**Schema**
- Define your own schema with configuration directives that accept typed parameters and blocks
- Define in flat files or build programmatically at runtime
- Generate automated documentation for your schema using Markdown

**Config**
- Includes built-in directives (`include`, `group`, and `use`) for organizing configuration

### Sample schema

```
## ## Servers
##
## Defines a server and the interface this server should listen on
## - <pre>server :9000 { ... }</pre>
## - <pre>server 127.0.0.1:8080 { ... }</pre>
##
server listenInterface:String {

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
server :9000 {
    virtualHost tristan.blea.se {
        alias foo.blea.se;
        root /var/www/se/blea/tristan;
    }

    virtualHost flexiconf.blea.se {
        root /var/www/se/blea/flexiconf;
    }
}
