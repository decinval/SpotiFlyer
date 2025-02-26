/*
 *  * Copyright (c)  2021  Shabinder Singh
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  *  You should have received a copy of the GNU General Public License
 *  *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    kotlin("js")
    //id("org.jetbrains.kotlin.js") version "1.4.31"
}

group = "com.shabinder"
version = "0.1"

repositories {
    jcenter()
    mavenCentral()
    maven(url = "https://dl.bintray.com/kotlin/kotlin-js-wrappers")
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation(Decompose.decompose)
    implementation(Koin.core)
    implementation(Ktor.clientJs)
    implementation(MVIKotlin.mvikotlin)
    implementation(MVIKotlin.coroutines)
    implementation(MVIKotlin.mvikotlinMain)
    implementation(MVIKotlin.mvikotlinLogging)
    implementation(project(":common:root"))
    implementation(project(":common:main"))
    implementation(project(":common:list"))
    implementation(project(":common:database"))
    implementation(project(":common:data-models"))
    implementation(project(":common:dependency-injection"))
    implementation("org.jetbrains:kotlin-react:17.0.1-pre.148-kotlin-1.4.30")
    implementation("org.jetbrains:kotlin-react-dom:17.0.1-pre.148-kotlin-1.4.30")
    implementation("org.jetbrains:kotlin-styled:1.0.0-pre.115-kotlin-1.4.10")
    implementation("org.jetbrains:kotlin-react-router-dom:5.2.0-pre.148-kotlin-1.4.30")
}

kotlin {
    js() {
        //useCommonJs()
        browser {
            webpackTask {
                cssSupport.enabled = true
            }
            runTask {
                cssSupport.enabled = true
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
        binaries.executable()
    }
}