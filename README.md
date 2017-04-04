
FutureBot-Discord
=================


Discord Bot (to be) used in the official FuturemanGaming discord server!
Written in [DV8FromTheWorld/JDA](https://github.com/DV8FromTheWorld/JDA) with possible slight modifications.

The music is module is powered by [LavaPlayer](https://github.com/sedmelluq/lavaplayer)

## Build 

This project uses [gradle](https://gradle.org/) to build. In addition it is built with the shadowJar task
which can be found in [build.gradle](/build.gradle).

### Using gradle to build:

Firstly make sure to use the gradle version this was built in

> Update gradle: `gradlew wrapper` 


Secondly run the build task
 
> Run: `gradlew run`

## Requirements

Kotlin v1.1.1

Gradle v3.4.2


### Dependencies

All of the listed dependencies should include the transitive dependencies. (e.g. JDA comes with org.json)

- [Kotlin stdlib-jre8](https://github.com/JetBrains/Kotlin)
    created by [JetBrains](https://github.com/JetBrains)
- [JDA v3.0.0](https://github.com/DV8FromTheWorld/JDA)
    created by [DV8FromTheWorld](https://github.com/DV8FromTheWorld)
- [Kotlin-JDA](https://github.com/JDA-Applications/Kotlin-JDA)
    created by [MinnDevelopment](https://github.com/MinnDevelopment)
- [LavaPlayer v1.2.34+](https://github.com/sedmelluq/lavaplayer)
    created by [sedmelluq](https://github.com/sedmelluq)
- [JDA-NAS v1.0.3+](https://github.com/sedmelluq/jda-nas)
    created by [sedmelluq](https://github.com/sedmelluq)
