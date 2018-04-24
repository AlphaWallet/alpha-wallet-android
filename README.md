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

## Contributing

We intend for this project to be an educational resource: we are excited to
share our wins, mistakes, and methodology of android development as we work
in the open. Our primary focus is to continue improving the app for our users in
line with our roadmap.

The best way to submit feedback and report bugs is to open a GitHub issue.
Please be sure to include your operating system, device, version number, and
steps to reproduce reported bugs. Keep in mind that all participants will be
expected to follow our code of conduct.

## Code of Conduct

We aim to share our knowledge and findings as we work daily to improve our
product, for our community, in a safe and open space. We work as we live, as
kind and considerate human beings who learn and grow from giving and receiving
positive, constructive feedback. We reserve the right to delete or ban any
behavior violating this base foundation of respect.

Help with localization?
Here is a public link to join localization project: https://lokalise.co/signup/3947163159df13df851b51.98101647/all/

## Cloning the repo notes

Since submodules are in use you will need to use this command sequence to clone:

git clone --recurse-submodules https://github.com/James-Sangalli/alpha-wallet

If you are updating a branch use this:

git checkout <branch you are switching to>
git submodule update --init --recursive
