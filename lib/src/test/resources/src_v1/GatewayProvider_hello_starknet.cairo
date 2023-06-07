#[contract]
mod HelloStarknet {
    struct Storage {
        balance: felt252,
    }

    // Increases the balance by the given amount.
    #[external]
    fn gw_increase_balance(amount: felt252) {
        balance::write(balance::read() + amount);
    }

    // Returns the current balance.
    #[view]
    fn gw_get_balance() -> felt252 {
        balance::read()
    }
}
