package com.wallet.crypto.trustapp.widget;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.wallet.crypto.trustapp.R;


public class AddWalletView extends FrameLayout implements View.OnClickListener {
	private OnNewWalletClickListener onNewWalletClickListener;
	private OnImportWalletClickListener onImportWalletClickListener;

	public AddWalletView(Context context) {
		this(context, R.layout.layout_dialog_add_account);
	}

	public AddWalletView(Context context, @LayoutRes int layoutId) {
		super(context);

		init(layoutId);
	}

	private void init(@LayoutRes int layoutId) {
		LayoutInflater.from(getContext()).inflate(layoutId, this, true);
		findViewById(R.id.new_account_action).setOnClickListener(this);
		findViewById(R.id.import_account_action).setOnClickListener(this);

        ViewPager viewPager = findViewById(R.id.intro);
        if (viewPager != null) {
            viewPager.setPageTransformer(false, new DepthPageTransformer());
            viewPager.setAdapter(new IntroPagerAdapter());
        }
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.new_account_action: {
				if (onNewWalletClickListener != null) {
					onNewWalletClickListener.onNewWallet(view);
				}
			} break;
			case R.id.import_account_action: {
				if (onImportWalletClickListener != null) {
					onImportWalletClickListener.onImportWallet(view);
				}
			} break;
		}
	}
	
	public void setOnNewWalletClickListener(OnNewWalletClickListener onNewWalletClickListener) {
		this.onNewWalletClickListener = onNewWalletClickListener;
	}
	
	public void setOnImportWalletClickListener(OnImportWalletClickListener onImportWalletClickListener) {
		this.onImportWalletClickListener = onImportWalletClickListener;
	}

	public interface OnNewWalletClickListener {
		void onNewWallet(View view);
	}

	public interface OnImportWalletClickListener {
		void onImportWallet(View view);
	}

	private static class IntroPagerAdapter extends PagerAdapter {
        private int[] titles = new int[] {
                R.string.intro_title_first_page,
                R.string.welcome_erc20_label_title,
                R.string.intro_title_second_page,
                R.string.intro_title_third_page,
        };
        private int[] messages = new int[] {
                R.string.intro_message_first_page,
                R.string.welcome_erc20_label_description,
                R.string.intro_message_second_page,
                R.string.intro_message_third_page,
        };
        private int[] images = new int[] {
                R.mipmap.onboarding_lock,
                R.drawable.onboarding_erc20,
                R.mipmap.onboarding_open_source,
                R.mipmap.onboarding_rocket
        };

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = LayoutInflater.from(container.getContext())
                    .inflate(R.layout.layout_page_intro, container, false);
            ((TextView) view.findViewById(R.id.title)).setText(titles[position]);
            ((TextView) view.findViewById(R.id.message)).setText(messages[position]);
            ((ImageView) view.findViewById(R.id.img)).setImageResource(images[position]);
            container.addView(view);
            return view;
        }

        @Override
        public int getCount() {
            return titles.length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }
    }

    private static class DepthPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 0) { // [-1,0]
                // Use the default slide transition when moving to the left page
                view.setAlpha(1);
                view.setTranslationX(0);
                view.setScaleX(1);
                view.setScaleY(1);

            } else if (position <= 1) { // (0,1]
                // Fade the page out.
                view.setAlpha(1 - position);

                // Counteract the default slide transition
                view.setTranslationX(pageWidth * -position);

                // Scale the page down (between MIN_SCALE and 1)
                float scaleFactor = MIN_SCALE
                        + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }
}
