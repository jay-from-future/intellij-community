// "Replace with assignment (original is empty)" "false"
// TOOL: org.jetbrains.kotlin.idea.codeInsight.inspections.shared.SuspiciousCollectionReassignmentInspection
// ACTION: Change type to mutable
// ACTION: Replace overloaded operator with function call
// ACTION: Replace with ordinary assignment
// WITH_STDLIB
fun test(other: Set<Int>) {
    var list = emptyList<Int>()
    foo()
    bar()
    list <caret>+= other
}

fun foo() {}
fun bar() {}