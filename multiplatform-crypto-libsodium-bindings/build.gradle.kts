/*
 *    Copyright 2019 Ugljesa Jovanovic
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.dokka.Platform
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.nativeplatform.platform.internal.Architectures
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

plugins {
    kotlin("multiplatform")
 //   id("maven-publish")
 //   id("signing")
    id("com.github.node-gradle.node") version "1.3.0"
    id("com.dorongold.task-tree") version "1.5"
    id("com.android.library")
    id("kotlin-android-extensions")
    id("org.jetbrains.dokka") version "1.5.30"

}


val sonatypeStaging = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
val sonatypeSnapshots = "https://oss.sonatype.org/content/repositories/snapshots/"

val sonatypePassword: String? by project

val sonatypeUsername: String? by project

val sonatypePasswordEnv: String? = System.getenv()["SONATYPE_PASSWORD"]
val sonatypeUsernameEnv: String? = System.getenv()["SONATYPE_USERNAME"]

repositories {
    mavenCentral()
    jcenter()
    google()
    maven { url = uri("https://jitpack.io") }
    maven { url  = uri("https://repo.sovrin.org/repository/maven-public")  }
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

}


group = ReleaseInfo.group
version = ReleaseInfo.bindingsVersion

val ideaActive = isInIdea()
println("Idea active: $ideaActive")
android {
    compileSdkVersion(AndroidPluginConfiguration.sdkVersion)
    defaultConfig {
        minSdkVersion(AndroidPluginConfiguration.minVersion)
        targetSdkVersion(AndroidPluginConfiguration.sdkVersion)
        //versionCode = 1
     //   versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
    }

    sourceSets.getByName("main") {
//        jniLibs.srcDir("src/androidMain/libs")
    }
}



kotlin {
    val hostOsName = getHostOsName()
    android() {
        publishLibraryVariants("release", "debug")
    }

    jvm()
    val projectRef = project


    println("Configuring macos targets")

    iosX64() {
        binaries {
            framework {
                optimized = true
            }
        }
    }
    iosArm64() {
        binaries {
            framework {
                optimized = true
            }
        }
    }
    iosArm32() {
        binaries {
            framework {
                optimized = true
            }
        }
    }
    /*
    iosSimulatorArm64() {
        binaries {
            framework {
                optimized = true
            }
        }
    }

     */

    macosX64() {
        binaries {
            framework {
                optimized = true
            }
        }
    }
    /*
    macosArm64() {
        binaries {
            framework {
                optimized = true
            }
        }
    }

     */
