// SPDX-License-Identifier: MIT
// OpenZeppelin Contracts for Cairo v0.5.0 (account/presets/Account.cairo)

%lang starknet

from starkware.cairo.common.cairo_builtins import HashBuiltin, SignatureBuiltin, BitwiseBuiltin
from starkware.starknet.common.syscalls import (
    call_contract,
    get_caller_address,
    get_contract_address,
    get_tx_info
)
//
// Constructor
//

@constructor
func constructor{syscall_ptr: felt*, pedersen_ptr: HashBuiltin*, range_check_ptr}(
    publicKey: felt
) {
    Account.initializer(publicKey);
    return ();
}

//
// Getters
//

@view
func getPublicKey{syscall_ptr: felt*, pedersen_ptr: HashBuiltin*, range_check_ptr}() -> (
    publicKey: felt
) {
    let (publicKey: felt) = Account.get_public_key();
    return (publicKey=publicKey);
}

@view
func supportsInterface{syscall_ptr: felt*, pedersen_ptr: HashBuiltin*, range_check_ptr}(
    interfaceId: felt
) -> (success: felt) {
    return Account.supports_interface(interfaceId);
}

//
// Setters
//

@external
func setPublicKey{syscall_ptr: felt*, pedersen_ptr: HashBuiltin*, range_check_ptr}(
    newPublicKey: felt
) {
    Account.set_public_key(newPublicKey);
    return ();
}

//
// Business logic
//

@view
func isValidSignature{
    syscall_ptr: felt*, pedersen_ptr: HashBuiltin*, ecdsa_ptr: SignatureBuiltin*, range_check_ptr
}(hash: felt, signature_len: felt, signature: felt*) -> (isValid: felt) {
    let (isValid: felt) = Account.is_valid_signature(hash, signature_len, signature);
    return (isValid=isValid);
}

@external
func __validate__{
    syscall_ptr: felt*, pedersen_ptr: HashBuiltin*, ecdsa_ptr: SignatureBuiltin*, range_check_ptr
}(call_array_len: felt, call_array: AccountCallArray*, calldata_len: felt, calldata: felt*) {
    let (tx_info) = get_tx_info();
    Account.is_valid_signature(tx_info.transaction_hash, tx_info.signature_len, tx_info.signature);
    return ();
}

@external
func __validate_declare__{
    syscall_ptr: felt*, pedersen_ptr: HashBuiltin*, ecdsa_ptr: SignatureBuiltin*, range_check_ptr
}(class_hash: felt) {
    let (tx_info) = get_tx_info();
    Account.is_valid_signature(tx_info.transaction_hash, tx_info.signature_len, tx_info.signature);
    return ();
}

@external
func __validate_deploy__{
    syscall_ptr: felt*,
    pedersen_ptr: HashBuiltin*,
    ecdsa_ptr: SignatureBuiltin*,
    range_check_ptr
} (
    class_hash: felt,
    salt: felt,
    publicKey: felt
) {
    let (tx_info) = get_tx_info();
    Account.is_valid_signature(tx_info.transaction_hash, tx_info.signature_len, tx_info.signature);
    return ();
}

@external
func __execute__{
    syscall_ptr: felt*,
    pedersen_ptr: HashBuiltin*,
    ecdsa_ptr: SignatureBuiltin*,
    bitwise_ptr: BitwiseBuiltin*,
    range_check_ptr,
}(call_array_len: felt, call_array: AccountCallArray*, calldata_len: felt, calldata: felt*) -> (
    response_len: felt, response: felt*
) {
    let (response_len, response) = Account.execute(
        call_array_len, call_array, calldata_len, calldata
    );
    return (response_len, response);
}

// SPDX-License-Identifier: MIT
// OpenZeppelin Contracts for Cairo v0.5.0 (utils/constants/library.cairo)


//
// Numbers
//

const UINT8_MAX = 255;

//
// Interface Ids
//

