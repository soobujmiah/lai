#!/usr/bin/env python3
"""Deterministically generate the LAI test-release signing keystore (CI only).

Why this exists
---------------
GitHub Actions runners auto-create a FRESH Android debug keystore on every job, so "release"
APKs signed with the debug key carry a DIFFERENT certificate each run. Android then refuses to
install a newer build over the previous one (signatures do not match), which broke every
debug-key "release" install (field reports 2026-08-19).

This script derives a FIXED RSA key + self-signed certificate from a committed seed, so every
CI "release" build is signed with the SAME certificate and installs cleanly over the previous
build (versionCode = GitHub run number keeps increasing).

Security posture
----------------
- The private key is derived from the seed constant in this file: anyone can reproduce it.
  This is a TEST-ONLY key for installable device builds. It is NEVER used for production —
  v* tag builds use the ANDROID_KEYSTORE_* secrets and set LAI_SIGNING_PRODUCTION=true, which
  drives BuildConfig.PRODUCTION_SIGNED.
- No key material is stored in the repository; the keystore is generated into the runner's
  temp directory at build time.

Usage: python3 scripts/ci/generate_test_keystore.py <output.p12>
Writes a PKCS#12 keystore with: password "lai-test-release", alias "lai-test".
"""

from __future__ import annotations

import datetime
import hashlib
import sys

SEED = b"lai-test-release-signing-v1"  # committed; reproducible by design

# Stateful across calls on purpose: pycryptodome draws repeatedly from randfunc while searching
# for primes; a stateless counter would return the same bytes on every call and loop forever.
_STATE = {"n": 0}


def _randfunc(n: int) -> bytes:
    """Deterministic pseudo-random stream: SHA-256 chain over SEED + a process-global counter."""
    out = b""
    while len(out) < n:
        out += hashlib.sha256(SEED + _STATE["n"].to_bytes(8, "big")).digest()
        _STATE["n"] += 1
    return out[:n]


def _fixed_serial() -> int:
    digest = hashlib.sha256(SEED + b"serial").digest()[:8]
    return int.from_bytes(digest, "big") & ((1 << 63) - 1)


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    out_path = sys.argv[1]

    from Crypto.Util.number import getPrime, inverse  # pip: pycryptodome
    from Crypto.PublicKey import RSA
    from cryptography import x509  # pip: cryptography
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import rsa
    from cryptography.x509.oid import NameOID

    # Deterministic RSA-2048-class key built from seeded primes (RSA.generate hangs on a
    # deterministic randfunc because it re-draws candidates from the same bytes forever).
    e = 65537
    p = getPrime(1024, randfunc=_randfunc)
    q = getPrime(1024, randfunc=_randfunc)
    n = p * q
    d = inverse(e, (p - 1) * (q - 1))
    pycrypto_key = RSA.construct((n, e, d, p, q))
    private = serialization.load_pem_private_key(pycrypto_key.export_key("PEM"), password=None)
    assert isinstance(private, rsa.RSAPrivateKey)

    # Deterministic self-signed certificate (fixed serial + validity window).
    name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "LAI Test Release Signing")])
    cert = (
        x509.CertificateBuilder()
        .subject_name(name)
        .issuer_name(name)
        .public_key(private.public_key())
        .serial_number(_fixed_serial())
        .not_valid_before(datetime.datetime(2026, 1, 1, tzinfo=datetime.timezone.utc))
        .not_valid_after(datetime.datetime(2056, 1, 1, tzinfo=datetime.timezone.utc))
        .sign(private, hashes.SHA256())
    )

    # PKCS#12 keystore (Gradle/AGP accepts it like a .jks).
    # Wrap key + certificate into a PKCS#12 keystore with openssl -legacy (PBES1). cryptography's
    # own PKCS12 output uses PBES2 (HmacPBESHA256) which older JDKs reject ("keystore password was
    # incorrect" / "Algorithm HmacPBESHA256 not available"). openssl is already a CI dependency
    # (used by catalog_publish.yml). The certificate is what signing uses, and it is deterministic.
    key_pem = private.private_bytes(
        serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()
    )
    cert_pem = cert.public_bytes(serialization.Encoding.PEM)
    import os
    import subprocess
    import tempfile

    with tempfile.TemporaryDirectory() as tmp:
        key_path = os.path.join(tmp, "key.pem")
        cert_path = os.path.join(tmp, "cert.pem")
        with open(key_path, "wb") as f:
            f.write(key_pem)
        with open(cert_path, "wb") as f:
            f.write(cert_pem)
        subprocess.run(
            [
                "openssl", "pkcs12", "-export", "-legacy",
                "-inkey", key_path,
                "-in", cert_path,
                "-name", "lai-test",
                "-out", out_path,
                "-passout", "pass:lai-test-release",
            ],
            check=True,
        )
    print(f"Test keystore written to {out_path}")
    print("store/key password: lai-test-release | alias: lai-test")
    return 0


if __name__ == "__main__":
    sys.exit(main())
