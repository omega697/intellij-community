// "Change type arguments to <*>" "true"
fun test(a: Any) {
    foo(a as List<Boolean><caret>)
}

fun <T> foo(list: List<T>) {}
// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.ChangeToStarProjectionFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.ChangeToStarProjectionFix