/*
    tvosX64() {
        binaries {
            framework {
                optimized = true
            }
        }
    }
    tvosArm64() {
        binaries {
            framework {
                optimized = true
            }
        }
    }
    tvosSimulatorArm64() {
        binaries {
            framework {
                optimized = true
            }
        }
    }

    watchosArm64() {
        binaries {
            framework {
                optimized = true
            }
        }
    }
    watchosArm32() {
        binaries {
            framework {
                optimized = true
            }
        }
    }
    watchosX86() {
        binaries {
            framework {
                optimized = true
            }
        }
    }
    watchosSimulatorArm64() {
        binaries {
            framework {
                optimized = true
            }
        }
    }



 */

    println("Configuring Mingw targets")
   // mingwX64() {
        /*
        binaries {
            staticLib {
                optimized = true
            }
        }
        compilations.getByName("main") {
            val libsodiumCinterop by cinterops.creating {
                defFile(projectRef.file("src/nativeInterop/cinterop/libsodium.def"))
                compilerOpts.add("-I${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-mingw-x86-64/include")
            }
            kotlinOptions.freeCompilerArgs = listOf(
                "-include-binary", "${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-mingw-x86-64/lib/libsodium.a"
            )
        }

         */
  //  }
    

    println(targets.names)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin(Deps.Common.stdLib))
                implementation(kotlin(Deps.Common.test))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin(Deps.Common.test))
                implementation(kotlin(Deps.Common.testAnnotation))
                implementation(Deps.Common.coroutines)
            }
        }

        val nativeDependencies = independentDependencyBlock {
        }

        val nativeMain by creating {
            dependsOn(commonMain)
            isRunningInIdea {
                kotlin.setSrcDirs(emptySet<String>())
            }
            dependencies {
                nativeDependencies(this)
            }
        }

        val nativeTest by creating {
            dependsOn(commonTest)
            isRunningInIdea {
                kotlin.setSrcDirs(emptySet<String>())
            }
            dependencies {
            }
        }



        //iosArm32, iosArm64, iosX64, macosX64, metadata, tvosArm64, tvosX64, watchosArm32, watchosArm64, watchosX86
        val macos64Bit = setOf(
            "macosX64", "macosArm64"
        )
        val iosArm = setOf(
            "iosArm64", "iosArm32"
        )
        val iosSimulator = setOf(
            "iosX64", "iosSimulatorArm64"
        )
        /*
        val mingw64Bit = setOf(
            "mingwX64"
        )


         */
        val tvosArm = setOf(
            "tvosArm64"
        )
        val tvosSimulator = setOf(
            "tvosX64", "tvosSimulatorArm64"
        )

        val watchosArm = setOf(
            "watchosArm32", "watchosArm64"
        )
        val watchosSimulator = setOf(
            "watchosX86", "watchosSimulatorArm64"
        )

        targets.withType<KotlinNativeTarget> {
            println("Target $name")

            compilations.getByName("main") {

                if (macos64Bit.contains(this@withType.name)) {
                    defaultSourceSet.dependsOn(createWorkaroundNativeMainSourceSet(this@withType.name, nativeDependencies))
                    println("Setting macos cinterop for $this")
                    val libsodiumCinterop by cinterops.creating {
                        val path = projectRef.file("src/nativeInterop/cinterop/libsodium.def")
                        println("log Setting macos cinterop for $path")
                        val file = defFile(path)
                        println("log Setting macos cinterop for $file")
                        println("log Setting macos cinterop for ${projectRef.rootDir}")
                        compilerOpts.add("-I${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-macos/include")
                    }
                    kotlinOptions.freeCompilerArgs = listOf(
                        "-include-binary", "${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-macos/lib/libsodium.a"
                    )
                }
                //All ioses share the same static library
                if (iosArm.contains(this@withType.name)) {
                    defaultSourceSet.dependsOn(createWorkaroundNativeMainSourceSet(this@withType.name, nativeDependencies))
                    println("Setting ios cinterop for $this")
                    val libsodiumCinterop by cinterops.creating {
                        defFile(projectRef.file("src/nativeInterop/cinterop/libsodium.def"))
                        compilerOpts.add("-I${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-ios/include")
                    }
                    kotlinOptions.freeCompilerArgs = listOf(
                        "-include-binary", "${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-ios/lib/libsodium.a"
                    )
                }

                if (iosSimulator.contains(this@withType.name)) {
                    defaultSourceSet.dependsOn(createWorkaroundNativeMainSourceSet(this@withType.name, nativeDependencies))
                    println("Setting ios cinterop for $this")
                    val libsodiumCinterop by cinterops.creating {
                        defFile(projectRef.file("src/nativeInterop/cinterop/libsodium.def"))
                        compilerOpts.add("-I${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-ios-simulators/include")
                    }
                    kotlinOptions.freeCompilerArgs = listOf(
                        "-include-binary", "${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-ios-simulators/lib/libsodium.a"
                    )
                }

                if (tvosArm.contains(this@withType.name)) {
                    defaultSourceSet.dependsOn(createWorkaroundNativeMainSourceSet(this@withType.name, nativeDependencies))
                    println("Setting ios cinterop for $this")
                    val libsodiumCinterop by cinterops.creating {
                        defFile(projectRef.file("src/nativeInterop/cinterop/libsodium.def"))
                        compilerOpts.add("-I${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-tvos/include")
                    }
                    kotlinOptions.freeCompilerArgs = listOf(
                        "-include-binary", "${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-tvos/lib/libsodium.a"
                    )
                }

                if (tvosSimulator.contains(this@withType.name)) {
                    defaultSourceSet.dependsOn(createWorkaroundNativeMainSourceSet(this@withType.name, nativeDependencies))
                    println("Setting ios cinterop for $this")
                    val libsodiumCinterop by cinterops.creating {
                        defFile(projectRef.file("src/nativeInterop/cinterop/libsodium.def"))
                        compilerOpts.add("-I${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-tvos-simulators/include")
                    }
                    kotlinOptions.freeCompilerArgs = listOf(
                        "-include-binary", "${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-tvos-simulators/lib/libsodium.a"
                    )
                }

                if (watchosArm.contains(this@withType.name)) {
                    defaultSourceSet.dependsOn(createWorkaroundNativeMainSourceSet(this@withType.name, nativeDependencies))
                    println("Setting ios cinterop for $this")
                    val libsodiumCinterop by cinterops.creating {
                        defFile(projectRef.file("src/nativeInterop/cinterop/libsodium.def"))
                        compilerOpts.add("-I${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-watchos/include")
                    }
                    kotlinOptions.freeCompilerArgs = listOf(
                        "-include-binary", "${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-watchos/lib/libsodium.a"
                    )
                }

                if (watchosSimulator.contains(this@withType.name)) {
                    defaultSourceSet.dependsOn(createWorkaroundNativeMainSourceSet(this@withType.name, nativeDependencies))
                    println("Setting ios cinterop for $this")
                    val libsodiumCinterop by cinterops.creating {
                        defFile(projectRef.file("src/nativeInterop/cinterop/libsodium.def"))
                        compilerOpts.add("-I${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-watchos-simulators/include")
                    }
                    kotlinOptions.freeCompilerArgs = listOf(
                        "-include-binary", "${projectRef.rootDir}/multiplatform-crypto-libsodium-bindings/sodiumWrapper/static-watchos-simulators/lib/libsodium.a"
                    )
                }



            }
            compilations.getByName("test") {
                println("Setting native test dep for $this@withType.name")
                defaultSourceSet.dependsOn(nativeTest)


            }
        }

        val androidMain by getting {
            isNotRunningInIdea {
                kotlin.srcDirs("src/androidMain", "src/androidSpecific", "src/jvmMain/kotlin")
            }
            isRunningInIdea {
                kotlin.srcDirs("src/androidSpecific", "src/jvmMain/kotlin")
            }
            dependencies {
                implementation("net.java.dev.jna:jna:5.5.0@aar")
                implementation(Deps.Jvm.resourceLoader) {
                    exclude("net.java.dev.jna", "jna")
                }
            }
        }

        val androidTest by getting {
            dependencies {
//                    implementation(kotlin(Deps.Jvm.test))
//                    implementation(kotlin(Deps.Jvm.testJUnit))
//                    implementation("androidx.test:runner:1.2.0")
//                    implementation("androidx.test:rules:1.2.0")
            }
        }

        val jvmMain by getting {
            kotlin.srcDirs("src/jvmSpecific", "src/jvmMain/kotlin")
            dependencies {
                implementation(kotlin(Deps.Jvm.stdLib))
                implementation(kotlin(Deps.Jvm.test))
                implementation(kotlin(Deps.Jvm.testJUnit))

                implementation(Deps.Jvm.resourceLoader)

                implementation(Deps.Jvm.Delegated.jna)

                implementation("org.slf4j:slf4j-api:1.7.30")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin(Deps.Jvm.test))
                implementation(kotlin(Deps.Jvm.testJUnit))
                implementation(kotlin(Deps.Jvm.reflection))
            }
        }


        runningOnMacos {
            println("Configuring Macos source sets")
            val macosX64Main by getting {
                dependsOn(nativeMain)
                if (ideaActive) {
                    kotlin.srcDir("src/nativeMain/kotlin")
                }

            }
            val macosX64Test by getting {
                dependsOn(nativeTest)
                if (ideaActive) {
                    kotlin.srcDir("src/nativeTest/kotlin")
                }

            }
/*
            val tvosX64Main by getting {
                dependsOn(commonMain)
            }

            val tvosArm64Main by getting {
                dependsOn(commonMain)
            }

            val watchosX86Main by getting {
                dependsOn(commonMain)
            }

            val watchosArm64Main by getting {
                dependsOn(commonMain)
            }

            val watchosArm32Main by getting {
                dependsOn(commonMain)
            }


 */
        }

