# Implementation Plan - Fill in Translations for Multicast Transactions

This plan covers adding translations for the `multicast_transactions` and `multicast_transactions_sub` string keys across all supported locales in the AlphaWallet project.

## Proposed Changes

The following strings will be added to the `strings.xml` files for each supported language.

### English (Reference)
- `multicast_transactions`: "Multicast Transactions"
- `multicast_transactions_sub`: "Use up to 3 RPCs to broadcast TX"

### [MODIFY] [res/values-es/strings.xml](file:///D:/Users/soyle/StudioProjects/alpha-wallet-android/app/src/main/res/values-es/strings.xml)
- `multicast_transactions`: "Transacciones Multicast"
- `multicast_transactions_sub`: "Usa hasta 3 RPC para transmitir TX"

### [MODIFY] [res/values-fr/strings.xml](file:///D:/Users/soyle/StudioProjects/alpha-wallet-android/app/src/main/res/values-fr/strings.xml)
- `multicast_transactions`: "Transactions Multicast"
- `multicast_transactions_sub`: "Utilisez jusqu'à 3 RPC pour diffuser les TX"

### [MODIFY] [res/values-id/strings.xml](file:///D:/Users/soyle/StudioProjects/alpha-wallet-android/app/src/main/res/values-id/strings.xml)
- `multicast_transactions`: "Transaksi Multicast"
- `multicast_transactions_sub`: "Gunakan hingga 3 RPC untuk menyiarkan TX"

### [MODIFY] [res/values-my/strings.xml](file:///D:/Users/soyle/StudioProjects/alpha-wallet-android/app/src/main/res/values-my/strings.xml)
- `multicast_transactions`: "Multicast ငွေလွှဲမှုများ"
- `multicast_transactions_sub`: "TX ကို ထုတ်လွှင့်ရန် RPC ၃ ခုအထိ အသုံးပြုပါ။"

### [MODIFY] [res/values-vi/strings.xml](file:///D:/Users/soyle/StudioProjects/alpha-wallet-android/app/src/main/res/values-vi/strings.xml)
- `multicast_transactions`: "Giao dịch Multicast"
- `multicast_transactions_sub`: "Sử dụng tối đa 3 RPC để phát TX"

### [MODIFY] [res/values-zh/strings.xml](file:///D:/Users/soyle/StudioProjects/alpha-wallet-android/app/src/main/res/values-zh/strings.xml)
- `multicast_transactions`: "多播交易"
- `multicast_transactions_sub`: "使用最多 3 个 RPC 广播交易"

## Verification Plan

### Manual Verification
- Verify that the strings are correctly appended to each `strings.xml` file.
- Verify that the XML structure remains valid.
