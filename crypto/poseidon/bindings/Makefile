CC = gcc
CFLAGS = -Wall -Werror -O3 -fPIC
LDFLAGS += -shared -L../poseidon/sources -l_pos

libposeidon_jni.so: starknet_crypto_Poseidon.c
	$(CC) $(CFLAGS) $(LDFLAGS) -o $@ $^
