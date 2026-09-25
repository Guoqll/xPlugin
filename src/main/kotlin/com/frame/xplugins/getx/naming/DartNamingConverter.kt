package com.frame.xplugins.getx.naming

data class DartName(
    val snakeCase: String,
    val pascalCase: String,
)

object DartNamingConverter {

    private val acronymBoundary = Regex("([A-Z]+)([A-Z][a-z])")
    private val camelBoundary = Regex("([a-z0-9])([A-Z])")
    private val separators = Regex("[\\s_-]+")
    private val validIdentifier = Regex("[a-z][a-z0-9_]*")

    fun routeName(input: String): DartName {
        val snakeCase = normalizeIdentifier(input, allowEmpty = false)
        return DartName(
            snakeCase = snakeCase,
            pascalCase = snakeCase.split('_').joinToString("") { part ->
                part.replaceFirstChar { it.uppercaseChar() }
            },
        )
    }

    fun prefix(input: String): String = normalizeIdentifier(
        input = input.trim().trim('_'),
        allowEmpty = true,
    )

    fun prefixedFileStem(prefix: String, name: String): String =
        listOf(prefix(prefix), name).filter { it.isNotEmpty() }.joinToString("_")

    private fun normalizeIdentifier(input: String, allowEmpty: Boolean): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            require(allowEmpty) { "Name cannot be empty" }
            return ""
        }
        require(trimmed.all { it.isLetterOrDigit() || it == '_' || it == '-' || it.isWhitespace() }) {
            "Only letters, numbers, spaces, underscores, and hyphens are supported"
        }
        require(trimmed.all { it.code < 128 }) { "Only ASCII names are supported in V1" }

        val normalized = trimmed
            .replace(acronymBoundary, "$1_$2")
            .replace(camelBoundary, "$1_$2")
            .replace(separators, "_")
            .trim('_')
            .lowercase()

        require(normalized.isNotEmpty() || allowEmpty) { "Name cannot be empty after normalization" }
        require(normalized.isEmpty() || validIdentifier.matches(normalized)) {
            "Name must start with a letter and contain only letters, numbers, or underscores"
        }
        return normalized
    }
}
