import com.swmansion.starknet.crypto.Poseidon;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.data.types.Felt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class DepsTest {
    @Test
    public void testPedersen() {
        var pedersen = StarknetCurve.pedersen(Felt.ONE, new Felt(2));
        assertNotEquals(Felt.ZERO, pedersen);
    }

    @Test
    public void testPoseidon() {
        var poseidon = Poseidon.poseidonHash(Felt.ONE, new Felt(2));
        assertNotEquals(Felt.ZERO, poseidon);
    }
}
