from auth.challenge import (
    generate_expected_response,
    set_shared_key,
    verify_response,
)


def test_nf1_challenge_response_security():
    shared_key = b"0123456789ABCDEF0123456789ABCDEF"  # 32 bytes
    set_shared_key(shared_key)

    challenge_a = b"\x01" * 16
    response_a = generate_expected_response(challenge_a)

    assert verify_response(challenge_a, response_a)

    tampered_response = bytearray(response_a)
    tampered_response[0] ^= 0xFF
    assert not verify_response(challenge_a, bytes(tampered_response))

    challenge_b = b"\x02" * 16
    assert not verify_response(challenge_b, response_a)