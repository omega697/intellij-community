// "Replace with 'p'" "true"
@Deprecated("", ReplaceWith("p"))
fun oldFun(p: Int): Int = p

fun foo() {
    val v = <caret>oldFun(0)
}

// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix