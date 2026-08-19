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

Determinism strategy
--------------------
- Key generation is PURE STDLIB (Miller-Rabin over deterministic candidates derived from the
  seed) — no third-party RNG that could consume randomness differently across versions, which
  is what made a pycryptodome-based attempt non-reproducible between environments.
- The certificate is built with `cryptography` (RSA-PKCS1v15 signing is deterministic; fixed
  serial/subject/validity), and the keystore is wrapped with `openssl pkcs12 -legacy` (PBES1)
  so every JDK/apksigner can read it.
- No key material is stored in the repository; the keystore is generated into the runner's
  temp directory at build time.

Security posture
----------------
The private key is reproducible from this file's seed by design: it is a TEST-ONLY key for
installable device builds. Production releases (v* tags) use the ANDROID_KEYSTORE_* secrets and
set LAI_SIGNING_PRODUCTION=true, which drives BuildConfig.PRODUCTION_SIGNED.

Usage: python3 scripts/ci/generate_test_keystore.py <output.p12>
Writes a PKCS#12 keystore with: password "lai-test-release", alias "lai-test".
"""

from __future__ import annotations

import datetime
import hashlib
import subprocess
import sys
import tempfile
import os

SEED = b"lai-test-release-signing-v1"  # committed; reproducible by design
EXPONENT = 65537

# Deterministic Miller-Rabin bases and trial-division primes (stdlib only).
_TRIAL_PRIMES = [
    2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
    73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151,
    157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229,
]


def _digest_stream(seed: bytes, idx: int, length: int) -> bytes:
    """Deterministic 'length' bytes derived from seed + index (SHA-256 chain)."""
    out = b""
    counter = 0
    while len(out) < length:
        out += hashlib.sha256(seed + idx.to_bytes(8, "big") + counter.to_bytes(8, "big")).digest()
        counter += 1
    return out[:length]


def _candidate(bits: int, idx: int) -> int:
    length = (bits + 7) // 8
    value = int.from_bytes(_digest_stream(SEED, idx, length), "big")
    value |= (1 << (bits - 1)) | (1 << (bits - 2)) | 1  # top two bits + odd
    return value


def _is_prime(n: int) -> bool:
    if n < 2:
        return False
    for p in _TRIAL_PRIMES:
        if n % p == 0:
            return n == p
    d = n - 1
    r = 0
    while d % 2 == 0:
        d //= 2
        r += 1
    for a in _TRIAL_PRIMES[:25]:  # deterministic base set, ample for 1024-bit
        x = pow(a, d, n)
        if x in (1, n - 1):
            continue
        for _ in range(r - 1):
            x = pow(x, 2, n)
            if x == n - 1:
                break
        else:
            return False
    return True


def _prime(bits: int, idx: int) -> int:
    candidate = _candidate(bits, idx)
    while not _is_prime(candidate):
        candidate += 2
    return candidate


def _fixed_serial() -> int:
    digest = hashlib.sha256(SEED + b"serial").digest()[:8]
    return int.from_bytes(digest, "big") & ((1 << 63) - 1)


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    out_path = sys.argv[1]

    from cryptography import x509  # pip: cryptography
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import rsa
    from cryptography.x509.oid import NameOID

    # Deterministic RSA key from seeded primes (pure stdlib).
    p = _prime(1024, idx=1)
    q = _prime(1024, idx=2)
    while q == p:
        q = _prime(1024, idx=3)
    n = p * q
    phi = (p - 1) * (q - 1)
    d = pow(EXPONENT, -1, phi)
    private = rsa.RSAPrivateNumbers(
        p=p,
        q=q,
        d=d,
        dmp1=d % (p - 1),
        dmq1=d % (q - 1),
        iqmp=pow(q, -1, p),
        public_numbers=rsa.RSAPublicNumbers(EXPONENT, n),
    ).private_key()

    # Deterministic self-signed certificate (RSA-PKCS1v15 signing is deterministic).
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

    # Wrap key + certificate into a PKCS#12 keystore with openssl -legacy (PBES1), readable by
    # every JDK/apksigner. openssl is already a CI dependency (catalog_publish.yml uses it).
    key_pem = private.private_bytes(
        serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()
    )
    cert_pem = cert.public_bytes(serialization.Encoding.PEM)
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
