import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.4.20"
	application
}

group = "com.romangraef"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
	maven { setUrl("https://dl.bintray.com/hotkeytlt/maven") }
}

dependencies {
	testImplementation(kotlin("test-junit"))
	implementation("com.github.h0tk3y.betterParse:better-parse:0.4.0")
}

tasks.test {
	useJUnit()
}

tasks.withType<KotlinCompile>() {
	kotlinOptions.jvmTarget = "1.8"
}

application {
	mainClassName = "MainKt"
}