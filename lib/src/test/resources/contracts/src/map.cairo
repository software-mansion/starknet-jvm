#[starknet::interface]
trait IMap<T> {
    // Returns the value associated with the given key.
    fn get(self: @T, key: felt252) -> felt252;
    // Sets the value associated with the given key.
    fn put(ref self: T, key: felt252, value: felt252);
}

#[starknet::contract]
mod Map {
    use traits::Into;

    #[storage]
    struct Storage {
        map: LegacyMap::<felt252, felt252>,
    }

    #[external(v0)]
    impl Map of super::IMap<ContractState> {
        fn get(self: @ContractState, key: felt252) -> felt252 {
            self.map.read(key)
        }
        fn put(ref self: ContractState, key: felt252, value: felt252) {
            self.map.write(key, value);
        }
    }
}

