package com.alphawallet.app.service;

import android.text.TextUtils;

import com.alphawallet.app.realm.AutoCloseableRealm;
import com.alphawallet.app.realm.RealmCompat;
import com.alphawallet.app.repository.entity.RealmBlockedRpc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLProtocolException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * Detects router/ISP blocks of RPC hostnames (TLS hijack / AiProtection style) and
 * persists them keyed by external IP + domain so round-robin can skip them.
 */
public class BlockedRpcService
{
    private static final String TAG = "BlockedRpc";
    private static final String IPIFY_URL = "https://api.ipify.org";
    private static final long IP_CACHE_MS = TimeUnit.HOURS.toMillis(1);
    private static final String UNKNOWN_IP = "unknown";

    private final RealmManager realmManager;
    private final OkHttpClient httpClient;

    private volatile String cachedExternalIp = null;
    private volatile long ipFetchedAt = 0L;

    /** Domains blocked this process, even before IP/DB write completes. */
    private final Set<String> sessionBlockedDomains = ConcurrentHashMap.newKeySet();

    /** Cached Realm rows for {@link #loadedIp}. */
    private final Set<String> blockedForCurrentIp = ConcurrentHashMap.newKeySet();
    private volatile String loadedIp = null;

    public BlockedRpcService(RealmManager realmManager, OkHttpClient httpClient)
    {
        this.realmManager = realmManager;
        this.httpClient = httpClient.newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .followRedirects(false)
                .retryOnConnectionFailure(false)
                .build();
    }

    public String[] filterBlocked(String[] urls)
    {
        if (urls == null || urls.length == 0)
        {
            return urls;
        }

        ensureBlockedCacheLoaded();

        List<String> kept = new ArrayList<>(urls.length);
        for (String url : urls)
        {
            if (!isBlocked(url))
            {
                kept.add(url);
            }
        }

        // Never return empty if that would disable the chain entirely — fall back to originals.
        if (kept.isEmpty())
        {
            return urls;
        }
        return kept.toArray(new String[0]);
    }

    public boolean isBlocked(String rpcUrl)
    {
        String domain = extractDomain(rpcUrl);
        if (TextUtils.isEmpty(domain))
        {
            return false;
        }
        if (sessionBlockedDomains.contains(domain))
        {
            return true;
        }
        ensureBlockedCacheLoaded();
        return blockedForCurrentIp.contains(domain);
    }

    /**
     * Inspect a failed RPC attempt. If it looks like a router/ISP TLS hijack, persist the domain.
     */
    public void recordFailureIfBlocked(String rpcUrl, Throwable error)
    {
        if (!looksLikeRouterBlock(error))
        {
            return;
        }
        String reason = summarizeThrowable(error);
        recordBlocked(rpcUrl, reason);
    }

    /**
     * Inspect an HTTP response that may be a router interstitial (HTTP RPC or MITM page).
     */
    public void recordResponseIfBlocked(String rpcUrl, Response response)
    {
        if (response == null || !looksLikeRouterBlockResponse(response))
        {
            return;
        }
        String reason = "router_http_block status=" + response.code()
                + " url=" + response.request().url();
        recordBlocked(rpcUrl, reason);
    }

    public void recordBlocked(String rpcUrl, String reason)
    {
        String domain = extractDomain(rpcUrl);
        if (TextUtils.isEmpty(domain))
        {
            return;
        }

        sessionBlockedDomains.add(domain);
        blockedForCurrentIp.add(domain);

        Timber.w("%s: marking RPC domain blocked: %s (%s)", TAG, domain, reason);

        new Thread(() -> persistBlocked(domain, reason), "blocked-rpc-persist").start();
    }

    public static String extractDomain(String rpcUrl)
    {
        if (TextUtils.isEmpty(rpcUrl))
        {
            return "";
        }
        try
        {
            URI uri = URI.create(rpcUrl.trim());
            String host = uri.getHost();
            if (!TextUtils.isEmpty(host))
            {
                return host.toLowerCase(Locale.ROOT);
            }
        }
        catch (Exception ignored)
        {
            // fall through
        }

        // Fallback for odd URLs without a clean URI host.
        String cleaned = rpcUrl.replaceFirst("(?i)^https?://", "");
        int slash = cleaned.indexOf('/');
        if (slash >= 0)
        {
            cleaned = cleaned.substring(0, slash);
        }
        int colon = cleaned.indexOf(':');
        if (colon >= 0)
        {
            cleaned = cleaned.substring(0, colon);
        }
        return cleaned.toLowerCase(Locale.ROOT).trim();
    }

