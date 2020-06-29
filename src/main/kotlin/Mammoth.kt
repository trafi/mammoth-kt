import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.trafi.mammoth.CodeGenerator
import com.trafi.mammoth.Schema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.SocketTimeoutException

class Mammoth : CliktCommand() {

    private val project: String by option(help = "Mammoth project id").default("whitelabel")
    private val version: String by option(help = "Mammoth schema version").default("")
    private val outputPath: String by option(help = "Generated schema code output path").default("")
    private val outputFilename: String by option(help = "Generated schema code output file name").default("MammothEvents.kt")

    override fun run() {
        try {
            val url = "https://mammoth.trafi.com/$project/schema/$version"
            echo(if (business) "Downloading schema from $url" else "Hunting for mammoths at $url")
            val schemaJsonString =
                OkHttpClient().newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (response.code != 200) {
                        throw IOException("${response.code} ${response.message}\n${response.body?.string().orEmpty()}")
                    }
                    response.body?.string() ?: throw IOException("${response.code} ${response.message} with empty body")
                }

            echo(if (business) "Parsing schema" else "Grooming mammoth")
            val json = Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true))
            val schema = json.parse(Schema.serializer(), schemaJsonString)

            echo(if (business) "Generating code" else "Cooking kotlet")
            val code = CodeGenerator.generateCode(schema)

            val file = File(outputPath).resolve(outputFilename)
            echo(if (business) "Writing generated code to $file" else "Serving hot kotlet at $file")
            FileWriter(file).use { it.write(code) }

            echo(if (business) "Success" else "Victory")
        } catch (e: Exception) {
            echo(if (business) "Error: $e" else "Oops, something went wrong: $e")
            if (e is SocketTimeoutException) {
                echo("Please make sure you are connected to the Trafi VPN")
            }
        }
    }
}

fun main(args: Array<String>) = Mammoth().main(args)

/**
 * Real business or just casual.
 */
private val business get() = Math.random() < 0.9