/*
        if (hostOsName == "windows") {
            val mingwX64Main by getting {
                dependsOn(nativeMain)
                if (ideaActive) {
                    kotlin.srcDir("src/nativeMain/kotlin")
                }
            }

            val mingwX64Test by getting {
                dependsOn(nativeTest)
                if (ideaActive) {
                    kotlin.srcDir("src/nativeTest/kotlin")
                }
            }
        }


 */

        all {
            languageSettings.enableLanguageFeature("InlineClasses")
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
        }
    }


}

tasks.whenTaskAdded {
    if("DebugUnitTest" in name || "ReleaseUnitTest" in name) {
        enabled = false // https://youtrack.jetbrains.com/issue/KT-34662 otherwise common tests fail, because we require native android libs to be loaded
    }
}

tasks {




    create<Jar>("javadocJar") {
        dependsOn(dokkaHtml)
        archiveClassifier.set("javadoc")
        from(dokkaHtml.get().outputDirectory)
    }

    dokkaHtml {
        println("Dokka !")
        dokkaSourceSets {
        }
    }



    /*
    if (getHostOsName() == "windows") {
        val mingwX64Test by getting(KotlinNativeTest::class) {

            testLogging {
                events("PASSED", "FAILED", "SKIPPED")
                showStandardStreams = true
            }
        }
    }


     */
}

