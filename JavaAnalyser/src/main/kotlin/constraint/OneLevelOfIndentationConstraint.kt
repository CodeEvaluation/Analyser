package constraint

import JavaFile
import analyser.Conformant
import analyser.Constraint
import analyser.ConstraintEvaluation
import analyser.Violation
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.util.stream.Collectors.joining


class OneLevelOfIndentationConstraint : Constraint {
    override fun evaluate(javaFile: JavaFile): ConstraintEvaluation {
        if (hasMoreThanOneLevelOfIndentation(javaFile)) {
            return Violation(
                javaFile.className(),
                "More that one level of indentation."
            )
        }

        return Conformant()
    }

    private fun hasMoreThanOneLevelOfIndentation(javaFile: JavaFile): Boolean {
        val allBraces: String = extractSemanticBlocksOf(javaFile)
        var nestingLevel = 0
        for (i in allBraces.indices) {
            if (allBraces[i] == '{') nestingLevel++
            if (allBraces[i] == '}') nestingLevel--
            if (nestingLevel > 3) return true
        }
        return hasMoreThanOneLevelOfIndentationInBlocksWithoutBraces(javaFile)
    }

    private fun extractSemanticBlocksOf(javaFile: JavaFile): String {
        val fileContent = javaFile.fileContent()
        val isCommentedLine: (String) -> Boolean = { line ->
            val trimmedLine = line.trim()
            listOf(
                "//",
                "*",
                "/*"
            ).none { trimmedLine.startsWith(it) } && !trimmedLine.endsWith("*/")
        }
        val fileContentWithoutComments =
            fileContent.lines().filter(isCommentedLine).stream()
                .collect(joining())
        return fileContentWithoutComments.filter { it in "{}" }
    }

    private fun hasMoreThanOneLevelOfIndentationInBlocksWithoutBraces(javaFile: JavaFile): Boolean {
        val classByName: ClassOrInterfaceDeclaration = javaFile.parse()
        val methodContainsNestedBlocks: (MethodDeclaration) -> Boolean = {
            val nestedBlocksVisitor = NestedBlocksVisitor()
            it.accept(nestedBlocksVisitor, null)
            nestedBlocksVisitor.foundNestedBlock
        }
        return classByName.methods.any(methodContainsNestedBlocks)
    }

    /**
     * This class is a visitor which is used to assess whether a given method contains more than one level of
     * indentation. It visits the parsed nodes in order. The 'super' call in these methods involves descending further
     * into the syntax tree, which means greater levels of nesting. This is detected with a simple counter, and any
     * excessive nesting is recorded by a one-way assignment to the 'foundNestedBlock' field.
     */
    private class NestedBlocksVisitor : VoidVisitorAdapter<Void>() {
        private var indentationLevel = 0

        var foundNestedBlock = false

        private fun increaseIndentationLevel() {
            indentationLevel++
            if (indentationLevel > 1) {
                foundNestedBlock = true
            }
        }

        private fun decreaseIndentationLevel() {
            indentationLevel--
        }

        override fun visit(n: TryStmt?, arg: Void?) {
            increaseIndentationLevel()
            super.visit(n, arg)
            decreaseIndentationLevel()
        }

        override fun visit(n: CatchClause?, arg: Void?) {
            increaseIndentationLevel()
            super.visit(n, arg)
            decreaseIndentationLevel()
        }

        override fun visit(n: IfStmt?, arg: Void?) {
            increaseIndentationLevel()
            super.visit(n, arg)
            decreaseIndentationLevel()
        }

        override fun visit(n: ForStmt?, arg: Void?) {
            increaseIndentationLevel()
            super.visit(n, arg)
            decreaseIndentationLevel()
        }

        override fun visit(n: ForEachStmt?, arg: Void?) {
            increaseIndentationLevel()
            super.visit(n, arg)
            decreaseIndentationLevel()
        }

        override fun visit(n: DoStmt?, arg: Void?) {
            increaseIndentationLevel()
            super.visit(n, arg)
            decreaseIndentationLevel()
        }

        override fun visit(n: WhileStmt?, arg: Void?) {
            increaseIndentationLevel()
            super.visit(n, arg)
            decreaseIndentationLevel()
        }
    }
}

