// FIR_IDENTICAL
interface I<T> {
    fun <U> a(t: T, u: U)
    fun b()
    fun c(t: T)
    fun <U> d(t: T, u: U)
}

class <caret>C : I<Int> {
    override fun b() {
    }

    override fun <U> d(t: Int, u: U) {
    }
}

// MEMBER_K2: "<U> a(t: Int, u: U): Unit"
// MEMBER_K1: "a(t: Int, u: U): Unit"
// MEMBER: "c(t: Int): Unit"