allprojects {
    tasks.withType(JavaCompile::class) {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
}


/*
signing {
    isRequired = false
    sign(publishing.publications)
}

publishing {
    publications.withType(MavenPublication::class) {
        artifact(tasks["javadocJar"])
        pom {
            name.set("Kotlin Multiplatform Crypto")
            description.set("Kotlin Multiplatform Libsodium Wrapper")
            url.set("https://github.com/ionspin/kotlin-multiplatform-crypto")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("ionspin")
                    name.set("Ugljesa Jovanovic")
                    email.set("opensource@ionspin.com")
                }
            }
            scm {
                url.set("https://github.com/ionspin/kotlin-multiplatform-libsodium")
                connection.set("scm:git:git://git@github.com:ionspin/kotlin-multiplatform-libsodium.git")
                developerConnection.set("scm:git:ssh://git@github.com:ionspin/kotlin-multiplatform-libsodium.git")

            }

        }
    }
    

    repositories {
        maven {

            url = uri(sonatypeStaging)
            credentials {
                username = sonatypeUsername ?: sonatypeUsernameEnv ?: ""
                password = sonatypePassword ?: sonatypePasswordEnv ?: ""
            }
        }

        maven {
            name = "snapshot"
            url = uri(sonatypeSnapshots)
            credentials {
                username = sonatypeUsername ?: sonatypeUsernameEnv ?: ""
                password = sonatypePassword ?: sonatypePasswordEnv ?: ""
            }
        }
    }
}


 */


object Versions {
    val kotlinCoroutines = "1.5.2-native-mt"
    val kotlin = "1.5.31"
    val kotlinSerialization = "1.3.0-RC"
    val kotlinSerializationPlugin = "1.5.31"
    val atomicfu = "0.14.3-M2-2-SNAPSHOT" //NOTE: my linux arm32 and arm64 build
    val nodePlugin = "1.3.0"
    val dokkaPlugin = "1.5.0"
    val taskTreePlugin = "1.5"
    val kotlinBigNumVersion = "0.2.8"
    val jna = "5.7.0"
    val kotlinPoet = "1.6.0"
    val sampleLibsodiumBindings = "0.8.5-SNAPSHOT"
    val ktor = "1.3.2"
    val timber = "4.7.1"
    val kodeinVersion = "7.1.0"

    val resourceLoader = "2.0.1"




}

object ReleaseInfo {
    val group = "com.ionspin.kotlin"
    val bindingsVersion = "0.8.5-SNAPSHOT"
}

object Deps {

    object Common {
        val stdLib = "stdlib-common"
        val test = "test-common"
        val testAnnotation = "test-annotations-common"
        val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}"
        val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinSerialization}"
        val atomicfu = "com.ionspin.kotlin.atomicfu:atomicfu:${Versions.atomicfu}"


        val kotlinBigNum = "com.ionspin.kotlin:bignum:${Versions.kotlinBigNumVersion}"

        val apiProject = ":multiplatform-crypto-api"

        val libsodiumBindings = "com.ionspin.kotlin:multiplatform-crypto-libsodium-bindings:${Versions.sampleLibsodiumBindings}"

        val kodein = "org.kodein.di:kodein-di:${Versions.kodeinVersion}"
    }

    object Js {

        object JsVersions {
            val react = "17.0.2-pre.218-kotlin-1.5.21"
            val reactNpm = "17.0.2"
            val styled = "5.3.0-pre.218-kotlin-1.5.21"
            val styledNpm = "5.3.0"

        }

        val stdLib = "stdlib-js"
        val test = "test-js"
        val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:${Versions.kotlinCoroutines}"
        val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:${Versions.kotlinSerialization}"

        val ktorClient = "io.ktor:ktor-client-js:${Versions.ktor}"
        val ktorClientSerialization = "io.ktor:ktor-client-serialization-js:${Versions.ktor}"
        val ktorClientWebSockets = "io.ktor:ktor-client-websockets-js:${Versions.ktor}"

        object Npm {
            val libsodium = Pair("libsodium-wrappers-sumo", "0.7.9")
            //val libsodiumWrappers = Pair("libsodium-wrappers-sumo", "file:${getProjectPath()}/multiplatform-crypto-delegated/libsodium-wrappers-sumo-0.7.6.tgz")
            val libsodiumWrappers = Pair("libsodium-wrappers-sumo", "0.7.9")

        }

    }

    object Jvm {
        val stdLib = "stdlib-jdk8"
        val test = "test"
        val testJUnit = "test-junit"
        val reflection = "reflect"
        val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}"
        val coroutinesjdk8 = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.kotlinCoroutines}"
        val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Versions.kotlinSerialization}"
        val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.kotlinCoroutines}"

        val kotlinPoet = "com.squareup:kotlinpoet:${Versions.kotlinPoet}"

        val resourceLoader = "com.goterl:resource-loader:${Versions.resourceLoader}"

        object Delegated {
            val jna = "net.java.dev.jna:jna:${Versions.jna}"
        }
    }

    object iOs {
        val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:${Versions.kotlinSerialization}"
        val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core-native:${Versions.kotlinCoroutines}"
    }

    object Native {
        val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:${Versions.kotlinSerialization}"
        val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core-native:${Versions.kotlinCoroutines}"

    }

    object Android {
        val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.kotlinCoroutines}"
        val ktorClientOkHttp = "io.ktor:ktor-client-okhttp:${Versions.ktor}"
        val ktorClient = "io.ktor:ktor-client-android:${Versions.ktor}"
        val ktorClientSerialization = "io.ktor:ktor-client-serialization-jvm:${Versions.ktor}"
        val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Versions.kotlinSerialization}"
        val timber = "com.jakewharton.timber:timber:${Versions.timber}"
        val jna = "net.java.dev.jna:jna:${Versions.jna}"
    }

    object Desktop {
        val libui = "com.github.msink:libui:0.1.8"
    }

}

