# crypto-tool

A tool for cryptocurrency holders.

Use `sbt dist` to build.
Remember to fill in api keys in `conf/crypto-tool.conf`.

Available commands:

  - Show balances: `./crypto-tool balance --exchanges binance,bittrex --eth-addresses "0xAddr1,0xAddr2" --etc-addresses "0xAddr1,0xAddr2"` (eth/etc addresses are optional, if not provided values from `crypto-tool.conf` will be used)
  - Calculate profit/loss: `./crypto-tool profit --exchange binance --currencies XVG,LTC,ETC`