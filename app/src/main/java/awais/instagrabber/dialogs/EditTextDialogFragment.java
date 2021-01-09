package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import awais.instagrabber.R;
import awais.instagrabber.utils.TextUtils;

public class EditTextDialogFragment extends DialogFragment {

    private Context context;
    private EditTextDialogFragmentCallback callback;

    public static EditTextDialogFragment newInstance(@StringRes final int title,
                                                     @StringRes final int positiveText,
                                                     @StringRes final int negativeText,
                                                     @Nullable final String initialText) {
        Bundle args = new Bundle();
        args.putInt("title", title);
        args.putInt("positive", positiveText);
        args.putInt("negative", negativeText);
        args.putString("initial", initialText);
        EditTextDialogFragment fragment = new EditTextDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public EditTextDialogFragment() {}

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        try {
            callback = (EditTextDialogFragmentCallback) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException("Calling fragment must implement EditTextDialogFragmentCallback interface");
        }
        this.context = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        int title = -1;
        int positiveButtonText = R.string.ok;
        int negativeButtonText = R.string.cancel;
        String initialText = null;
        if (arguments != null) {
            title = arguments.getInt("title", -1);
            positiveButtonText = arguments.getInt("positive", R.string.ok);
            negativeButtonText = arguments.getInt("negative", R.string.cancel);
            initialText = arguments.getString("initial", null);
        }
        final AppCompatEditText input = new AppCompatEditText(context);
        if (!TextUtils.isEmpty(initialText)) {
            input.setText(initialText);
        }
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setView(input)
                .setPositiveButton(positiveButtonText, (d, w) -> {
                    final String string = input.getText() != null ? input.getText().toString() : "";
                    if (callback != null) {
                        callback.onPositiveButtonClicked(string);
                    }
                })
                .setNegativeButton(negativeButtonText, (dialog, which) -> {
                    if (callback != null) {
                        callback.onNegativeButtonClicked();
                    }
                });
        if (title > 0) {
            builder.setTitle(title);
        }
        return builder.create();
    }

    public interface EditTextDialogFragmentCallback {
        void onPositiveButtonClicked(String text);

        void onNegativeButtonClicked();
    }
}
