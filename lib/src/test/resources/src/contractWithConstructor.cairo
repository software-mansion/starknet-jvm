%lang starknet

from starkware.cairo.common.cairo_builtins import HashBuiltin

@storage_var
func val1_storage() -> (res: felt) {
}

@constructor
func constructor{
    syscall_ptr: felt*,
    pedersen_ptr: HashBuiltin*,
    range_check_ptr,
}(val1: felt, val2: felt) {
    val1_storage.write(val1);
    return ();
}

@view
func get_val1{
    syscall_ptr: felt*,
    pedersen_ptr: HashBuiltin*,
    range_check_ptr,
}() -> (res: felt) {
    let (res) = val1_storage.read();
    return (res=res);
}
