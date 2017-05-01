package com.hiddenramblings.tagmo;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_check_sig)
public class CheckSignatureActivity extends AppCompatActivity {
    private static final String TAG = "CheckSignatureActivity";
    private static final int NFC_ACTIVITY = 0x102;
    byte[] currentTagData;

    @ViewById(R.id.txtTagInfo)
    TextView txtTagInfo;
    @ViewById(R.id.textError)
    TextView textError;

    NfcAdapter nfcAdapter;
    KeyManager keyManager;

    @AfterViews
    void afterViews() {
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        this.keyManager = new KeyManager(this);

        this.currentTagData = getIntent().getByteArrayExtra(Actions.EXTRA_TAG_DATA);
        update_status();

    }

    @Override
    protected void onResume() {
        super.onResume();
        clearError();
        if (!this.keyManager.hasBothKeys()) {
            showError("Keys not loaded");
            this.nfcAdapter = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @UiThread
    void showError(String msg) {
        textError.setText(msg);
        textError.setVisibility(View.VISIBLE);
        txtTagInfo.setVisibility(View.GONE);
    }

    @UiThread
    void clearError() {
        textError.setVisibility(View.GONE);
        txtTagInfo.setVisibility(View.VISIBLE);
    }

    @UiThread
    void update_status() {
        if (this.currentTagData != null) {
            try {
                byte[] charIdData = TagUtil.charIdDataFromTag(this.currentTagData);
                String charId = AmiiboDictionary.getDisplayName(charIdData);
                String uid = Util.bytesToHex(TagUtil.uidFromPages(this.currentTagData));
                String validate;

                try {
                    TagUtil.validateTag(this.currentTagData);
                    validate = "This tag is valid";
                } catch (Exception e) {
                    validate = e.getMessage();
                }



                String msg =
                        "Character ID: " + charId + "\n" +
                                "UID: " + uid + "\n" +
                        "Tag Validation: " + validate + "\n";

                txtTagInfo.setText(msg);


            } catch (Exception e) {
                Log.d(TAG, "Error parsing tag id", e);
            }
        } else {
                String msg = "No tag loaded yet. Press scan tag button.";
                txtTagInfo.setText(msg);
        }
    }

    @Click(R.id.btnGetTag)
    void scanTag () {
        Intent intent = new Intent(this, NfcActivity_.class);
        intent.setAction(NfcActivity.ACTION_SCAN_TAG);
        startActivityForResult(intent, NFC_ACTIVITY);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;

        Log.d(TAG, "onActivityResult");

        String action;
        switch (requestCode) {
            case NFC_ACTIVITY:
                if (data == null) return;
                action = data.getAction();
                if (!NfcActivity.ACTION_NFC_SCANNED.equals(action))
                    return;
                this.currentTagData = data.getByteArrayExtra(NfcActivity.EXTRA_TAG_DATA);
                update_status();
                if (this.currentTagData == null) {
                    Log.d(TAG, "Tag data is empty");
                    return;
                }
                break;
        }
    }
}
