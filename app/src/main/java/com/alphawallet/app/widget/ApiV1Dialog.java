package com.alphawallet.app.widget;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;
import com.alphawallet.app.api.v1.entity.ApiV1;
import com.alphawallet.app.api.v1.entity.Metadata;
import com.alphawallet.app.api.v1.entity.Method;
import com.alphawallet.app.api.v1.entity.request.ApiV1Request;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ApiV1Dialog extends BottomSheetDialog
{
    private final BottomSheetToolbarView toolbar;
    private final Context context;
    private final FunctionButtonBar functionButtonBar;
    private final LinearLayout infoLayout;

    public ApiV1Dialog(@NonNull Context context)
    {
        super(context);
        View view = View.inflate(getContext(), R.layout.dialog_api_v1, null);
        setContentView(view);
        this.context = context;
        toolbar = view.findViewById(R.id.bottom_sheet_toolbar);
        infoLayout = view.findViewById(R.id.layout_info);
        functionButtonBar = view.findViewById(R.id.layoutButtons);
        setCanceledOnTouchOutside(false);
        getBehavior().setDraggable(false);
    }

    public ApiV1Dialog(Context context, ApiV1Request request)
    {
        this(context);
        setMetadata(request.getMetadata());
        setMethod(request.getMethod());
    }

    private void setMethod(Method method)
    {
        if (method.getCallType().equals(ApiV1.CallType.CONNECT))
        {
            toolbar.setTitle(R.string.title_api_v1_connect_to);
            functionButtonBar.setPrimaryButtonText(R.string.action_connect);
            functionButtonBar.setSecondaryButtonText(R.string.dialog_reject);
        }
        else if (method.getCallType().equals(ApiV1.CallType.SIGN_PERSONAL_MESSAGE))
        {
            toolbar.setTitle(R.string.dialog_title_sign_transaction);
            functionButtonBar.setPrimaryButtonText(R.string.action_sign);
            functionButtonBar.setSecondaryButtonText(R.string.action_cancel);
        }
    }

    private void setMetadata(Metadata metadata)
    {
        if (metadata.iconUrl != null)
        {
            toolbar.setLogo(context, metadata.iconUrl);
        }
        addWidget(R.string.label_api_v1_app_name, metadata.name);
        addWidget(R.string.label_api_v1_app_url, metadata.appUrl);
    }

    public void setPrimaryButtonListener(View.OnClickListener listener)
    {
        functionButtonBar.setPrimaryButtonClickListener(listener);
    }

    public void setSecondaryButtonListener(View.OnClickListener listener)
    {
        functionButtonBar.setSecondaryButtonClickListener(listener);
        toolbar.setCloseListener(listener);
    }

    public void addWidget(int labelRes, String value)
    {
        if (!TextUtils.isEmpty(value))
        {
            SimpleSheetWidget widget = new SimpleSheetWidget(getContext(), labelRes);
            widget.setValue(value);
            infoLayout.addView(widget);
        }
    }

    public void addWidget(View view)
    {
        infoLayout.addView(view);
    }

    public void hideFunctionBar()
    {
        functionButtonBar.setVisibility(View.GONE);
    }
}
