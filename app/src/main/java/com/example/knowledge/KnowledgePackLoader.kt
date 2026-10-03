package com.example.knowledge

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

object KnowledgePackLoader {

    private const val EXPECTED_SCHEMA_VERSION = "1.0"
    private const val ASSET_FILE_NAME = "chartmind_knowledge_pack.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Volatile
    private var cachedPack: KnowledgePack? = null

    /**
     * Loads and parses the Knowledge Pack from app assets with schema version check.
     */
    fun loadKnowledgePack(context: Context): KnowledgePack {
        cachedPack?.let { return it }

        synchronized(this) {
            cachedPack?.let { return it }

            val jsonContent = context.assets.open(ASSET_FILE_NAME).use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).readText()
            }

            val pack = json.decodeFromString<KnowledgePack>(jsonContent)

            if (pack.schemaVersion != EXPECTED_SCHEMA_VERSION) {
                throw IllegalStateException(
                    "Unsupported Knowledge Pack schema_version: '${pack.schemaVersion}'. Expected: '$EXPECTED_SCHEMA_VERSION'"
                )
            }

            cachedPack = pack
            return pack
        }
    }

    /**
     * Directly parses raw JSON string for dynamic import / testing.
     */
    fun parseJson(jsonString: String): KnowledgePack {
        val pack = json.decodeFromString<KnowledgePack>(jsonString)
        if (pack.schemaVersion != EXPECTED_SCHEMA_VERSION) {
            throw IllegalStateException(
                "Unsupported Knowledge Pack schema_version: '${pack.schemaVersion}'. Expected: '$EXPECTED_SCHEMA_VERSION'"
            )
        }
        return pack
    }
}
