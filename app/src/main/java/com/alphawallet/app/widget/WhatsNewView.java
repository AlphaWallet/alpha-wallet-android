package com.alphawallet.app.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.GitHubRelease;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class WhatsNewView extends ConstraintLayout
{

    private RecyclerView recyclerView;

    public WhatsNewView(Context context, List<GitHubRelease> items, View.OnClickListener onCloseListener)
    {
        super(context);
        init(R.layout.layout_dialog_whats_new);

        findViewById(R.id.close_action).setOnClickListener(onCloseListener);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setAdapter(new WhatsNewAdapter(items));
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
    }

    private void init(@LayoutRes int layoutId)
    {
        LayoutInflater.from(getContext()).inflate(layoutId, this, true);
    }

    public class WhatsNewAdapter extends RecyclerView.Adapter<WhatsNewAdapter.WhatsNewItemViewHolder>
    {

        final SimpleDateFormat formatterFrom = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'", Locale.ROOT);
        final SimpleDateFormat formatterTo = new SimpleDateFormat("dd.MM.yy", Locale.ROOT);

        private final List<GitHubRelease> items;

        public WhatsNewAdapter(List<GitHubRelease> items)
        {
            super();
            this.items = items;
        }

        @Override
        public WhatsNewItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_whats_new, parent, false);

            return new WhatsNewItemViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(WhatsNewItemViewHolder holder, int position)
        {
            GitHubRelease release = items.get(position);
            try
            {
                Date createdAt = formatterFrom.parse(release.getCreatedAt());
                holder.date.setText(formatterTo.format(createdAt));

            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }

            String[] body = release.getBody().split("\r\n- ");
            holder.details.removeAllViews();
            int index = 0;
            for (String entry : body)
            {
                TextView t = new TextView(getContext(), null, R.attr.whatsNewEntryStyle);
                t.setText(entry.trim());
                if (index++ == 0)
                {
                    t.setCompoundDrawables(null, null, null, null);
                }
                holder.details.addView(t);
            }
        }

        @Override
        public int getItemCount()
        {
            return items.size();
        }

        class WhatsNewItemViewHolder extends RecyclerView.ViewHolder
        {
            TextView date;
            LinearLayout details;

            WhatsNewItemViewHolder(View view)
            {
                super(view);
                date = view.findViewById(R.id.date);
                details = view.findViewById(R.id.details);
            }
        }
    }


}
