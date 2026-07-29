package org.tensorflow.lite

import java.nio.ByteBuffer

class Interpreter {
    class Options {
        private var numThreads = 1

        fun setNumThreads(n: Int): Options {
            numThreads = n
            return this
        }
    }

    constructor(model: ByteBuffer, options: Options? = null)

    fun run(input: Any, output: Any) { }

    fun close() { }
}
