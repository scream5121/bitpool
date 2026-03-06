# BitPool — public-pool.io Mining Monitor

A lightweight Android app for monitoring your Bitcoin solo miners on [public-pool.io](https://public-pool.io).

## Features

- Live hashrate for all workers
- All-time best difficulty tracking
- Lottery odds (probability per day/year, expected wait time)
- Block reward value in USD and CAD (live BTC price via CoinGecko)
- Network difficulty and retarget countdown
- Pool stats (miners, hashrate, fee, block height)
- Your share of total pool hashrate
- Auto-refresh every 60 seconds
- Zero fees, zero accounts, zero tracking

## Data Sources

- **public-pool.io** — miner and pool stats
- **mempool.space** — network difficulty and retarget info
- **CoinGecko** — live BTC/USD and BTC/CAD price

## Requirements

- Android 7.0+ (API 24)
- Internet permission only

## Security

- HTTPS only — cleartext blocked at manifest, network config, and JS layers
- No third-party libraries
- Bitcoin address stored locally in SharedPreferences only (never transmitted)
- WebView hardened: no file access, no mixed content, no remote debugging
- Host whitelist enforced at native layer

## Building

Requires Android SDK with platform-34 and build-tools 34.0.0.

```bash
./gradlew assembleRelease
```

## License

MIT
