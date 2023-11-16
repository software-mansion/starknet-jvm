import com.swmansion.starknet.data.types.NumAsHex
import java.math.BigInteger

@get:JvmSynthetic
val String.toNumAsHex: NumAsHex
    get() = NumAsHex.fromHex(this)

@get:JvmSynthetic
val BigInteger.toNumAsHex: NumAsHex
    get() = NumAsHex(this)
