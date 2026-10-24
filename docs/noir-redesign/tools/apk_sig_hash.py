"""Print the signing certificate of an APK (v2/v3 block) as
  - sha256 fingerprint
  - Android's Signature.hashCode() hex (what `dumpsys package` shows in
    "signatures:[...]"), so a built APK can be checked against an installed
    package without root.
Usage: apk_sig_hash.py <file.apk>
"""
import hashlib, struct, sys

data = open(sys.argv[1], 'rb').read()
eocd = data.rfind(b'PK\x05\x06')
cd_off = struct.unpack('<I', data[eocd + 16:eocd + 20])[0]
assert data[cd_off - 16:cd_off] == b'APK Sig Block 42', 'no APK signing block'
size = struct.unpack('<Q', data[cd_off - 24:cd_off - 16])[0]
start = cd_off - size - 8
p, end = start + 8, cd_off - 24
blocks = {}
while p < end:
    ln, bid = struct.unpack('<QI', data[p:p + 12])
    blocks[bid] = data[p + 12:p + 8 + ln]
    p += 8 + ln

def lp(buf, off):
    n = struct.unpack('<I', buf[off:off + 4])[0]
    return buf[off + 4:off + 4 + n], off + 4 + n

for bid, name in ((0xf05368c0, 'v3'), (0x7109871a, 'v2')):
    if bid not in blocks:
        continue
    signers, _ = lp(blocks[bid], 0)
    signer, _ = lp(signers, 0)
    signed_data, _ = lp(signer, 0)
    _digests, off = lp(signed_data, 0)
    certs, _ = lp(signed_data, off)
    cert, _ = lp(certs, 0)
    h = 1
    for b in cert:
        h = (31 * h + (b - 256 if b > 127 else b)) & 0xFFFFFFFF
    print('%s sha256=%s hashCode=%x' % (name, hashlib.sha256(cert).hexdigest(), h))
    break
