import com.trafi.mammoth.CodeGenerator
import com.trafi.mammoth.Schema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CodeGeneratorTests {

    private val json = Json(JsonConfiguration.Stable)

    @Test
    fun `generates function with no parameters`() {
        val schemaJsonString = """
            {
              "projectId": "whitelabel",
              "versionNumber": 1,
              "events": [
                {
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
                  "parameters": []
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
        val schema = json.parse(Schema.serializer(), schemaJsonString)

        val code = CodeGenerator.generateCode(schema)
        assertEquals(
            """
            // whitelabel schema version 1
            // Generated with https://github.com/trafi/mammoth-kt
            // Do not edit manually.
            package com.trafi.analytics

            import kotlin.String

            private const val mammothSchemaVersion: String = "1"

            object AnalyticsEvent {
                /**
                 * Some screen was opened
                 */
                fun someScreenOpen(): RawEvent = RawEvent(
                    name = "screen_open",
                    parameters = mapOf(
                        "event_type" to "screen_open",
                        "score" to mammothSchemaVersion
                    )
                )
            }

            enum class EventType(
                val value: String
            ) {
                SCREEN_OPEN("screen_open"),

                ELEMENT_TAP("element_tap");
            }
            
            """.trimIndent(), code
        )
    }

}