object AndroidPluginConfiguration {
    val sdkVersion = 31
    val targetVersion = 31
    val minVersion = 24
}


object PluginsDeps {
    val kotlinSerializationPlugin = "plugin.serialization"
    val multiplatform = "multiplatform"
    val node = "com.github.node-gradle.node"
    val mavenPublish = "maven-publish"
    val signing = "signing"
    val dokka = "org.jetbrains.dokka"
    val taskTree = "com.dorongold.task-tree"
    val androidLibrary = "com.android.library"
    val kotlinAndroidExtensions = "kotlin-android-extensions"
    val androidApplication = "com.android.application"
    val kotlinAndroid = "kotlin-android"
    val kapt = "kotlin-kapt"
}


fun isInIdea() = System.getProperty("idea.active") == "true"

fun isInGitlabCi() = System.getenv("GITLAB_CI") == "true"

fun getProjectPath() : String {
    val path = System.getProperty("PROJECT_PATH")
    return path
}

fun getHostOsName(): String {
    val target = System.getProperty("os.name")
    if (target == "Linux") return "linux"
    if (target.startsWith("Windows")) return "windows"
    if (target.startsWith("Mac")) return "macos"
    return "unknown"
}

fun getHostArchitecture(): String {
    val architecture = System.getProperty("os.arch")
    DefaultNativePlatform.getCurrentArchitecture()
    println("Arch: $architecture")
    val resolvedArch = Architectures.forInput(architecture).name
    println("Resolved arch: $resolvedArch")
    return resolvedArch
}



fun KotlinMultiplatformExtension.isRunningInIdea(block: KotlinMultiplatformExtension.() -> Unit) {
    if (isInIdea()) {
        block(this)
    }
}

fun KotlinMultiplatformExtension.isNotRunningInIdea(block: KotlinMultiplatformExtension.() -> Unit) {
    if (!isInIdea()) {
        block(this)
    }
}

fun KotlinMultiplatformExtension.isRunningInGitlabCi(block: KotlinMultiplatformExtension.() -> Unit) {
    if (isInGitlabCi()) {
        block(this)
    }
}




fun KotlinMultiplatformExtension.runningOnMacos(block: KotlinMultiplatformExtension.() -> Unit) {
    if (getHostOsName() == "macos") {
        block(this)
    }
}

fun KotlinMultiplatformExtension.runningOnWindows(block: KotlinMultiplatformExtension.() -> Unit) {
    if (getHostOsName() == "windows") {
        block(this)
    }
}

fun independentDependencyBlock(nativeDeps: KotlinDependencyHandler.() -> Unit): KotlinDependencyHandler.() -> Unit {
    return nativeDeps
}

/**
 * On mac when two targets that have the same parent source set have cinterops defined, gradle creates a "common"
 * target task for that source set metadata, even though it's a native source set, to work around that, we create
 * an intermediary source set with the same set of dependancies
 *
 */
fun NamedDomainObjectContainer<KotlinSourceSet>.createWorkaroundNativeMainSourceSet(
    name: String,
    nativeDeps: KotlinDependencyHandler.() -> Unit
): KotlinSourceSet {

    return create("${name}Workaround") {
        if (!isInIdea()) {
            kotlin.srcDir("src/nativeMain")
            dependencies {
                nativeDeps.invoke(this)
            }
        }
    }

}

