package com.alphawallet.app.ui.widget.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.widget.TokenIcon;
import com.alphawallet.ethereum.NetworkInfo;

import org.web3j.protocol.Web3j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class NodeStatusAdapter extends RecyclerView.Adapter<NodeStatusAdapter.ViewHolder> {

    private final List<NetworkInfo> networkList;
    private final Map<Long, NodeStatus> statusMap = new ConcurrentHashMap<Long, NodeStatus>();
    private final ArrayList<Disposable> disposables = new ArrayList<>();

    public NodeStatusAdapter(List<NetworkInfo> networkList)
    {
        this.networkList = networkList;
        initStatusCheck();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_network_status, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NetworkInfo item = networkList.get(position);
        if (item != null)
        {
            holder.name.setText(item.name);
            holder.chainId.setText(holder.itemLayout.getContext().getString(R.string.chain_id, item.chainId));
            holder.tokenIcon.bindData(item.chainId);
            NodeStatus nodeStatus = statusMap.get(item.chainId);
            if (nodeStatus == null) return;

            if (nodeStatus == NodeStatus.STRONG)
            {
                holder.status.setBackgroundResource(R.drawable.ic_circle_green);
            }
            else if (nodeStatus == NodeStatus.MEDIUM)
            {
                holder.status.setBackgroundResource(R.drawable.ic_circle_yellow);
            }
            else
            {
                holder.status.setBackgroundResource(R.drawable.ic_circle_red);
            }
            holder.status.setVisibility(View.VISIBLE);
            holder.loader.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return networkList.size();
    }

    private void initStatusCheck()
    {
        for (NetworkInfo item : networkList)
        {
            Disposable disposable = Single.fromCallable( () -> fetchNodeStatus(item.chainId))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe( (nodeStatus) -> updateStatus(item.chainId, nodeStatus) );
            disposables.add(disposable);
        }
    }

    private NodeStatus fetchNodeStatus(long chainId)
    {
        NodeStatus status = NodeStatus.NOT_RESPONDING;
        final Web3j web3j = TokenRepository.getWeb3jService(chainId);
        try
        {
            long startTime = System.currentTimeMillis();
            String s = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            status = latency < 1000 ? NodeStatus.STRONG : NodeStatus.MEDIUM;
        }
        catch (Exception e)
        {
            Timber.e(e, "checkNodeStatus: exception: chainID: %s ", chainId);
        }
        return status;
    }

    private void updateStatus(long chainId, NodeStatus status)
    {
        statusMap.put(chainId, status);
        int position = 0;
        for (int i=0; i<networkList.size(); i++)
        {
            if (networkList.get(i).chainId == chainId)
            {
                position = i;
                break;
            }
        }
        notifyItemChanged(position);
        Timber.d("updateStatus: chain: %s-%s: %s", chainId, EthereumNetworkBase.getShortChainName(chainId),status);
    }

    public void dispose()
    {
        try
        {
            for (Disposable d : disposables)
            {
                if (!d.isDisposed()) d.dispose();
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "disposeAll: exception: ");
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView name;
        TextView chainId;
        View itemLayout;
        TokenIcon tokenIcon;
        ImageView status;
        ProgressBar loader;

        ViewHolder(View view)
        {
            super(view);
            name = view.findViewById(R.id.name);
            chainId = view.findViewById(R.id.chain_id);
            itemLayout = view.findViewById(R.id.layout_list_item);
            tokenIcon = view.findViewById(R.id.token_icon);
            status = view.findViewById(R.id.image_status);
            loader = view.findViewById(R.id.loader);
        }
    }

    enum NodeStatus
    {
        NOT_RESPONDING, STRONG, MEDIUM, WEAK
    }

}

