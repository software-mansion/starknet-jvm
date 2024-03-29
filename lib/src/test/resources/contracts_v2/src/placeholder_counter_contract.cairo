#[starknet::interface]
trait IOtherContract<TContractState> {
    fn decrease_allowed(self: @TContractState) -> bool;
}

#[starknet::interface]
trait ISaltedCounterContract<TContractState> {
    fn __placeholder___increase_counter(ref self: TContractState, amount: u128);
    fn __placeholder___decrease_counter(ref self: TContractState, amount: u128);
    fn __placeholder___get_counter(self: @TContractState) -> u128;
}

#[starknet::contract]
mod SaltedCounterContract {
    use starknet::ContractAddress;
    use super::{
        IOtherContractDispatcher, IOtherContractDispatcherTrait, IOtherContractLibraryDispatcher
    };

    #[storage]
    struct Storage {
        counter: u128,
        other_contract: IOtherContractDispatcher
    }

    #[event]
    #[derive(Drop, starknet::Event)]
    enum Event {
        CounterIncreased: CounterIncreased,
        CounterDecreased: CounterDecreased
    }

    #[derive(Drop, starknet::Event)]
    struct CounterIncreased {
        amount: u128
    }

    #[derive(Drop, starknet::Event)]
    struct CounterDecreased {
        amount: u128
    }

    #[constructor]
    fn constructor(
        ref self: ContractState, initial_counter: u128, other_contract_addr: ContractAddress
    ) {
        self.counter.write(initial_counter);
        self
            .other_contract
            .write(IOtherContractDispatcher { contract_address: other_contract_addr });
    }

    #[external(v0)]
    impl SaltedCounterContract of super::ISaltedCounterContract<ContractState> {
        fn __placeholder___get_counter(self: @ContractState) -> u128 {
            self.counter.read()
        }

        fn __placeholder___increase_counter(ref self: ContractState, amount: u128) {
            let current = self.counter.read();
            self.counter.write(current + amount);
            self.emit(CounterIncreased { amount });
        }

        fn __placeholder___decrease_counter(ref self: ContractState, amount: u128) {
            let allowed = self.other_contract.read().decrease_allowed();
            if allowed {
                let current = self.counter.read();
                self.counter.write(current - amount);
                self.emit(CounterDecreased { amount });
            }
        }
    }
}