// ERC165
const IERC165_ID = 0x01ffc9a7;
const INVALID_ID = 0xffffffff;

// Account
const IACCOUNT_ID = 0xa66bd575;

// ERC721
const IERC721_ID = 0x80ac58cd;
const IERC721_RECEIVER_ID = 0x150b7a02;
const IERC721_METADATA_ID = 0x5b5e139f;
const IERC721_ENUMERABLE_ID = 0x780e9d63;

// AccessControl
const IACCESSCONTROL_ID = 0x7965db0b;

//
// Roles
//

const DEFAULT_ADMIN_ROLE = 0;

//
// Starknet
//

const TRANSACTION_VERSION = 1;


// SPDX-License-Identifier: MIT
// OpenZeppelin Contracts for Cairo v0.5.0 (account/library.cairo)


from starkware.cairo.common.registers import get_fp_and_pc
from starkware.cairo.common.signature import verify_ecdsa_signature
from starkware.cairo.common.alloc import alloc
from starkware.cairo.common.uint256 import Uint256
from starkware.cairo.common.memcpy import memcpy
from starkware.cairo.common.math import split_felt
from starkware.cairo.common.math_cmp import is_le_felt
from starkware.cairo.common.bool import TRUE, FALSE
from starkware.cairo.common.cairo_secp.signature import verify_eth_signature_uint256

//
// Storage
//

@storage_var
func Account_public_key() -> (public_key: felt) {
}

//
// Structs
//

struct Call {
    to: felt,
    selector: felt,
    calldata_len: felt,
    calldata: felt*,
}

// Tmp struct introduced while we wait for Cairo
// to support passing `[AccountCall]` to __execute__
struct AccountCallArray {
    to: felt,
    selector: felt,
    data_offset: felt,
    data_len: felt,
}

namespace Account {
    //
    // Initializer
    //

    func initializer{syscall_ptr: felt*, pedersen_ptr: HashBuiltin*, range_check_ptr}(
        _public_key: felt
    ) {
        Account_public_key.write(_public_key);
        return ();
    }

    //
    // Guards
    //

    func assert_only_self{syscall_ptr: felt*}() {
        let (self) = get_contract_address();
        let (caller) = get_caller_address();
        with_attr error_message("Account: caller is not this account") {
            assert self = caller;
        }
        return ();
    }

    //
    // Getters
    //

    func get_public_key{syscall_ptr: felt*, pedersen_ptr: HashBuiltin*, range_check_ptr}() -> (
        public_key: felt
    ) {
        return Account_public_key.read();
    }

    func supports_interface{syscall_ptr: felt*, pedersen_ptr: HashBuiltin*, range_check_ptr}(interface_id: felt) -> (
        success: felt
    ) {
        if (interface_id == IERC165_ID) {
            return (success=TRUE);
        }
        if (interface_id == IACCOUNT_ID) {
            return (success=TRUE);
        }
        return (success=FALSE);
    }

    //
    // Setters
    //

    func set_public_key{syscall_ptr: felt*, pedersen_ptr: HashBuiltin*, range_check_ptr}(
        new_public_key: felt
    ) {
        assert_only_self();
        Account_public_key.write(new_public_key);
        return ();
    }

    //
    // Business logic
    //

    func is_valid_signature{
        syscall_ptr: felt*,
        pedersen_ptr: HashBuiltin*,
        ecdsa_ptr: SignatureBuiltin*,
        range_check_ptr,
    }(hash: felt, signature_len: felt, signature: felt*) -> (is_valid: felt) {
        let (_public_key) = Account_public_key.read();

        // This interface expects a signature pointer and length to make
        // no assumption about signature validation schemes.
        // But this implementation does, and it expects a (sig_r, sig_s) pair.
        let sig_r = signature[0];
        let sig_s = signature[1];

        verify_ecdsa_signature(
            message=hash, public_key=_public_key, signature_r=sig_r, signature_s=sig_s
        );

        return (is_valid=TRUE);
    }

