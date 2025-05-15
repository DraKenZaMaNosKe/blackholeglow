package com.secret.blackholeglow.fragments;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.models.WallpaperItem;

public class WallpaperInfoDialogFragment extends DialogFragment {

    private static final String ARG_IMAGE = "image";
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESC = "desc";
    private static final String ARG_AUTHOR = "author";

    private OnApplyClickListener onApplyClickListener;

    public interface OnApplyClickListener {
        void onApplyClick();
    }

    public void setOnApplyClickListener(OnApplyClickListener listener) {
        this.onApplyClickListener = listener;
    }

    public static WallpaperInfoDialogFragment newInstance(WallpaperItem item) {
        WallpaperInfoDialogFragment frag = new WallpaperInfoDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE, item.getResourceIdPreview());
        args.putString(ARG_TITLE, item.getNombre());
        args.putString(ARG_DESC, item.getDescripcion());
        args.putString(ARG_AUTHOR, "Desarrollador: DraKenZaMaNosKe y Esmeralda");
        frag.setArguments(args);
        return frag;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_wallpaper_info, container, false);

        Bundle args = getArguments();
        if (args != null) {
            ((ImageView) view.findViewById(R.id.dialog_image_preview))
                    .setImageResource(args.getInt(ARG_IMAGE));
            ((TextView) view.findViewById(R.id.dialog_text_title))
                    .setText(args.getString(ARG_TITLE));
            ((TextView) view.findViewById(R.id.dialog_text_description))
                    .setText(args.getString(ARG_DESC));
            ((TextView) view.findViewById(R.id.dialog_text_author))
                    .setText(args.getString(ARG_AUTHOR));
        }

        Button applyBtn = view.findViewById(R.id.dialog_button_apply);
        applyBtn.setOnClickListener(v -> {
            if (onApplyClickListener != null) onApplyClickListener.onApplyClick();
            dismiss();
        });

        // OPCIONAL: Animación rápida de escala al aparecer
        view.setScaleX(0.92f); view.setScaleY(0.92f);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Fondo transparente elegante
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // ------ FADE-IN ANIMADO ------
        View root = getView();
        if (root != null) {
            root.setAlpha(0f);
            root.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .setStartDelay(50)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }
    }
}
