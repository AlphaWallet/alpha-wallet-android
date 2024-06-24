package com.langitwallet.app.ui.widget.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.langitwallet.app.R;
import com.google.android.material.button.MaterialButton;
import com.langitwallet.app.ui.widget.OnSuggestionClickListener;

import java.util.List;

public class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.ViewHolder> {
    private List<String> suggestionList;
    private String suggestion = "";
    private final OnSuggestionClickListener onSuggestionClickListener;

    class ViewHolder extends RecyclerView.ViewHolder {
        MaterialButton name;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
        }
    }

    public SuggestionsAdapter(List<String> data, OnSuggestionClickListener listener) {
        this.suggestionList = data;
        this.onSuggestionClickListener = listener;
    }

    public void setData(List<String> data){
        suggestionList = data;
        suggestion = "";
        notifyDataSetChanged();
    }

    public void setData(List<String> data, String searchText){
        suggestionList = data;
        suggestion = searchText;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_suggestion_list, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        String data = suggestionList.get(i);
        viewHolder.name.setText(data);

        viewHolder.name.setOnClickListener(v -> {
            String outputWord = data.replaceFirst(suggestion, "");
            onSuggestionClickListener.onSuggestionClick(outputWord);
        });
    }

    @Override
    public int getItemCount() {
        return suggestionList.size();
    }

    public String getSingleSuggestion()
    {
        if (suggestionList.size() == 1) return suggestionList.get(0);
        else return "";
    }
}
