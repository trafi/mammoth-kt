package com.trafi.mammoth

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode
import java.util.Locale

private const val packageName = "com.trafi.analytics"
private const val enumValuePropertyName = "value"
private const val schemaVersionPropertyName = "mammothSchemaVersion"
private const val schemaVersionPublishName = "score"
private const val schemaVersionBusinessName = "schema_version"
private const val eventIdPublishName = "achievement_id"
private const val eventIdBusinessName = "schema_event_id"
private val locale = Locale.US

object CodeGenerator {
    private val analyticsObject = ClassName(packageName, "Analytics")
    private val eventClass = ClassName(packageName, "Analytics", "Event")
    private val rawEventClass = ClassName(packageName, "RawEvent")
    private val schemaVersion = MemberName(packageName, schemaVersionPropertyName)

    fun generateCode(schema: Schema, className: String): String {
        val file = FileSpec.builder(packageName, className)
            .indent("    ")
            .addFileComment("%L schema version %L\n", schema.projectId, schema.versionNumber)
            .addFileComment("Generated with https://github.com/trafi/mammoth-kt\nDo not edit manually.")
            .addProperty(
                PropertySpec
                    .builder(
                        schemaVersionPropertyName,
                        String::class,
                        KModifier.PRIVATE,
                        KModifier.CONST
                    )
                    .initializer("%S", schema.versionNumber)
                    .build()
            )
            .addType(
                TypeSpec.objectBuilder(className)
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
                            publishName.uppercase(locale),
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
            .returns(eventClass)
            .addParameters(event.parameters.map { parameter ->
                ParameterSpec
                    .builder(parameter.nativeParameterName, parameter.nativeTypeName)
                    .addKdoc(parameter.description)
                    .apply {
                        when (parameter.name) {
                            Schema.Event.Parameter.screenNameParameterName,
                            Schema.Event.Parameter.previousScreenNameParameterName -> {
                                defaultValue("%T.screenName", analyticsObject)
                            }
                            Schema.Event.Parameter.modalNameParameterName -> {
                                defaultValue("%T.modalName", analyticsObject)
                            }
                        }
                    }
                    .build()
            }.sortedBy { it.defaultValue != null })
            .addStatement(
                "return %T(\n⇥business = %L,\npublish = %L,\nexplicitConsumerTags = %L⇤\n)",
                eventClass,
                generateBusinessEvent(event),
                generatePublishEvent(event) ?: "null",
                generateSdkTags(event) ?: "null"
            )
            .build()
    }

    private fun generateSdkTags(event: Schema.Event): CodeBlock? {
        val sdkTags = event.tags.filter {
            it.clazz.contains(other = "Sdk", ignoreCase = true)
        }
        return CodeBlock.of(
            "listOf(\n⇥%L⇤\n)",
            sdkTags.joinToString(separator = ",\n") { "\"${it.name}\"" }
        ).takeIf { sdkTags.isNotEmpty() }
    }

    private fun generatePublishEvent(event: Schema.Event): CodeBlock? {
        val publishName = event.publishName ?: return null
        return generateRawEvent(
            name = publishName,
            parameterCodeBlocks = event.publishValues.map {
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
            }).plus(event.publishMetadataParameters)
        )
    }

    private fun generateBusinessEvent(event: Schema.Event): CodeBlock {
        return generateRawEvent(
            name = event.name,
            parameterCodeBlocks = event.businessValues.map {
                CodeBlock.of(
                    "%S to %S",
                    it.first,
                    it.second
                )
            }.plus(event.businessParameterExpressions.map {
                CodeBlock.of(
                    "%S to %L",
                    it.first,
                    it.second
                )
            }).plus(event.businessMetadataParameters)
        )
    }

    private fun generateRawEvent(name: String, parameterCodeBlocks: List<CodeBlock>): CodeBlock {
        return CodeBlock.of(
            "%T(\n⇥name = %S,\nparameters = %L⇤\n)",
            rawEventClass,
            name,
            CodeBlock.of(
                "mapOf(\n⇥%L⇤\n)",
                parameterCodeBlocks.joinToCode(separator = ",\n")
            )
        )
    }

    private val Schema.Event.publishMetadataParameters: List<CodeBlock>
        get() = listOf(
            CodeBlock.of(
                "%S to %S",
                eventIdPublishName,
                id
            ),
            CodeBlock.of(
                "%S to %M",
                schemaVersionPublishName,
                schemaVersion
            )
        )

    private val Schema.Event.businessMetadataParameters: List<CodeBlock>
        get() = listOf(
            CodeBlock.of(
                "%S to %S",
                eventIdBusinessName,
                id
            ),
            CodeBlock.of(
                "%S to %M",
                schemaVersionBusinessName,
                schemaVersion
            )
        )
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

private val Schema.Event.publishName: String?
    get() {
        val eventTypeValue =
            values.firstOrNull { it.parameter.name == Schema.Event.Parameter.eventTypeParameterName }
                ?: return null
        return eventTypeValue.stringEnumValue
            ?: throw IllegalArgumentException("${Schema.Event.Parameter.eventTypeParameterName} must have non-null value")
    }

private val Schema.Event.publishValues: List<Pair<String, String>>
    get() = values
        .filterNot { it.parameter.name == Schema.Event.Parameter.eventTypeParameterName }
        .map { it.parameter.publishName to it.publishValue }

private val Schema.Event.businessValues: List<Pair<String, String>>
    get() = values.map { it.parameter.name to it.publishValue }

private val Schema.Event.Value.publishValue: String
    get() = stringValue ?: integerValue?.toString() ?: stringEnumValue ?: booleanValue?.toString()
    ?: throw IllegalArgumentException("Invalid publish parameter value. Parameter: ${parameter.name}")

private val Schema.Event.publishParameterExpressions: List<Pair<String, String>>
    get() = parameters.map { it.publishName to it.nativeParameterExpression }

private val Schema.Event.businessParameterExpressions: List<Pair<String, String>>
    get() = parameters.map { it.name to it.nativeParameterExpression }

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

private fun String.capitalize(): String =
    replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(locale) else c.toString() }

private fun String.decapitalize(): String = replaceFirstChar { it.lowercase(locale) }
