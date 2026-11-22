#!/usr/bin/env bash
# Decode SIGNING_KEYSTORE_BASE64 and tell Gradle which keystore type it is.
# Tag builds (refs/tags/v*) fail when any signing secret is missing.
set -euo pipefail

if [ -z "${GITHUB_OUTPUT:-}" ]; then
  echo "GITHUB_OUTPUT is not set" >&2
  exit 1
fi

: "${RUNNER_TEMP:=${TMPDIR:-/tmp}}"
mkdir -p "$RUNNER_TEMP"

missing=()
[ -z "${SIGNING_KEYSTORE_BASE64:-}" ] && missing+=("SIGNING_KEYSTORE_BASE64")
[ -z "${SIGNING_STORE_PASSWORD:-}" ] && missing+=("SIGNING_STORE_PASSWORD")
[ -z "${SIGNING_KEY_ALIAS:-}" ] && missing+=("SIGNING_KEY_ALIAS")
[ -z "${SIGNING_KEY_PASSWORD:-}" ] && missing+=("SIGNING_KEY_PASSWORD")

is_tag=false
if [[ "${GITHUB_REF:-}" == refs/tags/v* ]]; then
  is_tag=true
fi

join_by_comma() {
  local joined=""
  local item
  for item in "$@"; do
    if [ -n "$joined" ]; then
      joined+=", "
    fi
    joined+="$item"
  done
  printf '%s' "$joined"
}

if [ "${#missing[@]}" -gt 0 ]; then
  echo "present=false" >> "$GITHUB_OUTPUT"
  missing_list=$(join_by_comma "${missing[@]}")
  if [ "$is_tag" = true ]; then
    echo "::error::Refusing to publish ${GITHUB_REF_NAME:-$GITHUB_REF}: missing signing secret(s): ${missing_list}. Android cannot install an unsigned release APK or update an existing install with it. Add repository Actions secrets SIGNING_KEYSTORE_BASE64 (base64 of the PKCS12 .p12 keystore), SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS, and SIGNING_KEY_PASSWORD."
    exit 1
  fi
  if [ "${#missing[@]}" -eq 4 ]; then
    echo "::warning::Signing secrets are not set. The release APK will be unsigned, and the debug APK will use this runner's debug keystore. Tag builds (v*) fail until SIGNING_KEYSTORE_BASE64, SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS, and SIGNING_KEY_PASSWORD are set."
    exit 0
  fi
  echo "::error::Incomplete signing secrets. Missing: ${missing_list}. Set all of SIGNING_KEYSTORE_BASE64, SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS, and SIGNING_KEY_PASSWORD, or unset all of them."
  exit 1
fi

keystore="${RUNNER_TEMP}/sms-organizer-release.p12"
umask 077
if ! printf '%s' "$SIGNING_KEYSTORE_BASE64" | base64 --decode > "$keystore" 2>"${RUNNER_TEMP}/signing-b64.err"; then
  rm -f "$keystore"
  echo "::error::SIGNING_KEYSTORE_BASE64 is not valid base64. Encode the binary PKCS12 keystore with: base64 -w0 your-release.p12"
  exit 1
fi
rm -f "${RUNNER_TEMP}/signing-b64.err"
chmod 600 "$keystore"

if [ ! -s "$keystore" ]; then
  rm -f "$keystore"
  echo "::error::SIGNING_KEYSTORE_BASE64 decoded to an empty file. Encode the binary PKCS12 keystore with: base64 -w0 your-release.p12"
  exit 1
fi

# keytool auto-detects the store type and will list a JKS file with -storetype pkcs12,
# so the header is what decides the type Gradle must use. PKCS12 is ASN.1 (0x30).
# JKS starts with FE ED FE ED. The CI keystore is an OpenSSL PKCS12 .p12.
magic=$(od -An -t x1 -N 4 "$keystore" | tr -d ' \n' | tr '[:upper:]' '[:lower:]')
case "$magic" in
  feedfeed*) store_type="jks" ;;
  30*) store_type="pkcs12" ;;
  *)
    rm -f "$keystore"
    echo "::error::Decoded SIGNING_KEYSTORE_BASE64 does not look like a PKCS12 or JKS keystore (header ${magic}). Base64-encode the binary .p12 file itself, not a PEM and not a second base64 pass."
    exit 1
    ;;
esac

if ! command -v keytool >/dev/null 2>&1; then
  rm -f "$keystore"
  echo "::error::keytool is not on PATH; set up a JDK before decoding the keystore."
  exit 1
fi

keytool_err="${RUNNER_TEMP}/keytool.err"
if ! keytool -list \
  -keystore "$keystore" \
  -storetype "$store_type" \
  -storepass:env SIGNING_STORE_PASSWORD \
  -alias "$SIGNING_KEY_ALIAS" \
  >/dev/null 2>"$keytool_err"; then
  echo "::error::Could not read alias '${SIGNING_KEY_ALIAS}' from the decoded ${store_type} keystore. Check SIGNING_STORE_PASSWORD and SIGNING_KEY_ALIAS. The expected CI keystore is a PKCS12 .p12 file (storeType pkcs12). A trailing newline in the password secret also causes this failure."
  if [ -s "$keytool_err" ]; then
    cat "$keytool_err" >&2
  fi
  rm -f "$keystore" "$keytool_err"
  exit 1
fi
rm -f "$keytool_err"

{
  echo "present=true"
  echo "keystore_path=${keystore}"
  echo "store_type=${store_type}"
} >> "$GITHUB_OUTPUT"

echo "Signing keystore decoded (${store_type}, alias ${SIGNING_KEY_ALIAS}). Release and debug APKs will use this key."
