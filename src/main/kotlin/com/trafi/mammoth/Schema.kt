package com.trafi.mammoth

import kotlinx.serialization.Serializable

@Serializable
class Schema(
    val projectId: String,
    val versionNumber: Int,
    val events: List<Event>,
    val types: List<Type>
) {

    @Serializable
    class Event(
        val name: String,
        val description: String,
        val values: List<Value>,
        val parameters: List<Parameter>
    ) {
        companion object {
            const val eventTypeIdentifier = "event_type"
        }

        @Serializable
        class Value(
            val parameter: Parameter,
            val stringValue: String?,
            val integerValue: Int?,
            val stringEnumValue: String?,
            val booleanValue: Boolean?
        )

        @Serializable
        class Parameter(
            val name: String,
            val typeName: String,
            val description: String,
            val publishName: String
        )
    }

    @Serializable
    class Type(
        val name: String,
        val stringEnum: List<String>?
    )

}
