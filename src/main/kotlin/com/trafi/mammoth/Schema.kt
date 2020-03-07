package com.trafi.mammoth

import kotlinx.serialization.SerialName
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
        val id: Long,
        val name: String,
        val description: String,
        val values: List<Value>,
        val parameters: List<Parameter>,
        val tags: List<Tag>
    ) {

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
        ) {
            companion object {
                const val eventTypeParameterName = "event_type"
                const val screenNameParameterName = "screen_name"
                const val previousScreenNameParameterName = "previous_screen_name"
                const val modalNameParameterName = "modal_name"
            }
        }

        @Serializable
        class Tag(
            val name: String,
            @SerialName("class") val clazz: String
        )
    }

    @Serializable
    class Type(
        val name: String,
        val stringEnum: List<String>?
    )

}