    func is_valid_eth_signature{
        syscall_ptr: felt*,
        pedersen_ptr: HashBuiltin*,
        bitwise_ptr: BitwiseBuiltin*,
        range_check_ptr,
    }(hash: felt, signature_len: felt, signature: felt*) -> (is_valid: felt) {
        alloc_locals;
        let (_public_key) = get_public_key();
        let (__fp__, _) = get_fp_and_pc();

        // This interface expects a signature pointer and length to make
        // no assumption about signature validation schemes.
        // But this implementation does, and it expects a the sig_v, sig_r,
        // sig_s, and hash elements.
        let sig_v: felt = signature[0];
        let sig_r: Uint256 = Uint256(low=signature[1], high=signature[2]);
        let sig_s: Uint256 = Uint256(low=signature[3], high=signature[4]);
        let (high, low) = split_felt(hash);
        let msg_hash: Uint256 = Uint256(low=low, high=high);

        let (local keccak_ptr: felt*) = alloc();

        with keccak_ptr {
            verify_eth_signature_uint256(
                msg_hash=msg_hash, r=sig_r, s=sig_s, v=sig_v, eth_address=_public_key
            );
        }

        return (is_valid=TRUE);
    }

    func execute{
        syscall_ptr: felt*,
        pedersen_ptr: HashBuiltin*,
        ecdsa_ptr: SignatureBuiltin*,
        bitwise_ptr: BitwiseBuiltin*,
        range_check_ptr,
    }(call_array_len: felt, call_array: AccountCallArray*, calldata_len: felt, calldata: felt*) -> (
        response_len: felt, response: felt*
    ) {
        alloc_locals;

        let (tx_info) = get_tx_info();
        // Disallow deprecated tx versions
        with_attr error_message("Account: deprecated tx version") {
            assert is_le_felt(TRANSACTION_VERSION, tx_info.version) = TRUE;
        }

        // Assert not a reentrant call
        let (caller) = get_caller_address();
        with_attr error_message("Account: reentrant call") {
            assert caller = 0;
        }

        // TMP: Convert `AccountCallArray` to 'Call'.
        let (calls: Call*) = alloc();
        _from_call_array_to_call(call_array_len, call_array, calldata, calls);
        let calls_len = call_array_len;

        // Execute call
        let (response: felt*) = alloc();
        let (response_len) = _execute_list(calls_len, calls, response);

        return (response_len=response_len, response=response);
    }

    func _execute_list{syscall_ptr: felt*}(calls_len: felt, calls: Call*, response: felt*) -> (
        response_len: felt
    ) {
        alloc_locals;

        // if no more calls
        if (calls_len == 0) {
            return (response_len=0);
        }

        // do the current call
        let this_call: Call = [calls];
        let res = call_contract(
            contract_address=this_call.to,
            function_selector=this_call.selector,
            calldata_size=this_call.calldata_len,
            calldata=this_call.calldata,
        );
        // copy the result in response
        memcpy(response, res.retdata, res.retdata_size);
        // do the next calls recursively
        let (response_len) = _execute_list(
            calls_len - 1, calls + Call.SIZE, response + res.retdata_size
        );
        return (response_len=response_len + res.retdata_size);
    }

    func _from_call_array_to_call{syscall_ptr: felt*}(
        call_array_len: felt, call_array: AccountCallArray*, calldata: felt*, calls: Call*
    ) {
        // if no more calls
        if (call_array_len == 0) {
            return ();
        }

        // parse the current call
        assert [calls] = Call(
            to=[call_array].to,
            selector=[call_array].selector,
            calldata_len=[call_array].data_len,
            calldata=calldata + [call_array].data_offset
            );
        // parse the remaining calls recursively
        _from_call_array_to_call(
            call_array_len - 1, call_array + AccountCallArray.SIZE, calldata, calls + Call.SIZE
        );
        return ();
    }
}
