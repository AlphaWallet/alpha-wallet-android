# AlphaWallet - Ultimate Ethereum Wallet for Android

[![Build Status](https://travis-ci.com/James-Sangalli/alpha-wallet.svg?token=J2hT1s5bGKT1npuPugWb&branch=master)](https://travis-ci.com/James-Sangalli/alpha-wallet.svg?token=J2hT1s5bGKT1npuPugWb&branch=master)
[![License](https://img.shields.io/badge/license-GPL3-green.svg?style=flat)](https://github.com/fastlane/fastlane/blob/master/LICENSE)

[<img src="https://raw.githubusercontent.com/TrustWallet/trust-wallet-android/master/resources/android_cover.png">](https://play.google.com/store/apps/details?id=com.wallet.crypto.trustapp)

[<img src=https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png height="88">](https://play.google.com/store/apps/details?id=com.wallet.crypto.trustapp)

Welcome to Trust's open source Android app!

## Getting Started

1. Install latest Android Studio (>=3.0.0).
2. Clone this repository.
3. Register with etherscan.io and populate the API keys in Controller.java
4. Build and run.

Try the [app](https://play.google.com/store/apps/details?id=com.wallet.crypto.trustapp) on Google Play Store.

## Deploying with fastlane

`fastlane screengrab` - take screenshots
`fastlane listing` - update play store listing

## Cloning the repo notes

Since submodules are in use you will need to use this command sequence to clone:

git clone --recurse-submodules https://github.com/James-Sangalli/alpha-wallet

If you are updating a branch use this:

git checkout <branch you are switching to>
git submodule update --init --recursive

