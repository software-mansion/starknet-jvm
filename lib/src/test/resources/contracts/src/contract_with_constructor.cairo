#[starknet::interface]
trait IContractWithConstructor<T> {
    // Returns the current val1.
    fn get_val1(self: @T) -> u128;
}

#[starknet::contract]
mod ContractWithConstructor {
    use traits::Into;

    #[storage]
    struct Storage {
        val1: u128,
    }

    #[constructor]
    fn constructor(ref self: ContractState, val1_: u128, val2_: u128) {
        self.val1.write(val1_);
    // val2_ is not used.
    }

    #[external(v0)]
    impl ContractWithConstructor of super::IContractWithConstructor<ContractState> {
        fn get_val1(self: @ContractState) -> u128 {
            self.val1.read()
        }
    }
}
