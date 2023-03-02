package com.alphawallet.app.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.NotificationTestViewModel;
import com.alphawallet.app.widget.InputAddress;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.messaging.FirebaseMessaging;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class NotificationTestFragment extends BaseFragment
    implements View.OnClickListener
{
    private NotificationTestViewModel viewModel;

    // Declare the launcher at the top of your Activity/Fragment:
    private final ActivityResultLauncher<String> requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted)
            {
                // FCM SDK (and your app) can post notifications.
            }
            else
            {
                // TODO: Inform user that that your app will not show notifications.
                Toast.makeText(getContext(), "Permission not granted.", Toast.LENGTH_SHORT).show();
            }
        });
    private TextView currentAddressText;
    private TextView resultText;
    private InputAddress addressInput;
    private MaterialButton fetchButton;
    private MaterialButton clearButton;

    private String address;

    private void askNotificationPermission()
    {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED)
            {
                // FCM SDK (and your app) can post notifications.
            }
            else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
            {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            }
            else
            {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_notification_test, container, false);

        toolbar(view);

        setToolbarTitle("Notification Test");

        initViewModel();

        initViews(view);

        return view;
    }

    private void initViews(View view)
    {
        currentAddressText = view.findViewById(R.id.text_current_address);
        resultText = view.findViewById(R.id.text_result);
        fetchButton = view.findViewById(R.id.btn_fetch);
        fetchButton.setOnClickListener(this);
        clearButton = view.findViewById(R.id.btn_clear);
        clearButton.setOnClickListener(this);
        addressInput = view.findViewById(R.id.input_address);
        addressInput.getEditText().setEnabled(false);
        addressInput.getInputView().addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                address = editable.toString();
                currentAddressText.setText(Utils.formatAddress(editable.toString()));
            }
        });
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this).get(NotificationTestViewModel.class);
        viewModel.defaultWallet().observe(getViewLifecycleOwner(), this::onDefaultWallet);
        viewModel.rawJsonString().observe(getViewLifecycleOwner(), this::onRawJsonString);
    }

    private void onRawJsonString(String result)
    {
        resultText.setText(result);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        address = wallet.address;

        currentAddressText.setText(Utils.formatAddress(address));

        askNotificationPermission();
    }

    @Override
    public void onClick(View view)
    {
        int id = view.getId();
        if (id == R.id.btn_fetch)
        {
            viewModel.fetchNotifications(address);
        }
        else if (id == R.id.btn_clear)
        {
            clearFields();
        }
    }

    private void clearFields()
    {
        Toast.makeText(getContext(), "Clear", Toast.LENGTH_SHORT).show();

        addressInput.setAddress("");

        resultText.setText("");
    }

    private void getToken()
    {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Timber.e(task.getException(), "Fetching FCM registration token failed");
                    return;
                }

                String token = task.getResult();

                Timber.d("token: " + token);
            });
    }

    private void subscribe(String topic)
    {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener(task -> {
                String msg = "Subscribed to " + topic;
                if (!task.isSuccessful())
                {
                    msg = "Subscribe failed";
                }
                Timber.d(msg);
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            });
    }

    private void unsubscribe(String topic)
    {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener(task -> {
                String msg = "Unsubscribed to" + topic;
                if (!task.isSuccessful())
                {
                    msg = "Unsubscribe failed";
                }
                Timber.d(msg);
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            });
    }
}
