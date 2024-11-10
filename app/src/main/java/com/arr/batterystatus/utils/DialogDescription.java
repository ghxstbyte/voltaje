package com.arr.batterystatus.utils;

import android.content.Context;
import com.arr.batterystatus.databinding.LayoutViewDescriptionBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class DialogDescription extends BottomSheetDialog {

    private LayoutViewDescriptionBinding binding;
    private Context mContext;

    public DialogDescription(Context context) {
        super(context);
        binding = LayoutViewDescriptionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    public DialogDescription setMessage(String message) {
        binding.message.setText(message);
        return this;
    }

    public DialogDescription setMessage(int message) {
        binding.message.setText(message);
        return this;
    }
}
