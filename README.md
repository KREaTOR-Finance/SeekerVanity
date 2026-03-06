# Vanity (Android / Solana Seeker)

Native Android dApp for generating Solana vanity addresses on-device.

## MVP requirements (locked)
- Target: Solana Seeker / Solana Mobile dApp store constraints
- On-device generation only; **no backend secret storage**
- Wallet connect: **MWA** (Mobile Wallet Adapter)
- Seed Vault: **cannot import programmatically from a dApp** → use secure export flow
- Patterns:
  - Prefix mode: **`SKR`** enforced (SKR paywall)
  - Suffix mode: up to **6** base58 chars; user can toggle case-sensitive vs case-insensitive match
  - Presets: `RAVEN`, `SEEKER` (valid base58)
- Export: **24-word BIP39** seed phrase; secret never stored; reveal requires user verification
- Payments: **250 SKR per generation** to treasury address `2atxENqo9UY4eSHTRZnQoiaJuh7MeDn7wq61KB77hpKM` (mainnet)
- Theme: black + neon green

## Next steps
1) Compose UI scaffold + theme
2) Generator engine (Kotlin first; NDK if needed)
3) Secure reveal + verify flow (FLAG_SECURE)
4) MWA connect
5) SKR paywall (create/send tx via MWA; verify signature/recipient/amount)

> Note: addresses and pricing are not secrets, but never commit any `.env` or API keys.
