package com.trafi.mammoth

import com.squareup.kotlinpoet.*

private const val packageName = "com.trafi.analytics"
private const val enumValuePropertyName = "value"
private const val schemaVersionPropertyName = "mammothSchemaVersion"
private const val schemaVersionPublishName = "score"

object CodeGenerator {
    private val rawEvent = ClassName(packageName, "RawEvent")
    private val schemaVersion = MemberName(
        packageName,
        schemaVersionPropertyName
    )

    fun generateCode(schema: Schema): String {
        val file = FileSpec.builder(packageName, "AnalyticsEvent")
            .addComment("%L schema version %L\n", schema.projectId, schema.versionNumber)
            .addComment("Generated by mammoth-kt. Do not edit manually.")
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
            TypeSpec.enumBuilder(type.name)
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
            .returns(rawEvent)
            .addParameters(event.parameters.map {
                ParameterSpec.builder(it.nativeParameterName, it.nativeTypeName).build()
            })
            .addStatement(
                "return %T(name = %S, parameters = %L)",
                rawEvent,
                event.publishName,
                CodeBlock.of(
                    "mapOf(%L)",
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
                            "%S to %M",
                            schemaVersionPublishName,
                            schemaVersion
                        )
                    ).joinToCode()
                )
            )
            .build()
    }
}

private val Schema.Event.nativeFunctionName: String get() = name.normalized.decapitalized
private val Schema.Event.Parameter.nativeParameterName: String get() = name.normalized.decapitalized

private val Schema.Event.Parameter.nativeTypeName: TypeName
    get() = when (typeName) {
        "String" -> String::class.asTypeName()
        "Integer" -> Int::class.asTypeName()
        else -> ClassName(packageName, typeName)
    }

private val Schema.Event.publishName: String
    get() {
        val eventTypeValue = values.firstOrNull { it.parameter.name == Schema.Event.eventTypeIdentifier }
            ?: throw IllegalArgumentException("Event does not contain valid EventType value")
        return eventTypeValue.stringEnumValue
            ?: throw IllegalArgumentException("${Schema.Event.eventTypeIdentifier} must have non-null value")
    }

private val Schema.Event.publishValues: List<Pair<String, String>>
    get() = values.map { it.parameter.publishName to it.publishValue }

private val Schema.Event.Value.publishValue: String
    get() = stringValue ?: integerValue?.toString() ?: stringEnumValue
    ?: throw IllegalArgumentException("Invalid publish parameter value. Parameter: ${parameter.name}")

private val Schema.Event.publishParameterExpressions: List<Pair<String, String>>
    get() = parameters.map { it.publishName to it.nativeParameterExpression }

private val Schema.Event.Parameter.nativeParameterExpression: String
    get() = when (typeName) {
        "String" -> nativeParameterName
        "Integer" -> "\$$nativeParameterName"
        else -> "$nativeParameterName.$enumValuePropertyName"
    }

private val String.decapitalized: String get() = decapitalize()
private val String.normalized: String get() = replace(" ", "")
