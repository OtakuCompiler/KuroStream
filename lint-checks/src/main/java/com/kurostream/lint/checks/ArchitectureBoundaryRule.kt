package com.kurostream.lint.checks

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.DetektVisitor
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective

class ArchitectureBoundaryRule(config: Config) : Rule(config) {
    
    override val issue: Issue = Issue(
        javaClass.simpleName,
        Severity.Defect,
        "Violates architecture boundaries defined in module dependencies",
        Debt.TWENTY_MIN
    )

    private val moduleBoundaries = ArchitectureDetektRule.MODULE_BOUNDARIES
    private val domainForbiddenImports = ArchitectureDetektRule.DOMAIN_FORBIDDEN_IMPORTS
    private val dataForbiddenImports = ArchitectureDetektRule.DATA_FORBIDDEN_IMPORTS

    override fun visit(root: KtFile) {
        super.visit(root)
        
        val packageName = root.packageFqName?.asString() ?: return
        
        // Check domain layer boundaries
        if (packageName.startsWith(ArchitectureDetektRule.DOMAIN_PACKAGE)) {
            root.importDirectives.forEach { import ->
                checkDomainImport(import, packageName)
            }
        }
        
        // Check data layer boundaries  
        if (packageName.startsWith(ArchitectureDetektRule.DATA_PACKAGE)) {
            root.importDirectives.forEach { import ->
                checkDataImport(import, packageName)
            }
        }
    }

    private fun checkDomainImport(import: KtImportDirective, sourcePackage: String) {
        val importedPackage = import.importedFqName?.asString() ?: return
        
        domainForbiddenImports.forEach { forbidden ->
            if (importedPackage.startsWith(forbidden.removeSuffix("."))) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(import),
                        "Domain layer should not import $forbidden: $importedPackage"
                    )
                )
            }
        }
    }

    private fun checkDataImport(import: KtImportDirective, sourcePackage: String) {
        val importedPackage = import.importedFqName?.asString() ?: return
        
        dataForbiddenImports.forEach { forbidden ->
            if (importedPackage.startsWith(forbidden.removeSuffix("."))) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(import),
                        "Data layer should not import $forbidden: $importedPackage"
                    )
                )
            }
        }
    }
}
