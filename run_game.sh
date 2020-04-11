#!/bin/sh

javac JaredBot.java
javac SumoBot.java
./halite -d "240 160" "java JaredBot" "java SumoBot"
