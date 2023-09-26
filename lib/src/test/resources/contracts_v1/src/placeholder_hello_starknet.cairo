#[contract]
mod SaltedHelloStarknet {
    struct Storage {
        balance: felt252, 
    }

    // Increases the balance by the given amount.
    #[external]
    fn __placeholder___increase_balance(amount: felt252) {
        balance::write(balance::read() + amount);
    }

    // Returns the current balance.
    #[view]
    fn __placeholder___get_balance() -> felt252 {
        balance::read()
    }
}
