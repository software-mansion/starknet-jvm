#[starknet::interface]
trait IBalance<T> {
    // Returns the current balance.
    fn get_balance(self: @T) -> u128;
    // Increases the balance by the given amount.
    fn increase_balance(ref self: T, a: u128);
}

#[starknet::contract]
mod Balance {
    use traits::Into;

    #[storage]
    struct Storage {
        balance: u128,
    }

    #[constructor]
    fn constructor(ref self: ContractState, value_: u128) {
        self.balance.write(value_);
    }

    #[external(v0)]
    impl Balance of super::IBalance<ContractState> {
        fn get_balance(self: @ContractState) -> u128 {
            self.balance.read()
        }
        fn increase_balance(ref self: ContractState, a: u128) {
            self.balance.write(self.balance.read() + a);
        }
    }
}

