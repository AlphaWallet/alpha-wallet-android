# Build instruction
### NOTE: Use JDK11
- Android Gradle Plugin requires Java 11
- Java 16 is not supported due to IllegalAccessError compile error when Realm tries to access the com.sun.tools.javac package

To build everything:

```
$ ./gradle build
```

You can also use your locally installed gradle, provide that the version is higher than 7.0.2

```
$ gradle build
```

## Build dmz server

DMZ server is a SpringBoot based server application that is deployed on a DMZ host, intentionally read only, stateless, have no database access and have no keys to steal from. It was made in 2017 to provide ad-hoc ad-hoc services, display token status by reading from blockchain using client wallet and provide server verification strings. There are plans to phase it out completely.

To build the dmz kit:

```
$ cd dmz
dmz $ gradle build bootJar

> Task :dmz:compileJava
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.

BUILD SUCCESSFUL in 1m 1s
5 actionable tasks: 3 executed, 2 up-to-date
```

This will give you `build/libs/dmz.jar`

Then, you can copy the jar file to the server in which this was deployed and restart the SpringBoot service.

