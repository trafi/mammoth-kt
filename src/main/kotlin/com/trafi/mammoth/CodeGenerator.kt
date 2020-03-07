package com.trafi.mammoth

import com.squareup.kotlinpoet.*

private const val packageName = "com.trafi.analytics"
private const val enumValuePropertyName = "value"
private const val schemaVersionPropertyName = "mammothSchemaVersion"
private const val schemaVersionPublishName = "score"
private const val eventIdPublishName = "achievement_id"

object CodeGenerator {
    private val analytics = ClassName(packageName, "Analytics")
    private val rawEvent = ClassName(packageName, "RawEvent")
    private val schemaVersion = MemberName(packageName, schemaVersionPropertyName)

    fun generateCode(schema: Schema): String {
        val file = FileSpec.builder(packageName, "AnalyticsEvent")
            .indent("    ")
            .addComment("%L schema version %L\n", schema.projectId, schema.versionNumber)
            .addComment("Generated with https://github.com/trafi/mammoth-kt\nDo not edit manually.")
            .addProperty(
                PropertySpec.builder(schemaVersionPropertyName, String::class, KModifier.PRIVATE, KModifier.CONST)
                    .initializer("%S", schema.versionNumber)
                    .build()
            )
            .addType(
                TypeSpec.objectBuilder("AnalyticsEvent")
                    .apply { schema.events.forEach { addFunction(generateEventFunction(it)) } }
                    .build()
            )
            .apply { schema.types.forEach { generateType(it)?.let { typeSpec -> addType(typeSpec) } } }
            .build()
        return file.toString()
    }

    private fun generateType(type: Schema.Type): TypeSpec? {
        return type.stringEnum?.let { stringEnum ->
            TypeSpec.enumBuilder(type.nativeTypeName)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter(enumValuePropertyName, String::class)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(enumValuePropertyName, String::class)
                        .initializer(enumValuePropertyName).build()
                )
                .apply {
                    stringEnum.forEach { publishName ->
                        addEnumConstant(
                            publishName.toUpperCase(),
                            TypeSpec.anonymousClassBuilder()
                                .addSuperclassConstructorParameter("%S", publishName).build()
                        )
                    }
                }
                .build()
        }
    }

    private fun generateEventFunction(event: Schema.Event): FunSpec {
        return FunSpec.builder(event.nativeFunctionName)
            .addKdoc(event.description)
            .returns(rawEvent)
            .addParameters(event.parameters.map { parameter ->
                ParameterSpec
                    .builder(parameter.nativeParameterName, parameter.nativeTypeName)
                    .addKdoc(parameter.description)
                    .apply {
                        when (parameter.name) {
                            Schema.Event.Parameter.screenNameParameterName,
                            Schema.Event.Parameter.previousScreenNameParameterName -> {
                                defaultValue("%T.screenName", analytics)
                            }
                            Schema.Event.Parameter.modalNameParameterName -> {
                                defaultValue("%T.modalName", analytics)
                            }
                        }
                    }
                    .build()
            }.sortedBy { it.defaultValue != null })
            .addStatement(
                "return %T(\n⇥name = %S,\nparameters = %L⇤\n)",
                rawEvent,
                event.publishName,
                CodeBlock.of(
                    "mapOf(\n⇥%L⇤\n)",
                    event.publishValues.map {
                        CodeBlock.of(
                            "%S to %S",
                            it.first,
                            it.second
                        )
                    }.plus(event.publishParameterExpressions.map {
                        CodeBlock.of(
                            "%S to %L",
                            it.first,
                            it.second
                        )
                    }).plus(
                        CodeBlock.of(
                            "%S to %S",
                            eventIdPublishName,
                            event.id
                        )
                    ).plus(
                        CodeBlock.of(
                            "%S to %M",
                            schemaVersionPublishName,
                            schemaVersion
                        )
                    ).joinToCode(separator = ",\n")
                )
            )
            .build()
    }
}

private val Schema.Event.nativeFunctionName: String get() = name.normalized.decapitalize()
private val Schema.Event.Parameter.nativeParameterName: String get() = name.normalized.decapitalize()
private val Schema.Type.nativeTypeName: String get() = name.normalized

private val Schema.Event.Parameter.nativeTypeName: TypeName
    get() = when (typeName) {
        "String" -> String::class.asTypeName()
        "Integer" -> Int::class.asTypeName()
        "Boolean" -> Boolean::class.asTypeName()
        else -> ClassName(packageName, typeName.normalized)
    }

private val Schema.Event.publishName: String
    get() {
        val eventTypeValue = values.firstOrNull { it.parameter.name == Schema.Event.Parameter.eventTypeParameterName }
            ?: throw IllegalArgumentException("Event does not contain valid ${Schema.Event.Parameter.eventTypeParameterName} value")
        return eventTypeValue.stringEnumValue
            ?: throw IllegalArgumentException("${Schema.Event.Parameter.eventTypeParameterName} must have non-null value")
    }

private val Schema.Event.publishValues: List<Pair<String, String>>
    get() = values.map { it.parameter.publishName to it.publishValue }

private val Schema.Event.Value.publishValue: String
    get() = stringValue ?: integerValue?.toString() ?: stringEnumValue ?: booleanValue?.toString()
    ?: throw IllegalArgumentException("Invalid publish parameter value. Parameter: ${parameter.name}")

private val Schema.Event.publishParameterExpressions: List<Pair<String, String>>
    get() = parameters.map { it.publishName to it.nativeParameterExpression }

private val Schema.Event.Parameter.nativeParameterExpression: String
    get() = when (typeName) {
        "String" -> nativeParameterName
        "Integer", "Boolean" -> "$nativeParameterName.toString()"
        else -> "$nativeParameterName.$enumValuePropertyName"
    }

// remove whitespace, convert snake_case to CamelCase
private val String.normalized: String
    get() = replace(" ", "")
        .split("_").joinToString(separator = "") { it.capitalize() }
