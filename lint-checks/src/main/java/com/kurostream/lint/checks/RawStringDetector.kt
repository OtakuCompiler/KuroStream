package com.kurostream.lint.checks

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UStringTemplateExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

class RawStringDetector : Detector(), SourceCodeScanner {
    
    companion object {
        val ISSUE = Issue.create(
            id = "RawStringInCode",
            briefDescription = "Raw string literals should be replaced with string resources",
            explanation = "Hardcoded strings in code make localization difficult. Use string resources instead.",
            category = Category.INTERNATIONALIZATION,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                RawStringDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableIssues() = listOf(ISSUE)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                // Check for string literals in method calls
                node.valueArguments.forEach { arg ->
                    if (arg is UStringTemplateExpression && arg.isPureString()) {
                        val stringValue = arg.expression?.text?.removeSurrounding("\"")
                        if (stringValue != null && stringValue.length > 3 && !isAllowedString(stringValue)) {
                            context.report(
                                ISSUE,
                                arg,
                                context.getLocation(arg),
                                "Raw string literal found: \"$stringValue\"",
                                fix().replace().text("R.string.${stringValue.toValidResourceName()}").build()
                            )
                        }
                    }
                }
                super.visitCallExpression(node)
            }
        }
    }

    private fun isAllowedString(string: String): Boolean {
        // Allow certain strings that are not user-facing
        val allowedPatterns = listOf(
            "^https?://",
            "^ftp://",
            "^file://",
            "^\\$", // Regex patterns
            "^[a-zA-Z_][a-zA-Z0-9_]*$", // Single words (likely constants)
            "^[0-9]+$", // Numbers
            "^[
	]+$", // Whitespace
            "^$" // Empty
        )
        return allowedPatterns.any { string.matches(Regex(it)) }
    }

    private fun String.toValidResourceName(): String {
        return this
            .replace(Regex("[^a-zA-Z0-9_]"), "_")
            .replace(Regex("^_+"), "")
            .replace(Regex("_+$"), "")
            .replace(Regex("_+"), "_")
            .lowercase()
            .takeIf { it.isNotEmpty() } ?: "string"
    }

    override fun applicableSuperClasses() = listOf("android.app.Activity", "androidx.appcompat.app.AppCompatActivity")
}
