[1mdiff --git a/app/src/main/java/com/wallet/crypto/trustapp/ui/AddTokenActivity.java b/app/src/main/java/com/wallet/crypto/trustapp/ui/AddTokenActivity.java[m
[1mindex 452a0ff..f47a763 100644[m
[1m--- a/app/src/main/java/com/wallet/crypto/trustapp/ui/AddTokenActivity.java[m
[1m+++ b/app/src/main/java/com/wallet/crypto/trustapp/ui/AddTokenActivity.java[m
[36m@@ -11,6 +11,8 @@[m [mimport android.support.v7.app.AlertDialog;[m
 import android.text.Editable;[m
 import android.text.TextUtils;[m
 [m
[32m+[m[32mimport android.text.TextWatcher;[m
[32m+[m[32mimport android.util.Log;[m
 import android.view.View;[m
 import android.widget.Button;[m
 import android.widget.ImageButton;[m
[36m@@ -23,6 +25,9 @@[m [mimport com.wallet.crypto.trustapp.R;[m
 import com.wallet.crypto.trustapp.entity.Address;[m
 import com.wallet.crypto.trustapp.entity.ErrorEnvelope;[m
 [m
[32m+[m[32mimport com.wallet.crypto.trustapp.entity.TokenInfo;[m
[32m+[m[32mimport com.wallet.crypto.trustapp.ui.barcode.BarcodeCaptureActivity;[m
[32m+[m[32mimport com.wallet.crypto.trustapp.util.QRURLParser;[m
 import com.wallet.crypto.trustapp.viewmodel.AddTokenViewModel;[m
 import com.wallet.crypto.trustapp.viewmodel.AddTokenViewModelFactory;[m
 import com.wallet.crypto.trustapp.widget.SystemView;[m
[36m@@ -80,7 +85,6 @@[m [mpublic class AddTokenActivity extends BaseActivity implements View.OnClickListen[m
         viewModel.progress().observe(this, systemView::showProgress);[m
         viewModel.error().observe(this, this::onError);[m
         viewModel.result().observe(this, this::onSaved);[m
[31m-[m
         viewModel.update().observe(this, this::onChecked);[m
         lastCheck = "";[m
 [m
