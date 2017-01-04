#!/bin/sh
LIVE=false                                                  # whether to announce that futureman is live or not
LOC=futurebot-discord-ALPHA-all.jar                         # where to find the application jar
java -ea -Dtwitch.live=$LIVE -jar "$LOC"
