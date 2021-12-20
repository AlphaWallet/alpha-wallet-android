package com.alphawallet.app.widget;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.GitHubRelease;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class WhatsNewView extends ConstraintLayout {

    private RecyclerView recyclerView;

    public WhatsNewView(Context context, List<GitHubRelease> items, View.OnClickListener onCloseListener) {
        super(context);
        init(R.layout.layout_dialog_whats_new);

        findViewById(R.id.close_action).setOnClickListener(onCloseListener);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setAdapter(new WhatsNewAdapter(items));
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
    }

    private void init(@LayoutRes int layoutId) {
        LayoutInflater.from(getContext()).inflate(layoutId, this, true);
    }

    public class WhatsNewAdapter extends RecyclerView.Adapter<WhatsNewAdapter.WhatsNewItemViewHolder>
    {

        final SimpleDateFormat formatterFrom = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'", Locale.ROOT);
        final SimpleDateFormat formatterTo = new SimpleDateFormat("dd.MM.yy", Locale.ROOT);

        private final List<GitHubRelease> items;
        public WhatsNewAdapter(List<GitHubRelease> items) {
            super();
            this.items = items;
        }

        @Override
        public WhatsNewItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_whats_new, parent, false);

            return new WhatsNewItemViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(WhatsNewItemViewHolder holder, int position) {
            GitHubRelease release = items.get(position);
            try {
                Date createdAt = formatterFrom.parse(release.getCreatedAt());
                holder.date.setText(formatterTo.format(createdAt));

            } catch (ParseException e) {
                e.printStackTrace();
            }

            String[] body = release.getBody().split("\r\n- ");
            holder.details.removeAllViews();
            int index = 0;
            for (String entry : body) {
                LinearLayout ll = new LinearLayout(getContext());

                ll.setOrientation(LinearLayout.HORIZONTAL);

                TextView tv = new TextView(getContext());
                tv.setText(entry);
                tv.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
                tv.setTypeface(ResourcesCompat.getFont(getContext(), R.font.font_regular));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);

                ImageView iv = new ImageView(getContext());
                iv.setImageDrawable(getContext().getDrawable(R.drawable.ic_icons_system_border_circle));
                ll.addView(iv);
                ll.addView(tv);
                if (index++ == 0) {
                    iv.setVisibility(View.INVISIBLE);
                }
                holder.details.addView(ll);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)ll.getLayoutParams();
                float scale = getContext().getResources().getDisplayMetrics().density;
                int dp16 = (int) (15*scale + 0.5f);
                int dp4 = (int) (4*scale + 0.5f);
                lp.setMargins(dp16, dp4, dp16, dp4);
                ll.setLayoutParams(lp);
                lp = (LinearLayout.LayoutParams)tv.getLayoutParams();
                lp.setMarginStart(dp4);
                tv.setLayoutParams(lp);
            }
        }

        @Override
        public int getItemCount() {
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
