package com.kurostream.plugin.sdk.manifest

import com.kurostream.domain.entity.ExtensionCapability
import com.kurostream.domain.entity.ExtensionInfo
import com.kurostream.domain.entity.SemanticVersion
import com.kurostream.domain.result.Result
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream

class ExtensionManifestValidator {
    private val json: Json = Json { ignoreUnknownKeys = false }
    private val jacksonMapper = ObjectMapper()
    private val schema: JsonSchema
    private val currentAppVersion: SemanticVersion = SemanticVersion(2, 0, 0)
    private val maxExtensionSize: Long = 10 * 1024 * 1024 // 10MB
    private val supportedCapabilities = ExtensionCapability.entries.map { it.name }.toSet()

    init {
        val schemaStream: InputStream = this::class.java.classLoader?.getResourceAsStream(
            "extension-manifest-schema.json"
        ) ?: throw IllegalStateException("Extension manifest schema not found")

        val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
        schema = schemaFactory.getSchema(schemaStream)
    }

    fun validateManifest(manifestJson: String, fileSize: Long? = null): Result<ExtensionInfo> {
        return try {
            fileSize?.let {
                if (it > maxExtensionSize) {
                    return Result.error(IllegalArgumentException(
                        "Extension size exceeds maximum allowed size of ${maxExtensionSize / (1024 * 1024)}MB"
                    ))
                }
            }

            val jsonElement = try {
                json.parseToJsonElement(manifestJson)
            } catch (e: Exception) {
                return Result.error(IllegalArgumentException("Invalid JSON format: ${e.message}"))
            }

            if (jsonElement !is JsonObject) {
                return Result.error(IllegalArgumentException("Manifest must be a JSON object"))
            }

            val jacksonNode: JsonNode = jacksonMapper.readTree(jsonElement.toString())
            val validationMessages = schema.validate(jacksonNode)
            if (validationMessages.isNotEmpty()) {
                val errorMessage = buildValidationErrorMessage(validationMessages)
                return Result.error(IllegalArgumentException(errorMessage))
            }

            val extensionInfo = convertToExtensionInfo(jsonElement)
            validateVersionCompatibility(extensionInfo)
            Result.success(extensionInfo)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    private fun validateVersionCompatibility(extensionInfo: ExtensionInfo) {
        if (extensionInfo.minAppVersion > currentAppVersion) {
            throw IllegalArgumentException(
                "Extension requires app version ${extensionInfo.minAppVersion} but current version is $currentAppVersion"
            )
        }
        if (extensionInfo.targetAppVersion != null) {
            val targetVer = extensionInfo.targetAppVersion!!
            if (targetVer < currentAppVersion) {
                throw IllegalArgumentException(
                    "Extension is not compatible with current app version $currentAppVersion"
                )
            }
        }
    }

    private fun buildValidationErrorMessage(messages: Set<ValidationMessage>): String {
        return "Manifest validation failed:\n" + messages.joinToString("\n") { m: ValidationMessage ->
            "- ${m.message}"
        }
    }

    private fun convertToExtensionInfo(jsonObject: JsonObject): ExtensionInfo {
        validateRequiredField(jsonObject, "id")
        validateRequiredField(jsonObject, "name")
        validateRequiredField(jsonObject, "version")
        validateRequiredField(jsonObject, "author")
        validateRequiredField(jsonObject, "pluginClassName")
        validateRequiredField(jsonObject, "apiVersion")
        validateRequiredField(jsonObject, "capabilities")

        val capabilities = parseCapabilities(jsonObject["capabilities"]?.jsonArray)
        val version = parseSemanticVersion(
            jsonObject["version"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing required field: version")
        )
        val minAppVersion = jsonObject["minAppVersion"]?.jsonPrimitive?.content?.let {
            parseSemanticVersion(it)
        } ?: SemanticVersion(1, 0, 0)
        val targetAppVersion = jsonObject["maxAppVersion"]?.jsonPrimitive?.content?.let {
            parseSemanticVersion(it)
        }

        return ExtensionInfo(
            id = jsonObject["id"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing required field: id"),
            name = jsonObject["name"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing required field: name"),
            author = jsonObject["author"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing required field: author"),
            version = version,
            description = jsonObject["description"]?.jsonPrimitive?.contentOrNull,
            iconUrl = jsonObject["iconUrl"]?.jsonPrimitive?.contentOrNull,
            packageName = jsonObject["pluginClassName"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing required field: pluginClassName"),
            capabilities = capabilities,
            minAppVersion = minAppVersion,
            targetAppVersion = targetAppVersion,
            isTrusted = false
        )
    }

    private fun validateRequiredField(jsonObject: JsonObject, fieldName: String) {
        if (!jsonObject.containsKey(fieldName)) {
            throw IllegalArgumentException("Missing required field: $fieldName")
        }
    }

    private fun parseCapabilities(capabilitiesArray: JsonArray?): Set<ExtensionCapability> {
        if (capabilitiesArray == null || capabilitiesArray.isEmpty()) {
            throw IllegalArgumentException("Capabilities array must contain at least one capability")
        }
        val capabilities = mutableSetOf<ExtensionCapability>()
        val invalidCapabilities = mutableListOf<String>()
        for (capability in capabilitiesArray) {
            val capabilityName = capability.jsonPrimitive.content
            try {
                if (supportedCapabilities.contains(capabilityName)) {
                    capabilities.add(ExtensionCapability.valueOf(capabilityName))
                } else {
                    invalidCapabilities.add(capabilityName)
                }
            } catch (e: IllegalArgumentException) {
                invalidCapabilities.add(capabilityName)
            }
        }
        if (invalidCapabilities.isNotEmpty()) {
            throw IllegalArgumentException(
                "Invalid capabilities: ${invalidCapabilities.joinToString()}. " +
                "Supported: ${supportedCapabilities.joinToString()}"
            )
        }
        return capabilities
    }

    private fun parseSemanticVersion(versionString: String): SemanticVersion {
        val versionRegex = "^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([a-zA-Z0-9]+))?$".toRegex()
        val matchResult = versionRegex.matchEntire(versionString)
            ?: throw IllegalArgumentException("Version must be in format major.minor.patch (e.g., 1.2.3), got: $versionString")
        val (majorStr, minorStr, patchStr) = matchResult.destructured
        return SemanticVersion(
            major = majorStr.toIntOrNull() ?: throw IllegalArgumentException("Major version must be a number, got: $majorStr"),
            minor = minorStr.toIntOrNull() ?: throw IllegalArgumentException("Minor version must be a number, got: $minorStr"),
            patch = patchStr.toIntOrNull() ?: throw IllegalArgumentException("Patch version must be a number, got: $patchStr")
        )
    }
}