    public static boolean looksLikeRouterBlock(Throwable error)
    {
        Throwable cursor = error;
        while (cursor != null)
        {
            if (cursor instanceof SSLHandshakeException
                    || cursor instanceof SSLProtocolException
                    || cursor instanceof SSLPeerUnverifiedException
                    || cursor instanceof CertificateException)
            {
                return true;
            }
            if (cursor instanceof SSLException)
            {
                return true;
            }

            String message = cursor.getMessage();
            if (!TextUtils.isEmpty(message))
            {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("ssl record")
                        || lower.contains("unexpected record")
                        || lower.contains("plaintext http")
                        || lower.contains("wrong version number")
                        || lower.contains("handshake failed")
                        || lower.contains("ssl_error")
                        || lower.contains("certificate"))
                {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    public static boolean looksLikeRouterBlockResponse(Response response)
    {
        String host = response.request().url().host();
        if (isPrivateOrLocalHost(host))
        {
            return true;
        }

        String location = response.header("Location");
        if (!TextUtils.isEmpty(location))
        {
            String lowerLoc = location.toLowerCase(Locale.ROOT);
            if (lowerLoc.contains("blocking.asp")
                    || lowerLoc.contains("aiprotection")
                    || looksPrivateInUrl(lowerLoc))
            {
                return true;
            }
        }

        // Some routers answer 200 with an interstitial body.
        try
        {
            if (response.body() != null)
            {
                // Don't consume large bodies; peek via content-type / prior URL path only when small.
                String contentType = response.header("Content-Type", "");
                if (contentType.toLowerCase(Locale.ROOT).contains("text/html"))
                {
                    // HTML from an RPC endpoint is never legitimate JSON-RPC.
                    return true;
                }
            }
        }
        catch (Exception ignored)
        {
            // ignore
        }

        String path = response.request().url().encodedPath();
        if (!TextUtils.isEmpty(path) && path.toLowerCase(Locale.ROOT).contains("blocking.asp"))
        {
            return true;
        }

        return false;
    }

    private void persistBlocked(String domain, String reason)
    {
        try
        {
            String externalIp = getExternalIp();
            ensureBlockedCacheLoaded(externalIp);

            try (AutoCloseableRealm closer = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
            {
                RealmCompat.executeTransaction(closer.getRealm(), mutableRealm -> {
                    String key = RealmBlockedRpc.makeKey(externalIp, domain);
                    RealmBlockedRpc row = RealmCompat.where(mutableRealm, RealmBlockedRpc.class)
                            .equalTo("key", key)
                            .findFirst();
                    if (row == null)
                    {
                        row = RealmCompat.createObject(mutableRealm, RealmBlockedRpc.class, key);
                    }
                    row.setExternalIp(externalIp);
                    row.setDomain(domain);
                    row.setReason(reason != null ? reason : "");
                    row.setTimeStamp(System.currentTimeMillis());
                });
            }

            blockedForCurrentIp.add(domain);
            Timber.i("%s: persisted blocked RPC domain=%s externalIp=%s", TAG, domain, externalIp);
        }
        catch (Exception e)
        {
            Timber.e(e, "%s: failed to persist blocked RPC %s", TAG, domain);
        }
    }

    private void ensureBlockedCacheLoaded()
    {
        ensureBlockedCacheLoaded(getExternalIpCachedOrUnknown());
    }

    private synchronized void ensureBlockedCacheLoaded(String externalIp)
    {
        if (externalIp.equals(loadedIp))
        {
            return;
        }

        Set<String> loaded = ConcurrentHashMap.newKeySet();
        try (AutoCloseableRealm closer = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
        {
            RealmCompat.Results<RealmBlockedRpc> results = RealmCompat.where(closer.getRealm(), RealmBlockedRpc.class)
                    .equalTo("externalIp", externalIp)
                    .findAll();
            for (RealmBlockedRpc row : results)
            {
                if (!TextUtils.isEmpty(row.getDomain()))
                {
                    loaded.add(row.getDomain().toLowerCase(Locale.ROOT));
                }
            }
        }
        catch (Exception e)
        {
            Timber.w(e, "%s: failed loading blocked RPC cache for %s", TAG, externalIp);
        }

        blockedForCurrentIp.clear();
        blockedForCurrentIp.addAll(loaded);
        blockedForCurrentIp.addAll(sessionBlockedDomains);
        loadedIp = externalIp;
    }

    /**
     * Best-effort public IP. Cached for {@link #IP_CACHE_MS}. Falls back to {@link #UNKNOWN_IP}.
     */
    public String getExternalIp()
    {
        long now = System.currentTimeMillis();
        String cached = cachedExternalIp;
        if (!TextUtils.isEmpty(cached) && (now - ipFetchedAt) < IP_CACHE_MS)
        {
            return cached;
        }

        synchronized (this)
        {
            cached = cachedExternalIp;
            if (!TextUtils.isEmpty(cached) && (now - ipFetchedAt) < IP_CACHE_MS)
            {
                return cached;
            }

            String fetched = fetchExternalIp();
            cachedExternalIp = fetched;
            ipFetchedAt = System.currentTimeMillis();
            if (!fetched.equals(loadedIp))
            {
                // Force reload of blocked set for the new network.
                loadedIp = null;
                ensureBlockedCacheLoaded(fetched);
            }
            return fetched;
        }
    }

    private String getExternalIpCachedOrUnknown()
    {
        String cached = cachedExternalIp;
        if (!TextUtils.isEmpty(cached))
        {
            return cached;
        }
        // Kick off async fetch for next time; use unknown for immediate filtering.
        new Thread(this::getExternalIp, "blocked-rpc-ip").start();
        return UNKNOWN_IP;
    }

    private String fetchExternalIp()
    {
        try
        {
            Request request = new Request.Builder().url(IPIFY_URL).get().build();
            try (Response response = httpClient.newCall(request).execute())
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    String ip = response.body().string().trim();
                    if (!TextUtils.isEmpty(ip) && ip.length() < 64 && !ip.contains("<"))
                    {
                        return ip;
                    }
                }
            }
        }
        catch (IOException e)
        {
            Timber.w("%s: external IP lookup failed: %s", TAG, e.getMessage());
        }
        return UNKNOWN_IP;
    }

    private static boolean isPrivateOrLocalHost(String host)
    {
        if (TextUtils.isEmpty(host))
        {
            return false;
        }
        String lower = host.toLowerCase(Locale.ROOT);
        if (lower.equals("localhost") || lower.endsWith(".local"))
        {
            return true;
        }
        try
        {
            InetAddress address = InetAddress.getByName(host);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress();
        }
        catch (Exception e)
        {
            return looksPrivateInUrl(lower);
        }
    }

    private static boolean looksPrivateInUrl(String value)
    {
        if (TextUtils.isEmpty(value))
        {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("192.168.")
                || lower.contains("127.0.0.1")
                || lower.matches(".*\\b10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b.*")
                || lower.matches(".*\\b172\\.(1[6-9]|2\\d|3[0-1])\\.\\d{1,3}\\.\\d{1,3}\\b.*")
                || lower.contains("blocking.asp")
                || lower.contains("aiprotection");
    }

    private static String summarizeThrowable(Throwable error)
    {
        if (error == null)
        {
            return "unknown";
        }
        StringBuilder sb = new StringBuilder();
        Throwable cursor = error;
        int depth = 0;
        while (cursor != null && depth < 4)
        {
            if (sb.length() > 0)
            {
                sb.append(" | ");
            }
            sb.append(cursor.getClass().getSimpleName());
            if (!TextUtils.isEmpty(cursor.getMessage()))
            {
                sb.append(": ").append(cursor.getMessage());
            }
            cursor = cursor.getCause();
            depth++;
        }
        String out = sb.toString();
        return out.length() > 480 ? out.substring(0, 480) : out;
    }
}
