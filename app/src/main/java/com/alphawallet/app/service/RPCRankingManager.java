package com.alphawallet.app.service;

import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.RpcConfigRepository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Response;

/**
 * Manages RPC config refresh and runtime RPC selection.
 */
public class RPCRankingManager
{
    private final RpcConfigRepository rpcConfigRepository;
    private final BlockedRpcService blockedRpcService;

    // In-memory round-robin pointer per chain
    private final ConcurrentHashMap<Long, AtomicInteger> roundRobinPointers = new ConcurrentHashMap<>();

    public RPCRankingManager(RpcConfigRepository rpcConfigRepository, BlockedRpcService blockedRpcService)
    {
        this.rpcConfigRepository = rpcConfigRepository;
        this.blockedRpcService = blockedRpcService;
    }

    public void checkAndUpdateRPCs()
    {
        rpcConfigRepository.checkAndUpdateRPCs();
    }

    public void forceRefreshRPCs()
    {
        rpcConfigRepository.forceRefreshRPCs();
    }

    public String[] getRPCsForChain(long chainId)
    {
        String[] urls = EthereumNetworkBase.getConfiguredRPCUrls(chainId);
        if (blockedRpcService != null)
        {
            return blockedRpcService.filterBlocked(urls);
        }
        return urls;
    }

    public int getNextRPCIndex(long chainId, int rpcCount)
    {
        if (rpcCount <= 0) return 0;

        AtomicInteger pointer = roundRobinPointers.computeIfAbsent(chainId, k -> new AtomicInteger(0));
        return Math.abs(pointer.getAndIncrement() % rpcCount);
    }

    public void recordCallResult(long chainId, String url, long responseMs)
    {
        // No-op for file-only RPC config model.
    }

    public void recordRpcFailure(String url, Throwable error)
    {
        if (blockedRpcService != null)
        {
            blockedRpcService.recordFailureIfBlocked(url, error);
        }
    }

    public void recordRpcResponse(String url, Response response)
    {
        if (blockedRpcService != null)
        {
            blockedRpcService.recordResponseIfBlocked(url, response);
        }
    }

    public boolean isCalibrating(long chainId)
    {
        return false;
    }

    public BlockedRpcService getBlockedRpcService()
    {
        return blockedRpcService;
    }
}
