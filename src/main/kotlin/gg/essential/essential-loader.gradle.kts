package gg.essential

import gg.essential.gradle.multiversion.Platform
import gg.essential.gradle.util.RelocationTransform.Companion.registerRelocationAttribute


plugins {
    id("gg.essential.loom")
}

val platform = Platform.of(project)

val essentialLoader: Configuration by configurations.creating

when {
    platform.isLegacyForge -> {
        dependencies {
            "implementation"(essentialLoader("gg.essential:loader-launchwrapper:1.1.3")!!)
        }
        tasks.named<Jar>("jar") {
            dependsOn(essentialLoader)
            manifest.attributes(mapOf(
                "FMLCorePluginContainsMod" to "Yes, yes it does",
                "TweakClass" to "gg.essential.loader.stage0.EssentialSetupTweaker"
            ))
            from({ zipTree(essentialLoader.singleFile) })
        }
    }

    platform.isFabric -> {
        dependencies {
            "include"("modRuntimeOnly"("gg.essential:loader-fabric:1.0.0")!!)
        }
    }

    platform.isModLauncher -> {
        dependencies {
            val relocatedPackage = findProperty("essential.loader.package")?.toString() ?: throw GradleException("""
                A package for the Essential loader to be relocated to has not been set.
                You need to set `essential.loader.package` in the project's `gradle.properties` file to a specific package where Essential's loader will be relocated to.
                The recommended package is an unused package within your mod's package.
            """.trimIndent())
            val relocationAttribute =
                registerRelocationAttribute("essential-loader-relocated") {
                    relocate("gg.essential.loader.stage0", "$relocatedPackage.stage0")
                    // preserve stage1 path
                    rename("gg/essential/loader/stage0/stage1.jar", "gg/essential/loader/stage0/stage1.jar")
                }
            essentialLoader.attributes {
                attribute(relocationAttribute, true)
            }
            if (platform.mcVersion < 11700) {
                "forgeRuntimeLibrary"(essentialLoader("gg.essential:loader-modlauncher8:1.0.0")!!)
            } else {
                "forgeRuntimeLibrary"(essentialLoader("gg.essential:loader-modlauncher9:1.0.0")!!)
            }
        }
        tasks.named<Jar>("jar") {
            dependsOn(essentialLoader)
            from({ zipTree(essentialLoader.singleFile) })
        }
    }

    else -> error("No loader available for this platform")
}

// Unit statement so it doesn't try to get the result from the when block
Unit