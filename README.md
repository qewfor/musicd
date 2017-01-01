
FutureBot-Discord
=================


Discord Bot (to be) used in the official FuturemanGaming discord server!
Written in [DV8FromTheWorld/JDA](https://github.com/DV8FromTheWorld/JDA) with possible slight modifications.
This Bot should use [PhantomBot](https://github.com/PhantomBot/PhantomBot) to retrieve all of its custom commands
that can be created/edited/deleted via the native web panel. The API connection is established through a WebSocketClient.

## Build 

This project uses [gradle](https://gradle.org/) to build. In addition it is built with the shadowJar task
which can be found in [build.gradle](/build.gradle).

### Using gradle to build:

Firstly make sure to use the gradle version this was built in

Update gradle: `gradlew wrapper` 


Secondly run the build task (shadowJar)
 
Run: `gradlew clean shadowJar`

## Requirements

Kotlin v1.0.6

Gradle v3.2.1


### Dependencies

All of the listed dependencies should include the transitive dependencies. (e.g. JDA comes with org.json)

- Kotlin v1.0.6
    created by [JetBrains](https://github.com/JetBrains)
- JDA v3.0.BETA2
    created by [DV8FromTheWorld](https://github.com/JetBrains)
