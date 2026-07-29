package com.kurostream.plugin.sdk.manifest

import com.kurostream.domain.entity.ExtensionCapability
import com.kurostream.domain.entity.SemanticVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExtensionManifestValidatorTest {
    private lateinit var validator: ExtensionManifestValidator

    @Before
    fun setUp() {
        validator = ExtensionManifestValidator()
    }

    @Test
    fun `validateManifest with valid manifest should return Success`() {
        val validManifest = """{
            "id": "com.example.animecatalog",
            "name": "Anime Catalog",
            "version": 1,
            "versionName": "1.0.0",
            "author": "Example Developer",
            "pluginClassName": "com.example.animecatalog.AnimePlugin",
            "apiVersion": 1,
            "capabilities": ["CATALOG_BROWSE", "VIDEO_SOURCE"],
            "minAppVersion": "1.0.0"
        }"""

        val result = validator.validateManifest(validManifest)
        
        assertTrue("Expected success result", result.isSuccess)
        val extensionInfo = result.getOrThrow()
        assertEquals("com.example.animecatalog", extensionInfo.id)
        assertEquals("Anime Catalog", extensionInfo.name)
        assertEquals(SemanticVersion(1, 0, 0), extensionInfo.version)
        assertEquals(setOf(ExtensionCapability.CATALOG_BROWSE, ExtensionCapability.VIDEO_SOURCE), extensionInfo.capabilities)
    }

    @Test
    fun `validateManifest with missing required field should return Error`() {
        val invalidManifest = """{
            "name": "Anime Catalog",
            "version": 1,
            "versionName": "1.0.0",
            "author": "Example Developer",
            "pluginClassName": "com.example.animecatalog.AnimePlugin",
            "apiVersion": 1,
            "capabilities": ["CATALOG_BROWSE"]
        }"""

        val result = validator.validateManifest(invalidManifest)
        
        assertTrue("Expected error result", result.isFailure)
        assertTrue(
            "Expected missing field error",
            result.exceptionOrNull()?.message?.contains("Missing required field: id") == true
        )
    }

    @Test
    fun `validateManifest with invalid capability should return Error`() {
        val invalidManifest = """{
            "id": "com.example.animecatalog",
            "name": "Anime Catalog",
            "version": 1,
            "versionName": "1.0.0",
            "author": "Example Developer",
            "pluginClassName": "com.example.animecatalog.AnimePlugin",
            "apiVersion": 1,
            "capabilities": ["INVALID_CAPABILITY"]
        }"""

        val result = validator.validateManifest(invalidManifest)
        
        assertTrue("Expected error result", result.isFailure)
        assertTrue(
            "Expected invalid capability error",
            result.exceptionOrNull()?.message?.contains("Invalid capabilities: INVALID_CAPABILITY") == true
        )
    }

    @Test
    fun `validateManifest with invalid version format should return Error`() {
        val invalidManifest = """{
            "id": "com.example.animecatalog",
            "name": "Anime Catalog",
            "version": 1,
            "versionName": "invalid.version.format",
            "author": "Example Developer",
            "pluginClassName": "com.example.animecatalog.AnimePlugin",
            "apiVersion": 1,
            "capabilities": ["CATALOG_BROWSE"]
        }"""

        val result = validator.validateManifest(invalidManifest)
        
        assertTrue("Expected error result", result.isFailure)
        assertTrue(
            "Expected version format error",
            result.exceptionOrNull()?.message?.contains("Version must be in format major.minor.patch") == true
        )
    }

    @Test
    fun `validateManifest with version compatibility issue should return Error`() {
        val invalidManifest = """{
            "id": "com.example.animecatalog",
            "name": "Anime Catalog",
            "version": 1,
            "versionName": "1.0.0",
            "author": "Example Developer",
            "pluginClassName": "com.example.animecatalog.AnimePlugin",
            "apiVersion": 1,
            "capabilities": ["CATALOG_BROWSE"],
            "minAppVersion": "3.0.0"
        }"""

        val result = validator.validateManifest(invalidManifest)
        
        assertTrue("Expected error result", result.isFailure)
        assertTrue(
            "Expected version compatibility error",
            result.exceptionOrNull()?.message?.contains("Extension requires app version 3.0.0") == true
        )
    }

    @Test
    fun `validateManifest with invalid JSON should return Error`() {
        val invalidJson = """{
            "id": "com.example.animecatalog",
            "name": "Anime Catalog",
            invalid json
        }"""

        val result = validator.validateManifest(invalidJson)
        
        assertTrue("Expected error result", result.isFailure)
        assertTrue(
            "Expected JSON format error",
            result.exceptionOrNull()?.message?.contains("Invalid JSON format") == true
        )
    }

    @Test
    fun `validateManifest with invalid ID format should return Error`() {
        val invalidManifest = """{
            "id": "invalid-id",
            "name": "Anime Catalog",
            "version": 1,
            "versionName": "1.0.0",
            "author": "Example Developer",
            "pluginClassName": "com.example.animecatalog.AnimePlugin",
            "apiVersion": 1,
            "capabilities": ["CATALOG_BROWSE"]
        }"""

        val result = validator.validateManifest(invalidManifest)
        
        assertTrue("Expected error result", result.isFailure)
        // The schema validation should catch this
        assertTrue(
            "Expected ID format error",
            result.exceptionOrNull()?.message?.contains("id") == true
        )
    }

    @Test
    fun `validateManifest with all optional fields should return Success`() {
        val completeManifest = """{
            "id": "com.example.animecatalog",
            "name": "Anime Catalog",
            "version": 1,
            "versionName": "1.0.0",
            "author": "Example Developer",
            "description": "A comprehensive anime catalog extension",
            "pluginClassName": "com.example.animecatalog.AnimePlugin",
            "apiVersion": 1,
            "minAppVersion": "1.0.0",
            "maxAppVersion": "2.5.0",
            "capabilities": ["CATALOG_BROWSE", "VIDEO_SOURCE"],
            "language": "en-US",
            "tvTypes": ["anime", "movie"],
            "requiresResources": true,
            "iconUrl": "https://example.com/icon.png",
            "screenshots": [
                "https://example.com/screenshot1.png",
                "https://example.com/screenshot2.png"
            ]
        }"""

        val result = validator.validateManifest(completeManifest)
        
        assertTrue("Expected success result", result.isSuccess)
        val extensionInfo = result.getOrThrow()
        assertEquals("com.example.animecatalog", extensionInfo.id)
        assertEquals("A comprehensive anime catalog extension", extensionInfo.description)
        assertEquals(SemanticVersion(1, 0, 0), extensionInfo.minAppVersion)
        assertEquals(SemanticVersion(2, 5, 0), extensionInfo.targetAppVersion)
    }
}