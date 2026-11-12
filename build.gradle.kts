import java.time.Duration

plugins {
    `java-library`
}

// The version is set in gradle.properties. Workflows pass -Pversion for
// nightly and pull request builds.

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    // Java 23 is the newest Temurin release available on every CI platform.
    options.release = 23
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-restricted", "-Werror"))
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
}

tasks.jar {
    // The jar holds no native code: the Signal Hound libraries are found at
    // runtime where the SDK installed them. See NativeLibraryLoader.
    from(files("LICENSE", "THIRD-PARTY-NOTICES.md")) {
        into("META-INF")
    }
    manifest {
        attributes(
            "Implementation-Title" to "jSignalHound",
            "Implementation-Version" to project.version,
            "Automatic-Module-Name" to "io.github.dsheirer.signalhound",
            "Main-Class" to "io.github.dsheirer.signalhound.SignalHoundInfo",
            "Enable-Native-Access" to "ALL-UNNAMED")
    }
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    // Directories holding Signal Hound libraries, such as those that
    // native/fetch-sdk.sh extracts. Without it the tests use the libraries
    // installed on the machine, and skip the APIs that are not installed.
    providers.gradleProperty("libraryPath").orNull?.let {
        systemProperty("jsignalhound.library.path", it)
    }
    // APIs whose library must load, such as "bb,sm,sp", so that CI fails rather
    // than skips when a library it provides cannot be loaded.
    systemProperty("jsignalhound.test.require", providers.gradleProperty("requireApis").getOrElse(""))
    systemProperty("jsignalhound.hardware.test", providers.gradleProperty("hardwareTest").getOrElse("false"))
    // A native call that never returns would otherwise keep the test worker,
    // and with it the USB device, busy until it is killed by hand.
    timeout = Duration.ofMinutes(10)
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
