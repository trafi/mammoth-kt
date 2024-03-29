import com.trafi.mammoth.CodeGenerator
import com.trafi.mammoth.Schema
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CodeGeneratorTests {

    private val json = Json

    @Test
    fun `generates function with no parameters`() {
        val schemaJsonString = """
            {
              "projectId": "whitelabel",
              "versionNumber": 1,
              "events": [
                {
                  "id": 0,
                  "name": "SomeScreenOpen",
                  "description": "Some screen was opened",
                  "values": [
                    {
                      "parameter": {
                        "name": "event_type",
                        "typeName": "event_type",
                        "description": "",
                        "publishName": "event_type"
                      },
                      "stringValue": null,
                      "integerValue": null,
                      "booleanValue": null,
                      "stringEnumValue": "screen_open"
                    }
                  ],
                  "parameters": [],
                  "tags": []
                }
              ],
              "types": [
                {
                  "name": "event_type",
                  "stringEnum": [
                    "screen_open",
                    "element_tap"
                  ]
                }
              ]
            }
        """.trimIndent()
        val schema = json.decodeFromString<Schema>(schemaJsonString)

        val code = CodeGenerator.generateCode(
            schema,
            className = "AnalyticsEvent",
            includeSchemaMetadata = true,
        )
        assertEquals(
            """
            // whitelabel schema version 1
            // Generated with https://github.com/trafi/mammoth-kt
            // Do not edit manually.
            package com.trafi.analytics

            import kotlin.String

            private const val mammothSchemaVersion: String = "1"

            public object AnalyticsEvent {
                /**
                 * Some screen was opened
                 */
                public fun someScreenOpen(): Analytics.Event = Analytics.Event(
                    business = RawEvent(
                        name = "SomeScreenOpen",
                        parameters = mapOf(
                            "event_type" to "screen_open",
                            "schema_event_id" to "0",
                            "schema_version" to mammothSchemaVersion,
                        ),
                    ),
                    publish = RawEvent(
                        name = "screen_open",
                        parameters = mapOf(
                            "achievement_id" to "0",
                            "score" to mammothSchemaVersion,
                        ),
                    ),
                    explicitConsumerTags = null,
                )
            }

            public enum class EventType(
                public val `value`: String,
            ) {
                SCREEN_OPEN("screen_open"),
                ELEMENT_TAP("element_tap"),
                ;
            }
            
            """.trimIndent(), code
        )
    }

    @Test
    fun `generates event with explicitConsumerTags`() {
        val schemaJsonString = """
            {
              "projectId": "whitelabel",
              "versionNumber": 1,
              "events": [
                {
                  "id": 0,
                  "name": "SomeScreenOpen",
                  "description": "Some screen was opened",
                  "values": [
                    {
                      "parameter": {
                        "name": "event_type",
                        "typeName": "event_type",
                        "description": "",
                        "publishName": "event_type"
                      },
                      "stringValue": null,
                      "integerValue": null,
                      "booleanValue": null,
                      "stringEnumValue": "screen_open"
                    }
                  ],
                  "parameters": [],
                  "tags": [
                     {
                        "name": "Braze",
                        "class": "Sdk"
                     },
                     {
                        "name": "Batch",
                        "class": "Sdk"
                     }
                  ]
                }
              ],
              "types": [
                {
                  "name": "event_type",
                  "stringEnum": [
                    "screen_open",
                    "element_tap"
                  ]
                }
              ]
            }
        """.trimIndent()
        val schema = json.decodeFromString<Schema>(schemaJsonString)

        val code = CodeGenerator.generateCode(
            schema,
            className = "AnalyticsEvent",
            includeSchemaMetadata = true,
        )
        assertEquals(
            """
            // whitelabel schema version 1
            // Generated with https://github.com/trafi/mammoth-kt
            // Do not edit manually.
            package com.trafi.analytics

            import kotlin.String

            private const val mammothSchemaVersion: String = "1"

            public object AnalyticsEvent {
                /**
                 * Some screen was opened
                 */
                public fun someScreenOpen(): Analytics.Event = Analytics.Event(
                    business = RawEvent(
                        name = "SomeScreenOpen",
                        parameters = mapOf(
                            "event_type" to "screen_open",
                            "schema_event_id" to "0",
                            "schema_version" to mammothSchemaVersion,
                        ),
                    ),
                    publish = RawEvent(
                        name = "screen_open",
                        parameters = mapOf(
                            "achievement_id" to "0",
                            "score" to mammothSchemaVersion,
                        ),
                    ),
                    explicitConsumerTags = listOf(
                        "Braze",
                        "Batch",
                    ),
                )
            }

            public enum class EventType(
                public val `value`: String,
            ) {
                SCREEN_OPEN("screen_open"),
                ELEMENT_TAP("element_tap"),
                ;
            }
            
            """.trimIndent(), code
        )
    }

    @Test
    fun `generates function for bare-bones schema`() {
        val schemaJsonString = """
            {
              "projectId": "some-project",
              "versionNumber": 1,
              "events": [
                {
                  "id": 0,
                  "name": "SomeScreenOpen",
                  "description": "Some screen was opened",
                  "values": [],
                  "parameters": [],
                  "tags": []
                }
              ],
              "types": []
            }
        """.trimIndent()
        val schema = json.decodeFromString<Schema>(schemaJsonString)

        val code = CodeGenerator.generateCode(
            schema,
            className = "AnalyticsEvent",
            includeSchemaMetadata = false,
        )
        assertEquals(
            """
            // some-project schema version 1
            // Generated with https://github.com/trafi/mammoth-kt
            // Do not edit manually.
            package com.trafi.analytics

            public object AnalyticsEvent {
                /**
                 * Some screen was opened
                 */
                public fun someScreenOpen(): Analytics.Event = Analytics.Event(
                    business = RawEvent(
                        name = "SomeScreenOpen",
                        parameters = mapOf(),
                    ),
                    publish = null,
                    explicitConsumerTags = null,
                )
            }
            
            """.trimIndent(), code
        )
    }
}
