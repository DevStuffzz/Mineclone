plugins {
    id("java")
    id("application")
}

group = "com.coreybeaver"
version = "1.0-SNAPSHOT"

val lwjglVersion = "3.4.1"
val jomlVersion = "1.10.8"

val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()

val lwjglNatives = when {
    osName.contains("win") -> "natives-windows"
    osName.contains("mac") && osArch.contains("aarch64") -> "natives-macos-arm64"
    osName.contains("mac") -> "natives-macos"
    osName.contains("linux") && (osArch.contains("arm") || osArch.contains("aarch64")) -> "natives-linux-arm64"
    osName.contains("linux") -> "natives-linux"
    else -> error("Unsupported platform: os=$osName arch=$osArch")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-assimp")
    implementation("org.lwjgl:lwjgl-freetype")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-msdfgen")
    implementation("org.lwjgl:lwjgl-openal")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-stb")

    implementation("org.lwjgl:lwjgl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-assimp::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-freetype::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-msdfgen::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-openal::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-stb::$lwjglNatives")

    implementation("org.joml:joml:$jomlVersion")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.coreybeaver.mineclone.Main")
}

tasks.withType<JavaExec>().configureEach {
    if (osName.contains("mac")) {
        jvmArgs("-XstartOnFirstThread")
    }
}