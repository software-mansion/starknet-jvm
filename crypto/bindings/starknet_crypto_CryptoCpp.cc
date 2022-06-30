#include <jni.h>

#include "./starknet_crypto_CryptoCpp.h"
#include "third_party/gsl/gsl-lite.hpp"
#include "starkware/algebra/prime_field_element.h"
#include "starkware/crypto/pedersen_hash.h"
#include "starkware/crypto/ffi/portable_endian.h"

using ValueType = starkware::PrimeFieldElement::ValueType;

constexpr size_t kElementSize = sizeof(ValueType);
constexpr size_t kOutBufferSize = 1024;
static_assert(kOutBufferSize >= kElementSize, "kOutBufferSize is not big enough");
constexpr int bytesCount = 32;

class InvalidFieldProvidedError : public std::runtime_error {
    public:
    explicit InvalidFieldProvidedError():
    std::runtime_error("Invalid field provided") {}
};

ValueType deserialize(const gsl::span<const gsl::byte> span) {
  const size_t N = ValueType::LimbCount();
  std::array<uint64_t, N> value{};
  gsl::copy(span, gsl::byte_span(value));
  for (uint64_t& x : value) {
    x = le64toh(x);
  }
  return ValueType(value);
}

void serialize(const ValueType& val, const gsl::span<gsl::byte> span_out) {
  const size_t N = ValueType::LimbCount();
  for (size_t i = 0; i < N; ++i) {
    uint64_t limb = htole64(val[i]);
    gsl::copy(gsl::byte_span(limb), span_out.subspan(i * sizeof(uint64_t), sizeof(uint64_t)));
  }
}

starkware::PrimeFieldElement fieldFromJArray(JNIEnv * env, jbyteArray input) {
    jsize size = env->GetArrayLength(input);
    if (size != kElementSize) {
        throw InvalidFieldProvidedError();
    }
    jboolean isCopy;
    gsl::byte* bytes = (gsl::byte*)(env->GetByteArrayElements(input, &isCopy));

    starkware::PrimeFieldElement field = starkware::PrimeFieldElement::FromBigInt(
        deserialize(gsl::make_span(bytes, kElementSize)
    ));
    env->ReleaseByteArrayElements(input, (jbyte*)bytes, JNI_ABORT);

    return field;
}

void throwException( JNIEnv *env, std::string className, std::string message)
{
    jclass exClass = env->FindClass(className.c_str());
    env->ThrowNew(exClass, message.c_str() );
}


JNIEXPORT jbyteArray JNICALL Java_starknet_crypto_CryptoCpp_pedersen
  (JNIEnv * env, jclass, jbyteArray first, jbyteArray second) {
  jbyteArray result = env->NewByteArray(bytesCount);
  try {
      auto hash = PedersenHash(
          fieldFromJArray(env, first),
          fieldFromJArray(env, second)
      );

      gsl::byte out[kOutBufferSize];
      serialize(hash.ToStandardForm(), gsl::make_span(out, kElementSize));

      env->SetByteArrayRegion(result, 0, bytesCount, (const jbyte*) out);
  } catch (const InvalidFieldProvidedError& e) {
    auto className = "java/lang/IllegalArgumentException";
    auto message = "Invalid field element provided.";
    throwException(env, className, message);
  } catch (...) {
    auto className = "java/lang/Exception";
    auto message = "Unknown crypto-cpp exception.";
    throwException(env, className, message);
  }
  return result;
}