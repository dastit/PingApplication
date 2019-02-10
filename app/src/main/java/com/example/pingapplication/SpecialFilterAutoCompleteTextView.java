package com.example.pingapplication;

import android.content.Context;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.util.AttributeSet;

public class SpecialFilterAutoCompleteTextView extends AppCompatAutoCompleteTextView {
    public SpecialFilterAutoCompleteTextView(Context context) {
        super(context);
    }

    public SpecialFilterAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpecialFilterAutoCompleteTextView(Context context, AttributeSet attrs,
                                             int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        int indexOfAddress = text.toString().lastIndexOf(" ");
        if( indexOfAddress >-1){
            text = text.toString().substring(indexOfAddress + 1);
        }
        super.performFiltering(text, keyCode);
    }
}
