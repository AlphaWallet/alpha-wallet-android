package io.stormbird.wallet.ui.widget.holder;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.OpenseaElement;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.service.OpenseaService;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class OpenseaHolder extends BinderViewHolder<OpenseaElement> {

    public static final int VIEW_TYPE = 1302;

    private final TextView title;
    private final OpenseaService openseaService;

    @Nullable
    private Disposable queryService;

    public OpenseaHolder(int resId, ViewGroup parent, OpenseaService service) {
        super(resId, parent);
        title = findViewById(R.id.name);
        openseaService = service;
    }

    @Override
    public void bind(@Nullable OpenseaElement element, @NonNull Bundle addition)
    {
        //for now add title and ERC721 graphic
        String assetName;
        if (element.assetName != null && !element.assetName.equals("null"))
        {
            assetName = element.assetName;
        }
        else
        {
            assetName = "ID# " + String.valueOf(element.tokenId);
        }
        title.setText(assetName);

        //now add the graphic
        queryService = openseaService.fetchBitmap(element.imageURL, 200)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::gotBitmap, this::bitmapError);
    }

    private void bitmapError(Throwable throwable)
    {
        throwable.printStackTrace();
    }

    private void gotBitmap(Bitmap object)
    {
        queryService.dispose();
        ImageView iv = findViewById(R.id.image_view);
        iv.setImageBitmap(object);
    }
}
