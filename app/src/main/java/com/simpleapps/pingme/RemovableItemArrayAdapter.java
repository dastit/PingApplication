package com.simpleapps.pingme;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

public abstract class RemovableItemArrayAdapter<String> extends ArrayAdapter<String>
        implements View.OnClickListener {

    private HostNameDatabase              hostNameDatabase;
    private List<String>                  objects;

    public RemovableItemArrayAdapter(@NonNull Context context,
                                     int resource,
                                     int textViewResourceId,
                                     @NonNull List<String> objects) {
        super(context, resource, textViewResourceId, objects);
        this.objects = objects;
        hostNameDatabase = HostNameDatabase.getInstance(getContext()
                                                                .getApplicationContext());
    }


    @NonNull
    @Override
    public View getView(final int position, @Nullable final View convertView,
                        @NonNull final ViewGroup parent) {
        final View     view         = super.getView(position, convertView, parent);
        final TextView textView     = view.findViewById(R.id.adapter_text);
        ImageButton    removeButton = view.findViewById(R.id.adapter_remove_button);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView deletedTextView = view.findViewById(R.id.adapter_deleted_text);

                //remove item from adapter and from db
                String item = getItem(position);
                remove(item);
                hostNameDatabase.hostNameDao().delete(item.toString());

                //confirmation of removing item to user
                v.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
                deletedTextView.setVisibility(View.VISIBLE);
            }
        });

        textView.setOnClickListener(this);

        return view;
    }

    @Override
    public int getPosition(@Nullable String item) {
        int index = objects.indexOf(item);
        return index;
    }

}
