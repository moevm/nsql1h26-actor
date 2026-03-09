plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.2"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.openapi.generator") version "7.2.0"
}

group = "com.NOSQL"
version = "0.0.1-SNAPSHOT"
description = "NOSQL backend"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mongodb")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	jvmToolchain(25)
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<JavaCompile> {
	targetCompatibility = "24"
	sourceCompatibility = "24"
}

tasks.withType<Test> {
	useJUnitPlatform()
}

openApiGenerate {
	generatorName.set("kotlin-spring")
	inputSpec.set("${project.projectDir.parent}/spec/api.yaml")
	outputDir.set("$buildDir/generated")
	apiPackage.set("com.NOSQL.NOSQL.api")
	modelPackage.set("com.NOSQL.NOSQL.model.generated")
	configOptions.set(mapOf(
		"library" to "spring-boot",
		"useSpringBoot3" to "true",
		"interfaceOnly" to "true"
	))
}

tasks.compileKotlin {
	dependsOn(tasks.openApiGenerate)
}
sourceSets["main"].kotlin.srcDirs("$buildDir/generated/src/main/kotlin")
