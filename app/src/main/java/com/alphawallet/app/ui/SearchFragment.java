package com.alphawallet.app.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.alphawallet.app.R;


public class SearchFragment extends Fragment {
    private View.OnClickListener listener;

    public void setCallbacks(View.OnClickListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        RelativeLayout layout = view.findViewById(R.id.layout);
        layout.setOnClickListener(listener);
        return view;
    }
}
