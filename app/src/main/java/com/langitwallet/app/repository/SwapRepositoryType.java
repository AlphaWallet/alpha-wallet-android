package com.langitwallet.app.repository;

import com.langitwallet.app.entity.lifi.SwapProvider;

import java.util.List;

public interface SwapRepositoryType
{
    List<SwapProvider> getProviders();
}
