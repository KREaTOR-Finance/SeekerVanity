# Vanity (Android / Solana Seeker)

Native Android dApp for generating Solana vanity addresses on-device.

## Current beta scope
- Target: Solana Seeker / Solana Mobile dApp Store constraints
- On-device generation only; no backend secret storage
- Wallet connect: MWA (Mobile Wallet Adapter), standard authorize/connect flow
- Seed Vault: cannot import programmatically from a dApp; use secure export flow
- Patterns:
  - Prefix mode: `SKR` enforced
  - Suffix mode: up to 6 base58 chars; user can toggle case-sensitive vs case-insensitive match
  - Presets: `RAVEN`, `SEEKER` (valid base58)
- Export: 12-word BIP39 seed phrase; secret never stored; reveal requires user verification
- Theme: black + neon green

## Next steps
1. Compose UI scaffold + theme
2. Generator engine (Kotlin first; NDK if needed)
3. Secure reveal + verify flow (FLAG_SECURE)
4. MWA connect and ownership approval UX

Do not commit any `.env` files or API keys.
