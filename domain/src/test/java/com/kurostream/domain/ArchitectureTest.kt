// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.domain

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Test

class ArchitectureTest {

    private val classes: JavaClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests::class.java)
        .importPackages("com.kurostream")

    @Test
    fun `domain should not depend on data or app`() {
        classes().that().resideInAPackage("..domain..")
            .should().onlyHaveDependentPackagesThatResideInAnyPackage(
                "..domain..",
                "kotlin..",
                "org.jetbrains.kotlinx..",
                "androidx..",
                "javax.."
            )
            .check(classes)
    }

    @Test
    fun `data should only depend on domain and common`() {
        classes().that().resideInAPackage("..data..")
            .should().onlyHaveDependentPackagesThatResideInAnyPackage(
                "..data..",
                "..domain..",
                "..common..",
                "..cache..",
                "kotlin..",
                "org.jetbrains.kotlinx..",
                "androidx..",
                "javax..",
                "com.google.dagger..",
                "dagger..",
                "com.squareup.retrofit2..",
                "com.squareup.moshi..",
                "com.squareup.okhttp3..",
                "io.ktor..",
                "com.google.firebase.."
            )
            .check(classes)
    }

    @Test
    fun `app should not depend on data implementations directly`() {
        classes().that().resideInAPackage("..app..")
            .should().notDependOnClassesThat().resideInAPackage("..data..impl..")
            .check(classes)
    }

    @Test
    fun `common should not depend on any other module`() {
        classes().that().resideInAPackage("..common..")
            .should().onlyHaveDependentPackagesThatResideInAnyPackage(
                "..common..",
                "kotlin..",
                "org.jetbrains.kotlinx..",
                "androidx..",
                "javax.."
            )
            .check(classes)
    }

    @Test
    fun `playback should only depend on domain and common`() {
        classes().that().resideInAPackage("..playback..")
            .should().onlyHaveDependentPackagesThatResideInAnyPackage(
                "..playback..",
                "..domain..",
                "..common..",
                "kotlin..",
                "org.jetbrains.kotlinx..",
                "androidx..",
                "javax..",
                "com.google.dagger..",
                "dagger..",
                "androidx.media3.."
            )
            .check(classes)
    }

    @Test
    fun `extensions should only depend on domain and common`() {
        classes().that().resideInAPackage("..extensions..")
            .should().onlyHaveDependentPackagesThatResideInAnyPackage(
                "..extensions..",
                "..domain..",
                "..common..",
                "..cache..",
                "kotlin..",
                "org.jetbrains.kotlinx..",
                "androidx..",
                "javax..",
                "com.google.dagger..",
                "dagger..",
                "com.squareup.retrofit2..",
                "com.squareup.moshi..",
                "com.squareup.okhttp3..",
                "io.ktor.."
            )
            .check(classes)
    }

    @Test
    fun `no cycles between modules`() {
        classes().should().notHaveCycles()
            .check(classes)
    }

    @Test
    fun `ViewModels should reside in app module`() {
        classes().that().haveSimpleNameEndingWith("ViewModel")
            .should().resideInAPackage("..app..")
            .check(classes)
    }

    @Test
    fun `Repositories should have Impl suffix in data module`() {
        classes().that().resideInAPackage("..data..")
            .and().haveSimpleNameEndingWith("Repository")
            .should().haveSimpleNameEndingWith("Impl")
            .check(classes)
    }

    @Test
    fun `UseCases should reside in domain module`() {
        classes().that().haveSimpleNameEndingWith("UseCase")
            .or().haveSimpleNameEndingWith("UseCases")
            .should().resideInAPackage("..domain..")
            .check(classes)
    }

    @Test
    fun `Entities should reside in domain module`() {
        classes().that().areAnnotatedWith("kotlinx.serialization.Serializable")
            .and().resideInAPackage("..domain..")
            .should().resideInAPackage("..domain..entity..")
            .check(classes)
    }

    @Test
    fun `Composables should be in ui packages`() {
        classes().that().haveMethodAnnotatedWith("androidx.compose.runtime.Composable")
            .should().resideInAPackage("..ui..")
            .check(classes)
    }
}