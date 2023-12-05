import com.swmansion.starknet.data.types.NumAsHex
import com.swmansion.starknet.data.types.NumAsHexBase
import java.math.BigInteger

@get:JvmSynthetic
val String.toNumAsHex: NumAsHex
    get() = NumAsHex.fromHex(this)

@get:JvmSynthetic
val BigInteger.toNumAsHex: NumAsHex
    get() = NumAsHex(this)

@get:JvmSynthetic
val NumAsHexBase.toNumAsHex: NumAsHex
    get() = NumAsHex(this.value)
